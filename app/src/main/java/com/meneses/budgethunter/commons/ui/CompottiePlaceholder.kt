package com.meneses.budgethunter.commons.ui

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * KMP-compatible Lottie animation placeholder using Compottie
 * This replaces the Android-only Lottie implementation
 */
@Composable
fun CompottiePlaceholder(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    val context = LocalContext.current
    var jsonContent by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(resId) {
        jsonContent = loadRawResourceAsString(context, resId)
    }
    
    jsonContent?.let { json ->
        val compositionResult = rememberLottieComposition {
            LottieCompositionSpec.JsonString(json)
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
}

/**
 * Load raw resource content as string
 */
private fun loadRawResourceAsString(context: Context, @RawRes resId: Int): String {
    return context.resources.openRawResource(resId).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readText()
        }
    }
}
