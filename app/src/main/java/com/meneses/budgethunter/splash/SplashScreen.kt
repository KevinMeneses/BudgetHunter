package com.meneses.budgethunter.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.R
import com.meneses.budgethunter.destinations.BudgetListScreenDestination
import com.meneses.budgethunter.destinations.SplashScreenDestination
import com.meneses.budgethunter.fakeNavigation
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.meneses.budgethunter.theme.Typography
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        SplashScreen(fakeNavigation)
    }
}

@Destination(start = true)
@Composable
fun SplashScreen(
    navigator: DestinationsNavigator
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.budget_hunter),
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.onBackground,
            style = Typography.titleLarge
        )
    }

    LaunchedEffect(Unit) {
        delay(1000)
        navigator.navigate(
            direction = BudgetListScreenDestination,
            builder = {
                popUpTo(
                    route = SplashScreenDestination.route,
                    popUpToBuilder = { inclusive = true }
                )
            }
        )
    }
}