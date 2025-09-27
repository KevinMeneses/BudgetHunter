package com.meneses.budgethunter.budgetEntry.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.attach_invoice_title
import budgethunter.composeapp.generated.resources.attach_new_file
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.delete_content_description
import budgethunter.composeapp.generated.resources.edit_content_description
import budgethunter.composeapp.generated.resources.file_exists_cannot_load
import budgethunter.composeapp.generated.resources.file_no_longer_accessible
import budgethunter.composeapp.generated.resources.file_no_longer_available
import budgethunter.composeapp.generated.resources.invoice_content_description
import budgethunter.composeapp.generated.resources.replace_file
import budgethunter.composeapp.generated.resources.select_from_files
import budgethunter.composeapp.generated.resources.select_invoice_option
import budgethunter.composeapp.generated.resources.share_content_description
import budgethunter.composeapp.generated.resources.take_a_picture
import budgethunter.composeapp.generated.resources.unable_to_display_file
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.commons.util.getImageBitmapFromFile
import com.meneses.budgethunter.commons.util.getImageBitmapFromPDFFile
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvoiceDisplayModal(
    show: Boolean,
    validatedFilePath: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onError: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    remember(validatedFilePath) {
                        if (validatedFilePath.endsWith(".pdf", ignoreCase = true)) {
                            getImageBitmapFromPDFFile(validatedFilePath)
                        } else {
                            getImageBitmapFromFile(validatedFilePath)
                        }
                    }?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = stringResource(Res.string.invoice_content_description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .padding(bottom = 16.dp)
                        )

                        // Action buttons
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
                                    contentDescription = stringResource(Res.string.edit_content_description),
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
                                    contentDescription = stringResource(Res.string.share_content_description),
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
                                    contentDescription = stringResource(Res.string.delete_content_description),
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(20.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } ?: run {
                        // If image can't be loaded, show the not loadable message
                        onError()
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun FileNotFoundModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onReattach: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(Res.string.file_no_longer_available),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.file_no_longer_accessible),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onReattach) {
                    Text(stringResource(Res.string.attach_new_file))
                }
            }
        )
    }
}

@Composable
fun FileNotLoadableModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onReplace: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(Res.string.unable_to_display_file),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.file_exists_cannot_load),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onReplace) {
                    Text(stringResource(Res.string.replace_file))
                }
            }
        )
    }
}

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
                    text = stringResource(Res.string.attach_invoice_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(Res.string.select_invoice_option),
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
                            Text(
                                text = "📷",
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.take_a_picture),
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
                            Text(
                                text = "📁",
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.select_from_files),
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
                        text = stringResource(Res.string.cancel),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            properties = DialogProperties()
        )
    }
}
