package es.verifirx.app.ui.common

import es.verifirx.app.data.SessionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))

fun formatSessionTimestamp(session: SessionRecord): String =
    dateFormat.format(Date(session.createdAtEpochMillis))

fun verdictSummaryText(session: SessionRecord): String {
    val total = session.rows.size
    return "${session.matches}/$total coincidencias · ${session.mismatches} no coinciden · ${session.needsReview} a revisar"
}
