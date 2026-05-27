package com.example.shipmonitoring.ui.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shipmonitoring.data.model.SubmissionResponse

@Composable
fun SubmissionStatusBadge(status: String) {
    val normalized = status.uppercase()
    val containerColor = when (normalized) {
        "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
        "REJECTED" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when (normalized) {
        "APPROVED" -> MaterialTheme.colorScheme.onPrimaryContainer
        "REJECTED" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = statusLabel(normalized),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SubmissionSummaryCard(
    submission: SubmissionResponse,
    showValidationAction: Boolean,
    onDetail: (SubmissionResponse) -> Unit,
    onApprove: ((SubmissionResponse) -> Unit)? = null,
    onReject: ((SubmissionResponse) -> Unit)? = null
) {
    val shipNumber = submission.ship?.shipNumber ?: "-"
    val shipName = submission.ship?.name ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$shipNumber - $shipName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nama Nahkoda: ${submission.captainName}")
            Text("Muatan: ${submission.cargo}")
            Text("Jumlah Muatan: ${submission.cargoAmount}")
            Text("Tanggal Pengajuan: ${formatDateTime(submission.submittedAt)}")
            Spacer(modifier = Modifier.height(10.dp))
            SubmissionStatusBadge(submission.status)

            if (submission.status.equals("REJECTED", ignoreCase = true) && !submission.reviewNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan admin: ${submission.reviewNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onDetail(submission) }) {
                    Text("Detail")
                }

                if (showValidationAction && onApprove != null && onReject != null) {
                    Button(onClick = { onApprove(submission) }) {
                        Text("Approve")
                    }
                    OutlinedButton(onClick = { onReject(submission) }) {
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionDetailDialog(
    submission: SubmissionResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        title = {
            Text("Detail Pengajuan")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val shipNumber = submission.ship?.shipNumber ?: "-"
                val shipName = submission.ship?.name ?: "-"

                Text("Data Kapal", fontWeight = FontWeight.SemiBold)
                Text("Nomor Kapal: $shipNumber")
                Text("Nama Kapal: $shipName")
                Text("Nama Nahkoda: ${submission.captainName}")

                Text("Data Pengajuan", fontWeight = FontWeight.SemiBold)
                Text("Jumlah Pegawai: ${submission.employeeCount}")
                Text("Muatan: ${submission.cargo}")
                Text("Jumlah Muatan: ${submission.cargoAmount}")
                Text("Tanggal Pengajuan: ${formatDateTime(submission.submittedAt)}")
                Text("Status: ${statusLabel(submission.status)}")

                Text("Dokumen", fontWeight = FontWeight.SemiBold)
                DocumentLinkButton("Surat Izin Berlayar", submission.sailingPermitUrl)
                DocumentLinkButton("Surat Tanda Panggilan", submission.callSignCertificateUrl)
                DocumentLinkButton("Sertifikat Keselamatan", submission.safetyCertificateUrl)
                DocumentLinkButton("Surat Izin Stasiun Radio Kapal", submission.radioStationPermitUrl)

                Text("Validasi", fontWeight = FontWeight.SemiBold)
                Text("Status: ${statusLabel(submission.status)}")
                Text("Catatan admin: ${submission.reviewNote ?: "-"}")
                Text("Tanggal review: ${formatDateTime(submission.reviewedAt)}")

                submission.arrivalInspection?.let { inspection ->
                    val yesCount = inspection.items.count { it.condition.equals("YES", ignoreCase = true) }
                    val noCount = inspection.items.count { it.condition.equals("NO", ignoreCase = true) }

                    Text("Hasil Cek Kedatangan", fontWeight = FontWeight.SemiBold)
                    Text("Tanggal cek: ${formatDateTime(inspection.checkedAt)}")
                    Text("Catatan cek: ${inspection.note ?: "-"}")
                    Text("Checklist: YA $yesCount / TIDAK $noCount")
                    DocumentLinkButton("Dokumen hasil cek", inspection.inspectionDocumentUrl.orEmpty())
                    DocumentLinkButton("Surat balasan", inspection.responseLetterUrl.orEmpty())
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "URL dokumen dapat kedaluwarsa, muat ulang detail jika dokumen gagal dibuka.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun DocumentLinkButton(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val canOpen = url.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            enabled = canOpen,
            onClick = {
                if (canOpen) {
                    val opened = runCatching {
                        uriHandler.openUri(url)
                    }.isSuccess

                    if (!opened) {
                        Toast.makeText(
                            context,
                            "Gagal membuka dokumen. Muat ulang detail untuk mendapatkan URL terbaru.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        ) {
            Text("Lihat Dokumen")
        }
    }
}
