package com.meneses.budgethunter.commons.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Load raw resource content as string
 */
// create expect/actual function for KMP and add iOS implementation on migration
@Composable
fun loadRawResourceAsString(resId: Any): String? {
    val resources = LocalResources.current
    var jsonContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resources) {
        jsonContent = resources.openRawResource(resId as Int).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }

    return jsonContent
}
