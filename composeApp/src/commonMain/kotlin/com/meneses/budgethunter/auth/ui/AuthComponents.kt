package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.dismiss
import budgethunter.composeapp.generated.resources.hide_password
import budgethunter.composeapp.generated.resources.password
import budgethunter.composeapp.generated.resources.show_password
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/** The app's mark: the wallet in a rounded yellow tile. */
@Composable
fun AppMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(MaterialTheme.shapes.large)
            .background(AppColors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = AppColors.onPrimaryContainer,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun AuthTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
        // Balanced lines, so a two-line title does not leave a lonely word
        style = TextStyle(lineBreak = LineBreak.Heading),
        modifier = modifier
    )
}

/** Full-width pill button, the shape every action on these screens shares. */
val AuthButtonModifier: Modifier = Modifier
    .fillMaxWidth()
    .height(52.dp)

/** A problem that belongs to no single field (wrong credentials, Google failed...), dismissable. */
@Composable
fun AuthErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(AppColors.errorContainer)
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = AppColors.onErrorContainer
        )
        Text(
            text = message,
            fontSize = 14.sp,
            color = AppColors.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.dismiss),
                tint = AppColors.onErrorContainer
            )
        }
    }
}

/**
 * Password with the eye to check what was typed. The icon shows what a tap does: the open eye
 * shows the password, the crossed one hides it again.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit = {},
    supportingText: String? = null,
    error: String? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.password)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }, onNext = { onImeAction() }),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (isVisible) Res.string.hide_password else Res.string.show_password
                    )
                )
            }
        },
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { text -> { Text(text) } },
        singleLine = true
    )
}

/** "¿No tienes cuenta? Regístrate": the question in plain text, the action as the link. */
@Composable
fun AuthSwitchPrompt(
    question: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = question,
            fontSize = 14.sp,
            color = AppColors.onSurfaceVariant
        )
        Text(
            text = action,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 10.dp)
        )
    }
}
