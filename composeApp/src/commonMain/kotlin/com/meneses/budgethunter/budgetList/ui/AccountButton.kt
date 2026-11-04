package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.account
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/**
 * The account in the app bar: a circle with the initial of [email], or a person icon in offline
 * mode, where there is no account yet.
 */
@Composable
fun AccountButton(
    email: String?,
    onClick: () -> Unit
) {
    val description = stringResource(Res.string.account)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 4.dp)
            .semantics { contentDescription = description }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val initial = email?.firstOrNull()?.uppercase()
            if (initial != null) {
                Text(
                    text = initial,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.onTertiaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = AppColors.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
