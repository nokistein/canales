# VerifiRX — Verificador de Justificantes de Dispensación (Receta XXI)

## 1. Problema y alcance

El personal de una farmacia, tras dispensar una receta a través de Receta XXI
(Junta de Andalucía), obtiene un **"Justificante de la Dispensación"**: una
hoja impresa con una fila por cada medicamento dispensado. Cada fila tiene:

- **Columna izquierda**: nombre del medicamento y Código Nacional (CN)
  impresos directamente por el sistema de Receta XXI.
- **Columna derecha**: el **cupón precinto** — la pegatina que se despega de
  la caja del medicamento real y se pega físicamente en la hoja — con su
  propio nombre/CN impreso y, casi siempre, un código de barras.

Un error de dispensación (coger la caja equivocada, una presentación
distinta, un genérico no correctamente registrado) se manifiesta como un
**desajuste entre lo impreso por el sistema y lo que dice el cupón**. VerifiRX
fotografía o importa el escaneo de la hoja y, para cada fila, compara ambas
columnas y marca **Coincide / No coincide / Revisar**.

No es un sistema de prescripción ni sustituye el juicio del farmacéutico: es
una segunda verificación rápida, tipo "doble check" fotográfico.

## 2. Principio de diseño: todo el procesamiento es local

La hoja contiene datos de dispensación (potencialmente vinculables a un
paciente). Por eso:

- El reconocimiento de texto y de códigos de barras se hace **en el
  dispositivo** con Google ML Kit (modelos on-device), no con OCR en la nube.
- La app **no declara permiso de `INTERNET`**. El modelo de reconocimiento de
  texto de ML Kit se descarga una única vez a través de Google Play Services
  (no mediante la propia app) y a partir de ahí funciona sin red; para
  evitar incluso esa descarga diferida, el manifest incluye la metadata
  `com.google.mlkit.vision.DEPENDENCIES=ocr`, que hace que Play Store
  precargue el modelo en la instalación.
- Nada se sube a un backend: las imágenes y el histórico se guardan solo en
  almacenamiento privado de la app (`filesDir`), excluido explícitamente de
  las copias de seguridad automáticas y de la transferencia a nuevo
  dispositivo (`data_extraction_rules.xml`), precisamente porque son datos
  de dispensación.

## 3. Arquitectura de módulos

```
canales/
├── matching/   Kotlin puro (sin Android). El algoritmo de verificación.
└── app/        Módulo Android: cámara, OCR/ML Kit, UI (Compose), persistencia.
```

Separar el algoritmo en un módulo JVM puro es la decisión central del
diseño: la lógica que decide si dos columnas "coinciden" es la parte que
importa que sea *correcta*, y aquí es 100 % testeable con JUnit sin necesitar
un emulador Android. `app` es una capa relativamente delgada que conecta
cámara → ML Kit → `matching` → pantallas.

### 3.1 `matching` — el motor de verificación

Pipeline, de entrada (bloques de texto OCR + códigos de barras) a salida
(veredicto por fila):

1. **`RowSegmenter`**: agrupa los bloques de texto reconocidos en filas de
   tabla, por proximidad vertical, y dentro de cada fila separa los bloques
   en columna izquierda/derecha según su posición horizontal relativa al
   ancho de página. Se optó deliberadamente por **no** intentar detectar
   líneas de tabla en la imagen (visión por computador clásica): el
   formulario de Receta XXI no siempre tiene rejilla visible y varía según
   impresora/escáner, mientras que agrupar por geometría del propio texto
   reconocido es mucho más robusto a la calidad del escaneo.

2. **`FieldParser`**: dentro del texto de cada columna, separa el nombre del
   medicamento del CN (`CnExtractor`), y si hay un código de barras asignado
   a esa fila, su valor decodificado **sustituye** al CN leído por OCR en el
   cupón (un código de barras lleva dígito de control; el OCR de una
   etiqueta térmica pequeña no).

3. **`CnExtractor`**: extrae el CN (6 dígitos, o 7 si el OCR captura también
   el dígito de control) y compara dos códigos siendo tolerante a ceros a la
   izquierda y a la presencia/ausencia de ese séptimo dígito.

4. **`NameSimilarity`**: similitud difusa de nombres (distancia de
   Levenshtein a nivel de carácter + Jaccard a nivel de token), pensada para
   absorber ruido de OCR y abreviaturas de presentación ("COMP" vs
   "COMPRIMIDOS"), sin depender de que el nombre sea idéntico carácter a
   carácter.

5. **`BarcodeAssigner`**: asigna cada código de barras decodificado a la fila
   verticalmente más cercana.

6. **`DispensationVerifier`**: orquesta lo anterior y decide el veredicto por
   fila. El **CN es la señal autoritativa** (es un código preciso, la
   probabilidad de coincidencia accidental es nula):
   - CN no coincide → **No coincide**, siempre, aunque el nombre se parezca
     (puede ser el mismo principio activo en otra presentación/envase).
   - CN coincide pero el nombre difiere mucho → **Revisar** (normalmente
     indica un fallo de segmentación de fila, no un error real de
     dispensación), *salvo* que el CN venga confirmado por código de barras,
     en cuyo caso se confía en él aunque el OCR del nombre en el cupón sea
     ilegible.
   - Falta CN en algún lado → **Revisar** (nunca se declara "Coincide" sin
     confirmación del CN).
   - CN y nombre coinciden → **Coincide**.

