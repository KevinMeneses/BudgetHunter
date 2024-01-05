package com.meneses.budgethunter.commons.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
actual fun LottiePlaceholder(
    @RawRes resId: Int,
    modifier: Modifier
) {
    val lottieComposition = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(resId)
    )
    val lottieAnimationState = animateLottieCompositionAsState(
        composition = lottieComposition.value,
        iterations = 1
    )
    LottieAnimation(
        composition = lottieComposition.value,
        progress = { lottieAnimationState.progress },
        contentScale = ContentScale.Inside,
        modifier = modifier.sizeIn(
            minHeight = 400.dp,
            maxHeight = 600.dp
        )
    )
}
