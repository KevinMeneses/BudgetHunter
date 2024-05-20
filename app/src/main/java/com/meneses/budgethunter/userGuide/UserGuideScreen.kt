package com.meneses.budgethunter.userGuide

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable

@Serializable
object UserGuideScreen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(goBack: () -> Unit) {
        val userGuidePages = remember { getUserGuidePages() }
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(R.string.user_guide),
                    leftButtonIcon = Icons.Default.ArrowBack,
                    leftButtonDescription = stringResource(id = R.string.come_back),
                    onLeftButtonClick = goBack
                )
            }
        ) { paddingValues ->
            var scale by remember { mutableFloatStateOf(1f) }
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .clipToBounds()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            var currentScale = scale
                            currentScale *= zoom
                            scale = currentScale.coerceIn(1f, 3f)
                        }
                    }
            ) {
                items(userGuidePages.size) {
                    Image(
                        painter = painterResource(id = userGuidePages[it]),
                        contentDescription = stringResource(R.string.page_, it),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    private fun getUserGuidePages() = listOf(
        R.drawable.user_guide_page_1,
        R.drawable.user_guide_page_2,
        R.drawable.user_guide_page_3,
        R.drawable.user_guide_page_4,
        R.drawable.user_guide_page_5,
        R.drawable.user_guide_page_6,
        R.drawable.user_guide_page_7,
        R.drawable.user_guide_page_8,
        R.drawable.user_guide_page_9,
        R.drawable.user_guide_page_10,
        R.drawable.user_guide_page_11,
        R.drawable.user_guide_page_12,
        R.drawable.user_guide_page_13,
        R.drawable.user_guide_page_14,
        R.drawable.user_guide_page_15
    )
}

