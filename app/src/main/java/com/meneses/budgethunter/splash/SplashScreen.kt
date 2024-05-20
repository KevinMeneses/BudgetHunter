package com.meneses.budgethunter.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.meneses.budgethunter.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        SplashScreen.Show {}
    }
}

@Serializable
object SplashScreen {
    @Composable
    fun Show(
        myViewModel: SplashScreenViewModel = viewModel(),
        onNavigate: () -> Unit
    ) {
        val context = LocalContext.current
        val uiState = myViewModel.uiState.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.budget_hunter),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onBackground,
                style = Typography.titleLarge
            )

            if (uiState.value.updatingApp) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp),
                    text = stringResource(R.string.wait_for_update),
                    fontSize = 16.sp
                )
            }
        }

        LaunchedEffect(uiState.value.navigate) {
            if (!uiState.value.navigate) {
                SplashEvent
                    .VerifyUpdate(context)
                    .run(myViewModel::sendEvent)
            } else {
                delay(1000)
                onNavigate()
            }
        }
    }
}

