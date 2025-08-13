package com.meneses.budgethunter.commons.ui

import androidx.compose.ui.text.AnnotatedString
import org.junit.Test
import org.junit.Assert.*

class ThousandSeparatorTransformationTest {

    private val transformation = ThousandSeparatorTransformation()

    @Test
    fun `empty input returns identity transformation`() {
        val input = AnnotatedString("")
        val result = transformation.filter(input)
        
        assertEquals("", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `single digit returns unchanged`() {
        val input = AnnotatedString("5")
        val result = transformation.filter(input)
        
        assertEquals("5", result.text.text)
    }

    @Test
    fun `three digits returns unchanged`() {
        val input = AnnotatedString("123")
        val result = transformation.filter(input)
        
        assertEquals("123", result.text.text)
    }

    @Test
    fun `four digits adds one comma`() {
        val input = AnnotatedString("1234")
        val result = transformation.filter(input)
        
        assertEquals("1,234", result.text.text)
    }

    @Test
    fun `seven digits adds two commas`() {
        val input = AnnotatedString("1234567")
        val result = transformation.filter(input)
        
        assertEquals("1,234,567", result.text.text)
    }

    @Test
    fun `ten digits adds three commas`() {
        val input = AnnotatedString("1234567890")
        val result = transformation.filter(input)
        
        assertEquals("1,234,567,890", result.text.text)
    }

    @Test
    fun `decimal number with four integer digits`() {
        val input = AnnotatedString("1234.56")
        val result = transformation.filter(input)
        
        assertEquals("1,234.56", result.text.text)
    }

    @Test
    fun `decimal number with seven integer digits`() {
        val input = AnnotatedString("1234567.89")
        val result = transformation.filter(input)
        
        assertEquals("1,234,567.89", result.text.text)
    }

    @Test
    fun `trailing decimal point is preserved`() {
        val input = AnnotatedString("1234.")
        val result = transformation.filter(input)
        
        assertEquals("1,234.", result.text.text)
    }

    @Test
    fun `decimal with one digit after point`() {
        val input = AnnotatedString("1234.5")
        val result = transformation.filter(input)
        
        assertEquals("1,234.5", result.text.text)
    }

    @Test
    fun `decimal with two digits after point`() {
        val input = AnnotatedString("1234567.12")
        val result = transformation.filter(input)
        
        assertEquals("1,234,567.12", result.text.text)
    }

    @Test
    fun `cursor position at start remains at start`() {
        val input = AnnotatedString("1234567")
        val result = transformation.filter(input)
        
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `cursor position after first digit maps correctly`() {
        val input = AnnotatedString("1234567")
        val result = transformation.filter(input)
        
        // Position after "1" in "1234567" should be position after "1" in "1,234,567"
        assertEquals(1, result.offsetMapping.originalToTransformed(1))
        assertEquals(1, result.offsetMapping.transformedToOriginal(1))
    }

    @Test
    fun `cursor position after fourth digit accounts for first comma`() {
        val input = AnnotatedString("1234567")
        val result = transformation.filter(input)
        
        // Position after "1234" in "1234567" should be position after "1,234" in "1,234,567"
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun `cursor position at end maps to end`() {
        val input = AnnotatedString("1234567")
        val result = transformation.filter(input)
        
        assertEquals(9, result.offsetMapping.originalToTransformed(7)) // "1,234,567".length = 9
        assertEquals(7, result.offsetMapping.transformedToOriginal(9))
    }

    @Test
    fun `cursor position with decimal point`() {
        val input = AnnotatedString("1234.56")
        val result = transformation.filter(input)
        
        // Position after "1234" (before decimal) should account for comma
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
        
        // Position after decimal point
        assertEquals(6, result.offsetMapping.originalToTransformed(5))
        assertEquals(5, result.offsetMapping.transformedToOriginal(6))
    }

    @Test
    fun `cursor position beyond input length is clamped`() {
        val input = AnnotatedString("1234")
        val result = transformation.filter(input)
        
        assertEquals(5, result.offsetMapping.originalToTransformed(10)) // Should clamp to "1,234".length
        assertEquals(4, result.offsetMapping.transformedToOriginal(10)) // Should clamp to "1234".length
    }

    @Test
    fun `edge case with exactly three digits`() {
        val input = AnnotatedString("999")
        val result = transformation.filter(input)
        
        assertEquals("999", result.text.text)
        assertEquals(3, result.offsetMapping.originalToTransformed(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(3))
    }

    @Test
    fun `edge case with million`() {
        val input = AnnotatedString("1000000")
        val result = transformation.filter(input)
        
        assertEquals("1,000,000", result.text.text)
    }

    @Test
    fun `edge case with decimal at million`() {
        val input = AnnotatedString("1000000.00")
        val result = transformation.filter(input)
        
        assertEquals("1,000,000.00", result.text.text)
    }

    @Test
    fun `cursor mapping handles commas correctly in middle positions`() {
        val input = AnnotatedString("1234567890")
        val result = transformation.filter(input)
        
        // "1234567890" -> "1,234,567,890"
        // Position after "1234" (index 4) should be after "1,234" (index 5)
        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
        
        // Position after "1234567" (index 7) should be after "1,234,567" (index 9)
        assertEquals(9, result.offsetMapping.originalToTransformed(7))
        assertEquals(7, result.offsetMapping.transformedToOriginal(9))
    }

    @Test
    fun `handles zero input`() {
        val input = AnnotatedString("0")
        val result = transformation.filter(input)
        
        assertEquals("0", result.text.text)
    }

    @Test
    fun `handles leading zeros`() {
        val input = AnnotatedString("0001234")
        val result = transformation.filter(input)
        
        assertEquals("0,001,234", result.text.text)
    }
}
