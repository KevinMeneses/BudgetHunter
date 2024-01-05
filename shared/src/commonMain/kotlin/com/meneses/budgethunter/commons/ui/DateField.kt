package com.meneses.budgethunter.commons.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R

@Composable
expect fun DateField(
    date: String?,
    label: String = stringResource(id = R.string.date),
    onDateSelected: (String) -> Unit
)
