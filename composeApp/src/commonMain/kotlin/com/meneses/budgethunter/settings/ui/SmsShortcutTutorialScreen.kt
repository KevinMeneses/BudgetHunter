package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.sms_shortcut_help_asks_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_asks_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_nothing_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_nothing_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_unrecognized_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_unrecognized_title
import budgethunter.composeapp.generated.resources.sms_shortcut_image_action
import budgethunter.composeapp.generated.resources.sms_shortcut_image_create
import budgethunter.composeapp.generated.resources.sms_shortcut_image_filter
import budgethunter.composeapp.generated.resources.sms_shortcut_image_open
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
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action
import budgethunter.composeapp.generated.resources.sms_shortcut_step_automation
import budgethunter.composeapp.generated.resources.sms_shortcut_step_filter
import budgethunter.composeapp.generated.resources.sms_shortcut_step_trigger
import budgethunter.composeapp.generated.resources.sms_shortcut_step_variable
import budgethunter.composeapp.generated.resources.sms_shortcut_step_variable_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_variable_title
import budgethunter.composeapp.generated.resources.sms_shortcut_test_body
import budgethunter.composeapp.generated.resources.sms_shortcut_test_title
import budgethunter.composeapp.generated.resources.sms_shortcut_tutorial_title
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Step by step guide to set up the Shortcuts automation that hands the bank SMS to the
 * "Register expense from message" App Intent. iOS only: Android reads the SMS itself.
 *
 * The screenshots were taken on iOS 26, in English (drawable) and in Spanish (drawable-es), so the
 * menu names in the texts match what the user sees. The automation filters by the bank name in the
 * message and not by sender, because the sender numbers change and are not the same for everyone.
 */
@Serializable
object SmsShortcutTutorialScreen {

    private const val SHORTCUTS_URL = "shortcuts://"

    @Composable
    fun Show(goBack: () -> Unit) {
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(Res.string.sms_shortcut_tutorial_title),
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onLeftButtonClick = goBack,
                    leftButtonDescription = stringResource(Res.string.come_back)
                )
            }
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
                    image = Res.drawable.sms_shortcut_step_automation,
                    imageDescription = Res.string.sms_shortcut_image_open
                ) {
                    OpenShortcutsButton()
                }

                StepCard(
                    number = 2,
                    title = Res.string.sms_shortcut_step_create_title,
                    body = Res.string.sms_shortcut_step_create_body,
                    image = Res.drawable.sms_shortcut_step_trigger,
                    imageDescription = Res.string.sms_shortcut_image_create
                )

                StepCard(
                    number = 3,
                    title = Res.string.sms_shortcut_step_filter_title,
                    body = Res.string.sms_shortcut_step_filter_body,
                    image = Res.drawable.sms_shortcut_step_filter,
                    imageDescription = Res.string.sms_shortcut_image_filter
                )

                StepCard(
                    number = 4,
                    title = Res.string.sms_shortcut_step_action_title,
                    body = Res.string.sms_shortcut_step_action_body,
                    image = Res.drawable.sms_shortcut_step_action,
                    imageDescription = Res.string.sms_shortcut_image_action
                )

                StepCard(
                    number = 5,
                    title = Res.string.sms_shortcut_step_variable_title,
                    body = Res.string.sms_shortcut_step_variable_body,
                    image = Res.drawable.sms_shortcut_step_variable,
                    imageDescription = Res.string.sms_shortcut_image_variable
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
        image: DrawableResource,
        imageDescription: StringResource,
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

                StepImage(image = image, description = stringResource(imageDescription))
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

    /** A screenshot of the Shortcuts app, cropped to the part the step talks about. */
    @Composable
    private fun StepImage(image: DrawableResource, description: String) {
        Image(
            painter = painterResource(image),
            contentDescription = description,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium
                )
        )
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
            Res.string.sms_shortcut_help_asks_title to Res.string.sms_shortcut_help_asks_body,
            Res.string.sms_shortcut_help_nothing_title to Res.string.sms_shortcut_help_nothing_body,
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
