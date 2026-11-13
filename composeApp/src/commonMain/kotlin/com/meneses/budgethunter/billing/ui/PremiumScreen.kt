package com.meneses.budgethunter.billing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.premium_active_title
import budgethunter.composeapp.generated.resources.premium_active_until
import budgethunter.composeapp.generated.resources.premium_benefit_more
import budgethunter.composeapp.generated.resources.premium_categorizations_left
import budgethunter.composeapp.generated.resources.premium_benefit_scans
import budgethunter.composeapp.generated.resources.premium_benefit_support
import budgethunter.composeapp.generated.resources.premium_fine_print
import budgethunter.composeapp.generated.resources.premium_free_trial
import budgethunter.composeapp.generated.resources.premium_load_failed
import budgethunter.composeapp.generated.resources.premium_manage
import budgethunter.composeapp.generated.resources.premium_offers_failed
import budgethunter.composeapp.generated.resources.premium_per_month
import budgethunter.composeapp.generated.resources.premium_per_year
import budgethunter.composeapp.generated.resources.premium_plan_annual
import budgethunter.composeapp.generated.resources.premium_plan_monthly
import budgethunter.composeapp.generated.resources.premium_plan_other
import budgethunter.composeapp.generated.resources.premium_processing
import budgethunter.composeapp.generated.resources.premium_restore
import budgethunter.composeapp.generated.resources.premium_retry
import budgethunter.composeapp.generated.resources.premium_scans_left
import budgethunter.composeapp.generated.resources.premium_scans_resets
import budgethunter.composeapp.generated.resources.premium_subtitle
import budgethunter.composeapp.generated.resources.premium_title
import budgethunter.composeapp.generated.resources.premium_unavailable
import budgethunter.composeapp.generated.resources.premium_usage_title
import com.meneses.budgethunter.billing.application.PremiumIntent
import com.meneses.budgethunter.billing.application.PremiumState
import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.PremiumPeriod
import com.meneses.budgethunter.billing.domain.AiUsage
import com.meneses.budgethunter.billing.domain.Allowance
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.commons.ui.LoadingScreen
import com.meneses.budgethunter.commons.ui.shortDateLabel
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * BudgetHunter Premium: the account's plan and this month's receipt scans, the plans for sale and restore.
 * Opened from Settings and from the "no scans left" notice. What premium includes is the server's decision;
 * this screen only shows it and sells.
 */
@Serializable
object PremiumScreen {

    private const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

