package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.privacy_policy_accept_checkbox
import budgethunter.composeapp.generated.resources.privacy_policy_link_text
import com.meneses.budgethunter.auth.domain.PrivacyPolicy
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Opens the privacy policy. No browser to open it with: nothing happens rather than a crash. */
fun UriHandler.openPrivacyPolicy() {
    runCatching { openUri(PrivacyPolicy.url) }
}

/**
 * [template] (one `%1$s`) with "privacy policy" in its place as a link to the page. The words are
 * found in the formatted text, so each language puts them where its grammar wants.
 */
@Composable
fun privacyPolicyLinkText(template: StringResource): AnnotatedString {
    val uriHandler = LocalUriHandler.current
    val linkText = stringResource(Res.string.privacy_policy_link_text)
    val fullText = stringResource(template, linkText)
    val start = fullText.indexOf(linkText)
    return buildAnnotatedString {
        if (start < 0) {
            append(fullText)
            return@buildAnnotatedString
        }
        append(fullText.substring(0, start))
        val link = LinkAnnotation.Clickable(
            tag = PrivacyPolicy.url,
            styles = TextLinkStyles(
                style = SpanStyle(
                    color = AppColors.primary,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            ),
            linkInteractionListener = { uriHandler.openPrivacyPolicy() }
        )
        withLink(link) { append(linkText) }
        append(fullText.substring(start + linkText.length))
    }
}

/** A line of text that mentions the privacy policy, with the link in it. */
@Composable
fun PrivacyPolicyNotice(
    template: StringResource,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    textAlign: TextAlign? = null
) {
    Text(
        text = privacyPolicyLinkText(template),
        modifier = modifier,
        fontSize = fontSize,
        color = AppColors.onSurfaceVariant,
        textAlign = textAlign
    )
}

/**
 * Sign up's required checkbox: "I accept the privacy policy". Tapping the row toggles it; tapping
 * the link opens the page instead.
 */
@Composable
fun PrivacyPolicyCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row handles the toggle, so it is read out once
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        PrivacyPolicyNotice(
            template = Res.string.privacy_policy_accept_checkbox,
            fontSize = 14.sp
        )
    }
}
