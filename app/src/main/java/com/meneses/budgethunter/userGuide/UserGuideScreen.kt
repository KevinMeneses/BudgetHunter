package com.meneses.budgethunter.userGuide

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.EMPTY
import com.meneses.budgethunter.commons.ui.AppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination
fun UserGuideScreen(
    navigator: DestinationsNavigator
) {
    val userGuidePages = remember { getUserGuidePages() }
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.user_guide),
                leftButtonIcon = Icons.Default.ArrowBack,
                onLeftButtonClick = { navigator.popBackStack() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(userGuidePages.size) {
                Image(
                    painter = painterResource(id = userGuidePages[it]),
                    contentDescription = EMPTY,
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
    R.drawable.user_guide_page_15,
)
