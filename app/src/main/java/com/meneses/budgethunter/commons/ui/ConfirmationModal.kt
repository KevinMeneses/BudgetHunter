package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.theme.AppColors

@Composable
fun ConfirmationModal(
    show: Boolean,
    message: String,
    confirmButtonText: String,
    cancelButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        Modal(onDismiss = onDismiss) {
            Text(
                text = message,
                modifier = Modifier.padding(bottom = 20.dp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    border = BorderStroke(width = 1.dp, color = AppColors.onBackground),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.background,
                        contentColor = AppColors.onBackground
                    ),
                    onClick = onDismiss,
                    content = { Text(text = cancelButtonText) }
                )
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.error,
                        contentColor = AppColors.onError
                    ),
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    content = {
                        Text(text = confirmButtonText)
                    }
                )
            }
        }
    }
}