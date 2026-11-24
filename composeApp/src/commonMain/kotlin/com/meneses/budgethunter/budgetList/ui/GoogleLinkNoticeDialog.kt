package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.google_link_notice_dismiss
import budgethunter.composeapp.generated.resources.google_link_notice_members
import budgethunter.composeapp.generated.resources.google_link_notice_owner
import budgethunter.composeapp.generated.resources.google_link_notice_password
import budgethunter.composeapp.generated.resources.google_link_notice_shared
import budgethunter.composeapp.generated.resources.google_link_notice_title
import budgethunter.composeapp.generated.resources.google_link_notice_yours
import com.meneses.budgethunter.auth.domain.GoogleLinkNotice
import org.jetbrains.compose.resources.stringResource

/**
 * Explains what Google sign in changed when it linked an account with a password of unknown
 * origin, and lists the budgets the account already shared so the user can spot one planted by
 * whoever registered the email first. Dismissing it is the only way to stop showing the banner.
 */
@Composable
fun GoogleLinkNoticeDialog(
    notice: GoogleLinkNotice?,
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show || notice == null) return

    AlertDialog(
        // Only the button dismisses it: a tap outside would lose the notice unread
        onDismissRequest = {},
        title = { Text(text = stringResource(Res.string.google_link_notice_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(Res.string.google_link_notice_password))
                if (notice.sharedBudgets.isNotEmpty()) {
                    Text(text = stringResource(Res.string.google_link_notice_shared))
                    notice.sharedBudgets.forEach { budget ->
                        SharedBudgetRow(budget = budget, accountEmail = notice.accountEmail)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.google_link_notice_dismiss),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun SharedBudgetRow(budget: GoogleLinkNotice.SharedBudget, accountEmail: String) {
    Column {
        Text(text = budget.name.ifBlank { "—" }, fontWeight = FontWeight.Bold)
        // Lines the server left empty are skipped rather than shown blank
        val owner = when {
            budget.ownerEmail.isBlank() -> null
            budget.ownerEmail.equals(accountEmail, ignoreCase = true) ->
                stringResource(Res.string.google_link_notice_yours)
            else -> stringResource(Res.string.google_link_notice_owner, budget.ownerEmail)
        }
        owner?.let { DetailLine(it) }
        if (budget.otherMembers.isNotEmpty()) {
            DetailLine(stringResource(Res.string.google_link_notice_members, budget.otherMembers.joinToString()))
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
