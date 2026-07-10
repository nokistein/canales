# VerifiRX

App Android para personal de farmacia que verifica el "Justificante de la
Dispensación — Receta XXI" (Junta de Andalucía): fotografía o importa el
escaneo de la hoja y, para cada medicamento dispensado, compara el nombre y
el Código Nacional (CN) impresos por el sistema con los del cupón precinto
pegado a su derecha, indicando si coinciden.

Ver el diseño completo (algoritmo de comparación, arquitectura de módulos,
decisiones y limitaciones conocidas) en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

## Estructura

- `matching/` — módulo Kotlin puro (sin dependencias de Android) con el
  algoritmo de verificación: segmentación de filas, extracción de CN,
  similitud de nombres y el veredicto final. Totalmente testeado.
- `app/` — módulo Android: cámara (CameraX), OCR y códigos de barras
  (Google ML Kit, en el dispositivo), UI (Jetpack Compose) e histórico local.

## Compilar y probar

El algoritmo (`matching`) no depende del SDK de Android y se puede compilar
y testear con solo un JDK:

```
./gradlew :matching:test
```

El módulo `app` requiere Android Studio / un SDK de Android instalado:

```
./gradlew :app:assembleDebug
```

## Privacidad

Todo el reconocimiento de texto y códigos de barras corre en el dispositivo
(ML Kit on-device). La app no pide permiso de `INTERNET`, no sube imágenes
ni datos a ningún servidor, y el histórico local está excluido de copias de
seguridad automáticas por tratarse de datos de dispensación.
