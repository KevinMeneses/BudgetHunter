package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.sms_shortcut_bank_keywords
import budgethunter.composeapp.generated.resources.sms_shortcut_bank_senders
import budgethunter.composeapp.generated.resources.sms_shortcut_banks_title
import budgethunter.composeapp.generated.resources.sms_shortcut_copied
import budgethunter.composeapp.generated.resources.sms_shortcut_copy
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_sender_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_sender_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_unrecognized_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_unrecognized_title
import budgethunter.composeapp.generated.resources.sms_shortcut_image_action
import budgethunter.composeapp.generated.resources.sms_shortcut_image_create
import budgethunter.composeapp.generated.resources.sms_shortcut_image_filter
import budgethunter.composeapp.generated.resources.sms_shortcut_image_open
import budgethunter.composeapp.generated.resources.sms_shortcut_image_placeholder
import budgethunter.composeapp.generated.resources.sms_shortcut_image_run
import budgethunter.composeapp.generated.resources.sms_shortcut_image_variable
import budgethunter.composeapp.generated.resources.sms_shortcut_intro_body
import budgethunter.composeapp.generated.resources.sms_shortcut_intro_budget
import budgethunter.composeapp.generated.resources.sms_shortcut_intro_time
import budgethunter.composeapp.generated.resources.sms_shortcut_intro_title
import budgethunter.composeapp.generated.resources.sms_shortcut_open_shortcuts
import budgethunter.composeapp.generated.resources.sms_shortcut_step
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_create_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_create_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_filter_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_filter_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_open_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_open_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_run_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_run_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_variable_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_variable_title
import budgethunter.composeapp.generated.resources.sms_shortcut_test_body
import budgethunter.composeapp.generated.resources.sms_shortcut_test_title
import budgethunter.composeapp.generated.resources.sms_shortcut_tutorial_title
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.sms.domain.BankMessageFilter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Step by step guide to set up the Shortcuts automation that hands the bank SMS to the
 * "Registrar gasto desde mensaje" App Intent. iOS only: Android reads the SMS itself.
 *
 * The images are placeholders on purpose, until real screenshots are taken.
 */
@Serializable
object SmsShortcutTutorialScreen {

    private const val SHORTCUTS_URL = "shortcuts://"

    @Composable
    fun Show(goBack: () -> Unit) {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(Res.string.sms_shortcut_tutorial_title),
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onLeftButtonClick = goBack,
                    leftButtonDescription = stringResource(Res.string.come_back)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IntroCard()

                StepCard(
                    number = 1,
                    title = Res.string.sms_shortcut_step_open_title,
                    body = Res.string.sms_shortcut_step_open_body,
                    image = Res.string.sms_shortcut_image_open
                ) {
                    OpenShortcutsButton()
                }

                StepCard(
                    number = 2,
                    title = Res.string.sms_shortcut_step_create_title,
                    body = Res.string.sms_shortcut_step_create_body,
                    image = Res.string.sms_shortcut_image_create
                )

                StepCard(
                    number = 3,
                    title = Res.string.sms_shortcut_step_filter_title,
                    body = Res.string.sms_shortcut_step_filter_body,
                    image = Res.string.sms_shortcut_image_filter
                ) {
                    BankFilters(snackbarHostState)
                }

                StepCard(
                    number = 4,
                    title = Res.string.sms_shortcut_step_run_title,
                    body = Res.string.sms_shortcut_step_run_body,
                    image = Res.string.sms_shortcut_image_run
                )

                StepCard(
                    number = 5,
                    title = Res.string.sms_shortcut_step_action_title,
                    body = Res.string.sms_shortcut_step_action_body,
                    image = Res.string.sms_shortcut_image_action
                )

                StepCard(
                    number = 6,
                    title = Res.string.sms_shortcut_step_variable_title,
                    body = Res.string.sms_shortcut_step_variable_body,
                    image = Res.string.sms_shortcut_image_variable
                )

                InfoCard(
                    title = Res.string.sms_shortcut_test_title,
                    body = Res.string.sms_shortcut_test_body
                )

                HelpCard()
            }
        }
    }

    @Composable
    private fun IntroCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.sms_shortcut_intro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.sms_shortcut_intro_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.sms_shortcut_intro_time),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.sms_shortcut_intro_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun StepCard(
        number: Int,
        title: StringResource,
        body: StringResource,
        image: StringResource,
        extraContent: @Composable () -> Unit = {}
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepNumber(number)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(Res.string.sms_shortcut_step, number),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                extraContent()

                ImagePlaceholder(stringResource(image))
            }
        }
    }

    @Composable
    private fun StepNumber(number: Int) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    /** Stands in for a screenshot of the Shortcuts app; the text says which one goes here. */
    @Composable
    private fun ImagePlaceholder(description: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .dashedBorder(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium,
                    on = 6.dp,
                    off = 4.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.sms_shortcut_image_placeholder, description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    @Composable
    private fun OpenShortcutsButton() {
        val uriHandler = LocalUriHandler.current
        Button(
            onClick = {
                // Shortcuts ships with iOS, but it can be uninstalled; then nothing opens
                runCatching { uriHandler.openUri(SHORTCUTS_URL) }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(Res.string.sms_shortcut_open_shortcuts))
        }
    }

    @Composable
    private fun BankFilters(snackbarHostState: SnackbarHostState) {
        @Suppress("DEPRECATION") // LocalClipboard needs a platform ClipEntry, plain text is enough here
        val clipboardManager = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val copy: (String) -> Unit = { value ->
            clipboardManager.setText(AnnotatedString(value))
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(getString(Res.string.sms_shortcut_copied, value))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.sms_shortcut_banks_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            BankMessageFilter.ALL.forEach { filter ->
                BankFilterRow(filter = filter, onCopy = copy)
            }
        }
    }

    @Composable
    private fun BankFilterRow(filter: BankMessageFilter, onCopy: (String) -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = filter.bankName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                FilterValues(
                    label = stringResource(Res.string.sms_shortcut_bank_senders),
                    values = filter.senders,
                    onCopy = onCopy
                )
                FilterValues(
                    label = stringResource(Res.string.sms_shortcut_bank_keywords),
                    values = filter.keywords,
                    onCopy = onCopy
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun FilterValues(label: String, values: List<String>, onCopy: (String) -> Unit) {
        if (values.isEmpty()) return

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                val copyDescription = stringResource(Res.string.sms_shortcut_copy, value)
                AssistChip(
                    onClick = { onCopy(value) },
                    label = { Text(value) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = copyDescription,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun InfoCard(title: StringResource, body: StringResource) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    private fun HelpCard() {
        val problems = listOf(
            Res.string.sms_shortcut_help_confirm_title to Res.string.sms_shortcut_help_confirm_body,
            Res.string.sms_shortcut_help_sender_title to Res.string.sms_shortcut_help_sender_body,
            Res.string.sms_shortcut_help_unrecognized_title to Res.string.sms_shortcut_help_unrecognized_body,
            Res.string.sms_shortcut_help_budget_title to Res.string.sms_shortcut_help_budget_body
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.sms_shortcut_help_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                problems.forEach { (problem, solution) ->
                    Row {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(problem),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(solution),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
