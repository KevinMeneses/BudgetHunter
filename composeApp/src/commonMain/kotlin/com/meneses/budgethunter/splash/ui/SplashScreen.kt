package com.meneses.budgethunter.splash.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budget_hunter
import budgethunter.composeapp.generated.resources.wait_for_update
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashState
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
object SplashScreen {
    @Composable
    fun Show(
        uiState: SplashState,
        onEvent: (SplashEvent) -> Unit,
        showBudgetList: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(Res.string.budget_hunter),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onBackground,
                style = Typography.titleLarge
            )

            if (uiState.updatingApp) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp),
                    text = stringResource(Res.string.wait_for_update),
                    fontSize = 16.sp
                )
            }
        }

        LaunchedEffect(uiState.navigate) {
            if (!uiState.navigate) {
                onEvent(SplashEvent.VerifyUpdate)
            } else {
                delay(200)
                showBudgetList()
            }
        }
    }
}
