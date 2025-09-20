package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    animateRightButton: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
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
                }
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
