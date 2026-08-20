package com.example

import com.example.data.local.entity.ProductEntity
import com.example.data.util.ProductMatcher
import org.junit.Assert.*
import org.junit.Test

class ProductMatcherTest {

    private val sampleCatalog = listOf(
        ProductEntity(id = "1", name = "Tata Salt 1kg", barcode = "8901234567890", costPrice = 20.0, sellingPrice = 28.0, currentStock = 50.0),
        ProductEntity(id = "2", name = "Tata Tea Gold 250g", barcode = "8901234567891", costPrice = 110.0, sellingPrice = 140.0, currentStock = 30.0),
        ProductEntity(id = "3", name = "Tata Tea Premium 500g", barcode = "8901234567892", costPrice = 200.0, sellingPrice = 250.0, currentStock = 25.0),
        ProductEntity(id = "4", name = "Fortune Sunlite Oil 1L", barcode = "8901234567893", costPrice = 120.0, sellingPrice = 145.0, currentStock = 40.0),
        ProductEntity(id = "5", name = "Maggi 2-Minute Noodles 70g", barcode = "8901234567894", costPrice = 11.5, sellingPrice = 14.0, currentStock = 100.0)
    )

    @Test
    fun `exact name match resolves directly`() {
        val result = ProductMatcher.matchProduct("Tata Salt 1kg", sampleCatalog)
        assertNotNull(result.exactMatch)
        assertEquals("1", result.exactMatch?.id)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `exact barcode match resolves directly`() {
        val result = ProductMatcher.matchProduct("8901234567893", sampleCatalog)
        assertNotNull(result.exactMatch)
        assertEquals("Fortune Sunlite Oil 1L", result.exactMatch?.name)
    }

    @Test
    fun `normalized name without punctuation matches`() {
        val result = ProductMatcher.matchProduct("maggi 2 minute noodles 70g", sampleCatalog)
        assertNotNull(result.exactMatch)
        assertEquals("5", result.exactMatch?.id)
    }

    @Test
    fun `ambiguous query returns candidates list`() {
        val result = ProductMatcher.matchProduct("Tata Tea", sampleCatalog)
        assertTrue(result.isAmbiguous || result.candidates.size > 1)
        assertTrue(result.candidates.any { it.name.contains("Tata Tea Gold") })
        assertTrue(result.candidates.any { it.name.contains("Tata Tea Premium") })
    }

    @Test
    fun `fuzzy query with minor typo matches`() {
        val result = ProductMatcher.matchProduct("Fortun Sunlite Oil", sampleCatalog)
        assertTrue(result.exactMatch != null || result.candidates.isNotEmpty())
        val matched = result.exactMatch ?: result.candidates.firstOrNull()
        assertEquals("4", matched?.id)
    }
}
