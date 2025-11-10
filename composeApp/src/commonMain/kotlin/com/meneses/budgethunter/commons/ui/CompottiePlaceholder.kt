package com.meneses.budgethunter.commons.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import budgethunter.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * KMP-compatible Lottie animation placeholder using Compottie
 * This replaces the Android-only Lottie implementation
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CompottiePlaceholder(
    fileName: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    var jsonContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileName) {
        jsonContent = Res.readBytes("files/$fileName").decodeToString()
    }

    jsonContent?.let { content ->
        val compositionResult = rememberLottieComposition {
            LottieCompositionSpec.JsonString(content)
        }

        val progress = animateLottieCompositionAsState(
            composition = compositionResult.value,
            iterations = iterations
        )

        val painter = rememberLottiePainter(
            composition = compositionResult.value,
            progress = { progress.progress }
        )

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
