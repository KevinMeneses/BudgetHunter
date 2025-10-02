package com.meneses.budgethunter.commons.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition

/**
 * KMP-compatible Lottie animation placeholder using Compottie
 * This replaces the Android-only Lottie implementation
 */
@Composable
fun CompottiePlaceholder(
    jsonContent: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.JsonString(jsonContent)
    }

    val progress = animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = iterations
    )

    LottieAnimation(
        composition = compositionResult.value,
        progress = { progress.progress },
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
