package com.meneses.budgethunter.settings.ui

import androidx.compose.runtime.Composable
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.email_shortcut_help_asks_body
import budgethunter.composeapp.generated.resources.email_shortcut_help_nothing_body
import budgethunter.composeapp.generated.resources.email_shortcut_help_nothing_title
import budgethunter.composeapp.generated.resources.email_shortcut_help_unrecognized_body
import budgethunter.composeapp.generated.resources.email_shortcut_image_create
import budgethunter.composeapp.generated.resources.email_shortcut_image_filter
import budgethunter.composeapp.generated.resources.email_shortcut_image_variable
import budgethunter.composeapp.generated.resources.email_shortcut_intro_body
import budgethunter.composeapp.generated.resources.email_shortcut_intro_requirements
import budgethunter.composeapp.generated.resources.email_shortcut_step_create_body
import budgethunter.composeapp.generated.resources.email_shortcut_step_create_title
import budgethunter.composeapp.generated.resources.email_shortcut_step_filter
import budgethunter.composeapp.generated.resources.email_shortcut_step_filter_body
import budgethunter.composeapp.generated.resources.email_shortcut_step_filter_title
import budgethunter.composeapp.generated.resources.email_shortcut_step_trigger
import budgethunter.composeapp.generated.resources.email_shortcut_step_variable
import budgethunter.composeapp.generated.resources.email_shortcut_step_variable_body
import budgethunter.composeapp.generated.resources.email_shortcut_step_variable_title
import budgethunter.composeapp.generated.resources.email_shortcut_test_body
import budgethunter.composeapp.generated.resources.email_shortcut_tutorial_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_asks_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_budget_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_body
import budgethunter.composeapp.generated.resources.sms_shortcut_help_confirm_title
import budgethunter.composeapp.generated.resources.sms_shortcut_help_unrecognized_title
import budgethunter.composeapp.generated.resources.sms_shortcut_image_action
import budgethunter.composeapp.generated.resources.sms_shortcut_image_open
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_action_title
import budgethunter.composeapp.generated.resources.sms_shortcut_step_automation
import budgethunter.composeapp.generated.resources.sms_shortcut_step_open_body
import budgethunter.composeapp.generated.resources.sms_shortcut_step_open_title
import budgethunter.composeapp.generated.resources.sms_shortcut_test_title
import kotlinx.serialization.Serializable

/**
 * Step by step guide to set up the Shortcuts automation that hands the bank alert emails to the
 * same "Register expense from message" App Intent as the SMS. iOS only, and the trigger "Email"
 * needs iOS 17 and the account added to Apple Mail.
 *
 * Opening Shortcuts and adding the action are the same as in the SMS guide, so they reuse its texts
 * and screenshots. Unlike the SMS, the automation filters by sender (a bank's alert address is
 * fixed), and it passes the email's Content (its body): the trigger hands over an Email object,
 * and picking the property keeps the text predictable. The screenshots were taken on the iOS 26.2
 * simulator, where Mail does not run, so what a real email turns into has not been checked yet.
 */
@Serializable
object EmailShortcutTutorialScreen {

    @Composable
    fun Show(goBack: () -> Unit) {
        ShortcutTutorialScaffold(title = Res.string.email_shortcut_tutorial_title, goBack = goBack) {
            ShortcutIntroCard(
                body = Res.string.email_shortcut_intro_body,
                extraNotes = listOf(Res.string.email_shortcut_intro_requirements)
            )

            ShortcutStepCard(
                number = 1,
                title = Res.string.sms_shortcut_step_open_title,
                body = Res.string.sms_shortcut_step_open_body,
                image = Res.drawable.sms_shortcut_step_automation,
                imageDescription = Res.string.sms_shortcut_image_open
            ) {
                OpenShortcutsButton()
            }

            ShortcutStepCard(
                number = 2,
                title = Res.string.email_shortcut_step_create_title,
                body = Res.string.email_shortcut_step_create_body,
                image = Res.drawable.email_shortcut_step_trigger,
                imageDescription = Res.string.email_shortcut_image_create
            )

            ShortcutStepCard(
                number = 3,
                title = Res.string.email_shortcut_step_filter_title,
                body = Res.string.email_shortcut_step_filter_body,
                image = Res.drawable.email_shortcut_step_filter,
                imageDescription = Res.string.email_shortcut_image_filter
            )

            ShortcutStepCard(
                number = 4,
                title = Res.string.sms_shortcut_step_action_title,
                body = Res.string.sms_shortcut_step_action_body,
                image = Res.drawable.sms_shortcut_step_action,
                imageDescription = Res.string.sms_shortcut_image_action
            )

            ShortcutStepCard(
                number = 5,
                title = Res.string.email_shortcut_step_variable_title,
                body = Res.string.email_shortcut_step_variable_body,
                image = Res.drawable.email_shortcut_step_variable,
                imageDescription = Res.string.email_shortcut_image_variable
            )

            ShortcutInfoCard(
                title = Res.string.sms_shortcut_test_title,
                body = Res.string.email_shortcut_test_body
            )

            ShortcutHelpCard(
                problems = listOf(
                    Res.string.sms_shortcut_help_confirm_title to Res.string.sms_shortcut_help_confirm_body,
                    Res.string.sms_shortcut_help_asks_title to Res.string.email_shortcut_help_asks_body,
                    Res.string.email_shortcut_help_nothing_title to Res.string.email_shortcut_help_nothing_body,
                    Res.string.sms_shortcut_help_unrecognized_title to Res.string.email_shortcut_help_unrecognized_body,
                    Res.string.sms_shortcut_help_budget_title to Res.string.sms_shortcut_help_budget_body
                )
            )
        }
    }
}
