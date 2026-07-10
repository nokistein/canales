package es.verifirx.app.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.verifirx.app.R
import es.verifirx.app.data.RowRecord
import es.verifirx.app.data.SessionRecord
import es.verifirx.app.data.StoredVerdict
import es.verifirx.app.ui.theme.MatchColor
import es.verifirx.app.ui.theme.MismatchColor
import es.verifirx.app.ui.theme.ReviewColor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: ResultsViewModel) {
    val session by viewModel.session.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.results_title)) }) },
    ) { padding ->
        val current = session ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AsyncImage(
                    model = File(current.imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            }
            item { SummaryRow(current) }
            items(current.rows, key = RowRecord::rowIndex) { row ->
                RowComparisonCard(
                    row = row,
                    onOverride = { verdict, note -> viewModel.setManualVerdict(row.rowIndex, verdict, note) },
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(session: SessionRecord) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryChip(stringResource(R.string.results_summary_matches, session.matches), MatchColor)
        SummaryChip(stringResource(R.string.results_summary_mismatches, session.mismatches), MismatchColor)
        SummaryChip(stringResource(R.string.results_summary_review, session.needsReview), ReviewColor)
    }
}

@Composable
private fun SummaryChip(text: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Text(text, color = color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun RowComparisonCard(row: RowRecord, onOverride: (StoredVerdict, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val effectiveVerdict = row.manualVerdict ?: row.verdict

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VerdictIcon(effectiveVerdict)
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(row.leftName ?: "—", style = MaterialTheme.typography.bodyLarge)
                    Text(row.rightName ?: "—", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (expanded) {
                RowDetail(row = row, onOverride = onOverride)
            }
        }
    }
}

@Composable
private fun VerdictIcon(verdict: StoredVerdict) {
    when (verdict) {
        StoredVerdict.MATCH -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.results_verdict_match), tint = MatchColor)
        StoredVerdict.MISMATCH -> Icon(Icons.Filled.Error, contentDescription = stringResource(R.string.results_verdict_mismatch), tint = MismatchColor)
        StoredVerdict.NEEDS_REVIEW -> Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.results_verdict_review), tint = ReviewColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowDetail(row: RowRecord, onOverride: (StoredVerdict, String) -> Unit) {
    var note by remember(row.rowIndex) { mutableStateOf(row.manualNote.orEmpty()) }
    var selected by remember(row.rowIndex) { mutableStateOf(row.manualVerdict ?: row.verdict) }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        FieldColumn(stringResource(R.string.results_row_left), row.leftName, row.leftCn)
        FieldColumn(stringResource(R.string.results_row_right), row.rightName, row.rightCn, fromBarcode = row.cnFromBarcode)
        Text(row.reason, modifier = Modifier.padding(top = 8.dp))

        Text(stringResource(R.string.results_manual_override), modifier = Modifier.padding(top = 12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 4.dp)) {
            StoredVerdict.entries.forEachIndexed { index, verdict ->
                SegmentedButton(
                    selected = selected == verdict,
                    onClick = { selected = verdict },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = StoredVerdict.entries.size),
                ) {
                    Text(verdictLabel(verdict))
                }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.results_override_note_label)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Button(
            onClick = { onOverride(selected, note) },
            enabled = selected != (row.manualVerdict ?: row.verdict) || note.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.results_save))
        }
    }
}

@Composable
private fun FieldColumn(label: String, name: String?, cn: String?, fromBarcode: Boolean = false) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(name ?: "—")
        val cnSuffix = if (fromBarcode) " (código de barras)" else ""
        Text((cn ?: "—") + cnSuffix)
    }
}

@Composable
private fun verdictLabel(verdict: StoredVerdict): String = when (verdict) {
    StoredVerdict.MATCH -> stringResource(R.string.results_verdict_match)
    StoredVerdict.MISMATCH -> stringResource(R.string.results_verdict_mismatch)
    StoredVerdict.NEEDS_REVIEW -> stringResource(R.string.results_verdict_review)
}
