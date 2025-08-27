package com.meneses.budgethunter.budgetEntry.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.commons.util.getBitmapFromPDFFile
import com.meneses.budgethunter.theme.AppColors

@Composable
fun AttachInvoiceModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFile: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.attach_invoice_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.select_invoice_option),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 28.dp)
                    )

                    Card(
                        modifier = Modifier
                            .dashedBorder(
                                width = 1.dp,
                                color = AppColors.onSecondaryContainer,
                                shape = AbsoluteRoundedCornerShape(15.dp),
                                on = 10.dp,
                                off = 8.dp
                            ),
                        onClick = {
                            onTakePhoto()
                            onDismiss()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.camera),
                                contentDescription = stringResource(R.string.take_picture_icon_description),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.take_a_picture),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .dashedBorder(
                                width = 1.dp,
                                color = AppColors.onSecondaryContainer,
                                shape = AbsoluteRoundedCornerShape(15.dp),
                                on = 10.dp,
                                off = 8.dp
                            ),
                        onClick = {
                            onSelectFile()
                            onDismiss()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search_file),
                                contentDescription = stringResource(R.string.select_from_files_icon_description),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.select_from_files),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}

@Composable
fun ShowInvoiceModal(
    show: Boolean,
    invoice: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    remember(invoice) {
                        if (invoice?.endsWith(".pdf", ignoreCase = true) == true) {
                            try {
                                val bitmap = getBitmapFromPDFFile(invoice)
                                bitmap.asImageBitmap()
                            } catch (e: Exception) {
                                // If PDF processing fails, try to decode as image
                                BitmapFactory
                                    .decodeFile(invoice)
                                    ?.asImageBitmap()
                            }
                        } else {
                            BitmapFactory
                                .decodeFile(invoice)
                                ?.asImageBitmap()
                        }
                    }?.let {
                        Image(
                            bitmap = it,
                            contentDescription = stringResource(R.string.invoice_content_description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .padding(bottom = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Card(
                            onClick = {
                                onEdit()
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_content_description),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Card(
                            onClick = {
                                onShare()
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_content_description),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Card(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_content_description),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
