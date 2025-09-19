package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.meneses.budgethunter.theme.AppColors

/**
 * Platform-aware AppBar that adapts navigation patterns based on the platform:
 * - iOS: Back arrow (left) + Menu button (right)
 * - Android: Hamburger menu (left) + Action button (right)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformAwareAppBar(
    modifier: Modifier = Modifier,
    title: String,
    rightButtonIcon: ImageVector? = null,
    rightButtonDescription: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    onRightButtonClick: (() -> Unit)? = null,
    animateLeftButton: Boolean = false,
    animateRightButton: Boolean = false
) {
    val currentPlatform = getCurrentPlatform()

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
            when (currentPlatform) {
                Platform.IOS -> {
                    if (onBackClick != null) {
                        IconButton(
                            modifier = Modifier
                                .pulsateEffect(animateLeftButton)
                                .blinkEffect(animateLeftButton),
                            onClick = onBackClick
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back",
                                tint = AppColors.onSurface
                            )
                        }
                    }
                }

                Platform.ANDROID -> {
                    // Android: Show hamburger menu if onMenuClick is provided
                    if (onMenuClick != null) {
                        IconButton(
                            modifier = Modifier
                                .pulsateEffect(animateLeftButton)
                                .blinkEffect(animateLeftButton),
                            onClick = onMenuClick
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu",
                                tint = AppColors.onSurface
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (rightButtonIcon != null && onRightButtonClick != null) {
                IconButton(
                    modifier = Modifier
                        .pulsateEffect(animateRightButton)
                        .blinkEffect(animateRightButton),
                    onClick = onRightButtonClick
                ) {
                    Icon(
                        imageVector = rightButtonIcon,
                        contentDescription = rightButtonDescription,
                        tint = AppColors.onSurface
                    )
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
