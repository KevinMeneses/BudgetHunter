package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.meneses.budgethunter.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    title: String,
    leftButtonIcon: ImageVector? = null,
    rightButtonIcon: ImageVector? = null,
    secondRightButtonIcon: ImageVector? = null,
    leftButtonDescription: String? = null,
    rightButtonDescription: String? = null,
    secondRightButtonDescription: String? = null,
    onLeftButtonClick: (() -> Unit)? = null,
    onRightButtonClick: (() -> Unit)? = null,
    onSecondRightButtonClick: (() -> Unit)? = null,
    animateLeftButton: Boolean = false,
    animateRightButton: Boolean = false,
    rightButtonDropdownContent: (@Composable () -> Unit)? = null,
    /** A second line under the title, e.g. the budget a screen belongs to. */
    subtitle: String? = null,
    /** Extra content after the right buttons, for actions that are not a plain icon. */
    trailingContent: (@Composable () -> Unit)? = null
) {
    // Title on the left, as Android lays out its app bars; the actions stay together on the right
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = AppColors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (leftButtonIcon != null) {
                IconButton(
                    modifier = Modifier
                        .pulsateEffect(animateLeftButton)
                        .blinkEffect(animateLeftButton),
                    onClick = {
                        onLeftButtonClick?.invoke()
                    }
                ) {
                    Icon(
                        imageVector = leftButtonIcon,
                        contentDescription = leftButtonDescription,
                        tint = AppColors.onSurface
                    )
                }
            }
        },
        actions = {
            Row {
                if (secondRightButtonIcon != null) {
                    IconButton(
                        onClick = {
                            onSecondRightButtonClick?.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = secondRightButtonIcon,
                            contentDescription = secondRightButtonDescription,
                            tint = AppColors.onSurface
                        )
                    }
                }
                if (rightButtonIcon != null) {
                    Box {
                        IconButton(
                            modifier = Modifier
                                .pulsateEffect(animateRightButton)
                                .blinkEffect(animateRightButton),
                            onClick = {
                                onRightButtonClick?.invoke()
                            }
                        ) {
                            Icon(
                                imageVector = rightButtonIcon,
                                contentDescription = rightButtonDescription,
                                tint = AppColors.onSurface
                            )
                        }
                        rightButtonDropdownContent?.invoke()
                    }
                }
                trailingContent?.invoke()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.surface,
            titleContentColor = AppColors.onSurface,
            navigationIconContentColor = AppColors.onSurface,
            actionIconContentColor = AppColors.onSurface
        ),
        modifier = modifier
    )
}
