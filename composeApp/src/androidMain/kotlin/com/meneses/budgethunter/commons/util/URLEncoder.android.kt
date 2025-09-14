package com.meneses.budgethunter.commons.util

import java.net.URLDecoder
import java.net.URLEncoder

actual fun encodeURL(string: String, encoding: String): String {
    return URLEncoder.encode(string, encoding)
}

actual fun decodeURL(string: String, encoding: String): String {
    return URLDecoder.decode(string, encoding)
}
