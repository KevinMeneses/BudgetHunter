package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        AppBar(
            title = "App Bar",
            leftButtonIcon = Icons.Default.ArrowBack,
            rightButtonIcon = Icons.Default.Add
        )
    }
}

@Composable
fun AppBar(
    title: String,
    leftButtonIcon: ImageVector? = null,
    rightButtonIcon: ImageVector? = null,
    onLeftButtonClick: (() -> Unit)? = null,
    onRightButtonClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LeftButton(leftButtonIcon, onLeftButtonClick)
        Title(title)
        RightButton(rightButtonIcon, onRightButtonClick, AppColors)
    }
}

@Composable
private fun BoxScope.RightButton(
    rightIcon: ImageVector?,
    onRightIconClick: (() -> Unit)?,
    AppColors: ColorScheme
) {
    if (rightIcon != null) {
        Icon(
            imageVector = rightIcon,
            contentDescription = EMPTY,
            modifier = Modifier
                .padding(end = 10.dp)
                .align(Alignment.CenterEnd)
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
    onLeftIconClick: (() -> Unit)?
) {
    if (leftIcon != null) {
        Icon(
            imageVector = leftIcon,
            contentDescription = EMPTY,
            modifier = Modifier
                .padding(start = 10.dp)
                .align(Alignment.CenterStart)
                .clickable { onLeftIconClick?.invoke() },
            tint = AppColors.onSecondaryContainer
        )
    }
}