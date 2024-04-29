package com.meneses.budgethunter.budgetEntry.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.Modal
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.theme.AppColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AttachInvoiceModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFile: () -> Unit
) {
    if (show) {
        Modal(onDismiss) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.outlinedCardColors(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .dashedBorder(
                        width = 1.dp,
                        color = AppColors.onSecondaryContainer,
                        shape = AbsoluteRoundedCornerShape(5.dp),
                        on = 10.dp,
                        off = 8.dp
                    )
                    .clickable(onClick = onTakePhoto)
                    .width(TextFieldDefaults.MinWidth)
                    .padding(15.dp)
            ) {
                Text(
                    text = "Take a picture",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.outlinedCardColors(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .dashedBorder(
                        width = 1.dp,
                        color = AppColors.onSecondaryContainer,
                        shape = AbsoluteRoundedCornerShape(5.dp),
                        on = 10.dp,
                        off = 8.dp
                    )
                    .clickable(onClick = onSelectFile)
                    .width(TextFieldDefaults.MinWidth)
                    .padding(15.dp)
            ) {
                Text(
                    text = "Select from files",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    painter = painterResource(id = R.drawable.search_file),
                    contentDescription = "",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ShowInvoiceModal(
    show: Boolean,
    invoice: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (show) {
        Modal(onDismiss) {
            Spacer(modifier = Modifier.height(20.dp))

            BitmapFactory
                .decodeFile(invoice)
                ?.asImageBitmap()
                ?.let { Image(bitmap = it, contentDescription = "") }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.width(TextFieldDefaults.MinWidth)
            ) {
                Card(
                    colors = CardDefaults.outlinedCardColors(),
                    modifier = Modifier
                        .dashedBorder(
                            width = 1.dp,
                            color = AppColors.onSecondaryContainer,
                            shape = AbsoluteRoundedCornerShape(5.dp),
                            on = 10.dp,
                            off = 8.dp
                        )
                        .clickable(onClick = onEdit)
                        .weight(0.45f)
                        .padding(15.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.1f))

                Card(
                    colors = CardDefaults.outlinedCardColors(),
                    modifier = Modifier
                        .dashedBorder(
                            width = 1.dp,
                            color = AppColors.onSecondaryContainer,
                            shape = AbsoluteRoundedCornerShape(5.dp),
                            on = 10.dp,
                            off = 8.dp
                        )
                        .clickable(onClick = onDelete)
                        .weight(0.45f)
                        .padding(15.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(25.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
