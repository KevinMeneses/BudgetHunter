package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.theme.AppColors

@Composable
fun AppBar(
    title: String,
    leftButtonIcon: ImageVector? = null,
    rightButtonIcon: ImageVector? = null,
    leftButtonDescription: String? = null,
    rightButtonDescription: String? = null,
    onLeftButtonClick: (() -> Unit)? = null,
    onRightButtonClick: (() -> Unit)? = null,
    animateLeftButton: Boolean = false,
    animateRightButton: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LeftButton(
            leftIcon = leftButtonIcon,
            contentDescription = leftButtonDescription,
            onLeftIconClick = onLeftButtonClick,
            animate = animateLeftButton
        )
        Title(title)
        RightButton(
            rightIcon = rightButtonIcon,
            contentDescription = rightButtonDescription,
            onRightIconClick = onRightButtonClick,
            animate = animateRightButton
        )
    }
}

@Composable
private fun BoxScope.RightButton(
    rightIcon: ImageVector?,
    contentDescription: String?,
    onRightIconClick: (() -> Unit)?,
    animate: Boolean
) {
    if (rightIcon != null) {
        Icon(
            imageVector = rightIcon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(end = 10.dp)
                .align(Alignment.CenterEnd)
                .pulsateEffect(animate)
                .blinkEffect(animate)
                .clickable { onRightIconClick?.invoke() },
            tint = AppColors.onSecondaryContainer
        )
    }
}

@Composable
private fun BoxScope.Title(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.Companion
            .align(Alignment.Center)
            .padding(vertical = 15.dp)
    )
}

@Composable
private fun BoxScope.LeftButton(
    leftIcon: ImageVector?,
    contentDescription: String?,
    onLeftIconClick: (() -> Unit)?,
    animate: Boolean
) {
    if (leftIcon != null) {
        Icon(
            imageVector = leftIcon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(start = 10.dp)
                .align(Alignment.CenterStart)
                .pulsateEffect(animate)
                .blinkEffect(animate)
                .clickable { onLeftIconClick?.invoke() },
            tint = AppColors.onSecondaryContainer
        )
    }
}