Cobertura de tests actual: **24/24 tests unitarios en verde**
(`./gradlew :matching:test`), incluyendo un test de extremo a extremo que
simula una hoja de 4 filas con un acierto limpio, un CN que no coincide, un
cupón ausente y un caso donde el OCR del nombre es ilegible pero el código de
barras confirma el CN.

### 3.2 `app` — Android

- **Cámara y carga**: CameraX (vista previa en directo, encuadre guiado) +
  selector de galería (`ActivityResultContracts.GetContent`) para subir un
  escaneo ya existente.
- **OCR**: `TextRecognitionAdapter` (ML Kit Text Recognition, latino) da una
  línea de texto por bloque reconocido — el nivel de granularidad que
  `RowSegmenter` necesita. `BarcodeAdapter` (ML Kit Barcode Scanning) busca
  Code 128 / Code 39 / EAN-13 / Data Matrix, los formatos habituales en un
  cupón precinto.
- **`DocumentImageProcessor`**: ejecuta OCR y códigos de barras en paralelo
  (corrutinas) y llama a `DispensationVerifier` del módulo `matching`.
- **Persistencia**: sin Room/Hilt deliberadamente (ver §4). `SessionRepository`
  guarda cada verificación como una imagen JPEG + un JSON
  (`kotlinx.serialization`) en `filesDir`, para tener histórico/auditoría.
- **UI**: Jetpack Compose + Material 3, sin Hilt (contenedor de
  dependencias manual, `ServiceLocator`), navegación con Navigation-Compose.
  - **Inicio**: verificaciones recientes + botón "Nueva verificación".
  - **Captura**: vista previa de cámara con marco guía, botón de disparo y
    botón de galería; pide el permiso de cámara solo cuando hace falta y
    permite subir un escaneo aunque se deniegue.
  - **Procesando**: overlay mientras corre el pipeline OCR → comparación.
  - **Resultado**: imagen capturada, resumen (coinciden / no coinciden / a
    revisar) y una fila expandible por medicamento con nombre + CN de cada
    columna, motivo del veredicto y la posibilidad de que el farmacéutico
    **anule manualmente** un veredicto (con nota obligatoria), para los
    casos límite que el algoritmo marca como "Revisar".
  - **Historial**: verificaciones guardadas, para consulta posterior.

## 4. Decisiones y por qué

- **Sin Hilt, sin Room**: el grafo de dependencias de la app es pequeño (un
  puñado de singletons); Room/Hilt añaden procesadores de anotaciones (KSP)
  que no aportan beneficio proporcional aquí y son una fuente extra de
  fallos de compilación que no se pueden verificar en este entorno (ver §6).
  Un `ServiceLocator` manual y persistencia en JSON son más fáciles de
  razonar y de verificar a simple vista.
- **CN por encima del nombre**: es la decisión de producto más importante
  del algoritmo. Un farmacéutico no debería tener que fiarse de una
  comparación difusa de texto para algo tan crítico como "¿es la misma caja?"
  — el CN es unívoco, el nombre es una señal de apoyo/aviso.
- **Código de barras por encima del OCR del cupón**: las pegatinas del
  cupón se imprimen en impresoras térmicas de baja resolución y a menudo
  torcidas; el código de barras lleva dígito de control y es mucho más
  fiable que el OCR de ese mismo texto.
- **Ninguna fila se marca "Coincide" sin CN confirmado por ambos lados**:
  aunque el nombre sea idéntico, si no se pudo leer el CN en algún lado el
  veredicto es "Revisar", nunca "Coincide" ni "No coincide" — el sistema no
  debe dar una falsa sensación de seguridad cuando le faltan datos.

## 5. Extensiones futuras (fuera del alcance actual)

- Recorte/enderezado automático del documento antes de OCR (ML Kit
  Document Scanner o `GmsDocumentScanner`) para tolerar fotos en ángulo.
- Soporte de PDF multipágina (escáner de sobremesa) además de imagen.
- Exportar el historial a PDF/CSV para adjuntarlo a una incidencia.
- Ajuste de la fracción de columna (`columnSplitFraction`) y del umbral de
  similitud de nombre desde una pantalla de ajustes, por si otra provincia
  usa un formato de hoja con proporciones distintas.

## 6. Limitaciones de esta entrega

Este entorno de desarrollo no tiene instalado el SDK de Android (ni
`ANDROID_HOME`, ni un emulador), así que **el módulo `app` no se ha podido
compilar ni ejecutar aquí**. Lo que sí se ha verificado de forma real:

- `./gradlew :matching:test` compila y pasa 24/24 tests — el algoritmo de
  comparación (la parte que decide "coincide o no") está probado de verdad,
  no solo escrito.
- El módulo `app` está escrito contra APIs estables y ampliamente usadas
  (CameraX 1.3, ML Kit Text Recognition 16 / Barcode Scanning 17, Compose
  BOM 2024.06, Navigation-Compose 2.7), pero **no ha sido compilado ni
  probado en un dispositivo/emulador real**. Antes de dar la app por
  terminada hace falta abrirla en Android Studio (o correr
  `./gradlew :app:assembleDebug` con un SDK instalado), corregir lo que
  el compilador señale y probar el flujo de cámara con hojas reales — la
  segmentación por geometría de texto en particular conviene calibrarla
  (`columnSplitFraction`, umbral de agrupación por filas) con fotos reales
  de hojas de Receta XXI, ya que ahora mismo sus valores por defecto están
  elegidos por criterio razonable, no ajustados contra el formato real.
