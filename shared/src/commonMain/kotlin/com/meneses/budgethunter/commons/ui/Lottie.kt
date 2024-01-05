package com.meneses.budgethunter.commons.ui

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun LottiePlaceholder(
    @RawRes resId: Int,
    modifier: Modifier = Modifier
)