    @Composable
    fun Show(
        uiState: PremiumState,
        onIntent: (PremiumIntent) -> Unit,
        goBack: () -> Unit
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        uiState.message?.let { res ->
            val message = stringResource(res)
            LaunchedEffect(res) {
                snackbarHostState.showSnackbar(message)
                onIntent(PremiumIntent.DismissMessage)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    AppBar(
                        title = stringResource(Res.string.premium_title),
                        leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onLeftButtonClick = goBack,
                        leftButtonDescription = stringResource(Res.string.come_back)
                    )
                }
            ) { paddingValues ->
                when {
                    uiState.isLoading && uiState.usage == null -> Box(Modifier.padding(paddingValues)) { LoadingScreen() }
                    uiState.loadFailed -> LoadFailed(
                        modifier = Modifier.padding(paddingValues),
                        onRetry = { onIntent(PremiumIntent.Load) }
                    )
                    else -> Content(
                        uiState = uiState,
                        onIntent = onIntent,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }

            if (uiState.isWorking) LoadingOverlay()
        }
    }

    @Composable
    private fun Content(uiState: PremiumState, onIntent: (PremiumIntent) -> Unit, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isPremium) {
                ActiveCard(premiumUntil = uiState.premiumUntil)
            } else {
                Header()
            }

            uiState.usage?.let { UsageCard(it) }

            when {
                uiState.isPremium -> if (uiState.canPurchase) ManageButton()
                uiState.isAwaitingServer -> Notice(Res.string.premium_processing)
                else -> {
                    Benefits()
                    Plans(uiState = uiState, onIntent = onIntent)
                }
            }
        }
    }

    @Composable
    private fun Header() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(Res.string.premium_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun ActiveCard(premiumUntil: String?) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = AppColors.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.premium_active_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.onPrimaryContainer
                    )
                    premiumUntil?.let {
                        Text(
                            text = stringResource(Res.string.premium_active_until, shortDateLabel(it.take(ISO_DATE_LENGTH))),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun UsageCard(usage: AiUsage) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.premium_usage_title),
                    style = MaterialTheme.typography.titleMedium
                )
                usage.receiptScans?.let { AllowanceRow(Res.string.premium_scans_left, it) }
                usage.categorizations?.let { AllowanceRow(Res.string.premium_categorizations_left, it) }
                Text(
                    text = stringResource(Res.string.premium_scans_resets, shortDateLabel(usage.resetsOn)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun AllowanceRow(text: StringResource, allowance: Allowance) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(text, allowance.remaining, allowance.limit),
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { if (allowance.limit > 0) allowance.used.toFloat() / allowance.limit else 1f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun Benefits() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Benefit(Icons.AutoMirrored.Filled.ReceiptLong, Res.string.premium_benefit_scans)
            Benefit(Icons.Default.AutoAwesome, Res.string.premium_benefit_more)
            Benefit(Icons.Default.CheckCircle, Res.string.premium_benefit_support)
        }
    }

    @Composable
    private fun Benefit(icon: ImageVector, text: StringResource) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = AppColors.primary)
            Spacer(Modifier.width(12.dp))
            Text(text = stringResource(text), style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun Plans(uiState: PremiumState, onIntent: (PremiumIntent) -> Unit) {
        when {
            !uiState.canPurchase -> Notice(Res.string.premium_unavailable)
            uiState.offersFailed -> {
                Notice(Res.string.premium_offers_failed)
                OutlinedButton(onClick = { onIntent(PremiumIntent.Load) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.premium_retry))
                }
            }
            else -> {
                // Yearly first: it is the better deal and the one worth defaulting to
                uiState.offers.sortedByDescending { it.period == PremiumPeriod.ANNUAL }.forEach { offer ->
                    OfferButton(offer = offer, enabled = !uiState.isWorking) {
                        onIntent(PremiumIntent.Purchase(offer.id))
                    }
                }
                Text(
                    text = stringResource(Res.string.premium_fine_print),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.onSurfaceVariant
                )
            }
        }
        if (uiState.canPurchase) {
            TextButton(
                onClick = { onIntent(PremiumIntent.Restore) },
                enabled = !uiState.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.premium_restore))
            }
        }
    }

    @Composable
    private fun OfferButton(offer: PremiumOffer, enabled: Boolean, onClick: () -> Unit) {
        val name = when (offer.period) {
            PremiumPeriod.MONTHLY -> stringResource(Res.string.premium_plan_monthly)
            PremiumPeriod.ANNUAL -> stringResource(Res.string.premium_plan_annual)
            PremiumPeriod.OTHER -> stringResource(Res.string.premium_plan_other)
        }
        val price = when (offer.period) {
            PremiumPeriod.MONTHLY -> stringResource(Res.string.premium_per_month, offer.price)
            PremiumPeriod.ANNUAL -> stringResource(Res.string.premium_per_year, offer.price)
            PremiumPeriod.OTHER -> offer.price
        }
        val button: @Composable (@Composable () -> Unit) -> Unit = { content ->
            if (offer.period == PremiumPeriod.ANNUAL) {
                Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { content() }
            } else {
                OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { content() }
            }
        }
        button {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(text = "$name · $price", fontWeight = FontWeight.SemiBold)
                offer.freeTrialDays?.let { days ->
                    Text(
                        text = stringResource(Res.string.premium_free_trial, days),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Composable
    private fun ManageButton() {
        val uriHandler = LocalUriHandler.current
        OutlinedButton(
            onClick = { uriHandler.openUri(PLAY_SUBSCRIPTIONS_URL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.premium_manage))
        }
    }

    @Composable
    private fun Notice(text: StringResource) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun LoadFailed(modifier: Modifier, onRetry: () -> Unit) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Notice(Res.string.premium_load_failed)
            Button(onClick = onRetry) { Text(stringResource(Res.string.premium_retry)) }
        }
    }

    private const val ISO_DATE_LENGTH = 10
}
