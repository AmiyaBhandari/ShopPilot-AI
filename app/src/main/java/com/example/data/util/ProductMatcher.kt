package com.example.data.util

import com.example.data.local.entity.ProductEntity

data class ProductMatchResult(
    val exactMatch: ProductEntity? = null,
    val candidates: List<ProductEntity> = emptyList(),
    val isAmbiguous: Boolean = false,
    val isUnmatched: Boolean = false
)

object ProductMatcher {

    /**
     * Match query product name / code against catalog using a strict multi-tier hierarchy:
     * 1. Exact Barcode / SKU / ID
     * 2. Exact Name (case-insensitive, trimmed)
     * 3. Normalized Name (cleaned punctuation and standard unit aliases)
     * 4. Token Substring / Fuzzy match
     * 5. Disambiguation if multiple strong candidates
     */
    fun matchProduct(query: String, catalog: List<ProductEntity>): ProductMatchResult {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || catalog.isEmpty()) {
            return ProductMatchResult(isUnmatched = true)
        }

        // 1. Exact Barcode / SKU / ID match
        val idOrCodeMatch = catalog.find {
            (it.barcode.isNotBlank() && it.barcode.equals(trimmedQuery, ignoreCase = true)) ||
            (it.sku.isNotBlank() && it.sku.equals(trimmedQuery, ignoreCase = true)) ||
            it.id.equals(trimmedQuery, ignoreCase = true)
        }
        if (idOrCodeMatch != null) {
            return ProductMatchResult(exactMatch = idOrCodeMatch)
        }

        // 2. Exact Name match (case-insensitive)
        val exactNameMatches = catalog.filter {
            it.name.trim().equals(trimmedQuery, ignoreCase = true)
        }
        if (exactNameMatches.size == 1) {
            return ProductMatchResult(exactMatch = exactNameMatches.first())
        } else if (exactNameMatches.size > 1) {
            return ProductMatchResult(candidates = exactNameMatches, isAmbiguous = true)
        }

        // 3. Normalized Name match (normalized whitespace, punctuation, unit aliases)
        val normalizedQuery = normalizeString(trimmedQuery)
        val normalizedExactMatches = catalog.filter {
            normalizeString(it.name) == normalizedQuery
        }
        if (normalizedExactMatches.size == 1) {
            return ProductMatchResult(exactMatch = normalizedExactMatches.first())
        } else if (normalizedExactMatches.size > 1) {
            return ProductMatchResult(candidates = normalizedExactMatches, isAmbiguous = true)
        }

        // 4. Token / Fuzzy Substring match
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        val scoredCandidates = catalog.mapNotNull { prod ->
            val normProdName = normalizeString(prod.name)
            val prodTokens = normProdName.split(" ").filter { it.isNotBlank() }
            
            var matchScore = 0
            if (normProdName.contains(normalizedQuery)) {
                matchScore += 80
            } else if (normalizedQuery.contains(normProdName)) {
                matchScore += 70
            }

            // Check token overlaps
            var matchedTokenCount = 0
            for (qToken in queryTokens) {
                if (prodTokens.any { it == qToken || it.startsWith(qToken) || qToken.startsWith(it) }) {
                    matchedTokenCount++
                }
            }

            val tokenOverlapRatio = if (queryTokens.isNotEmpty()) matchedTokenCount.toDouble() / queryTokens.size else 0.0
            if (tokenOverlapRatio >= 0.5) {
                matchScore += (tokenOverlapRatio * 50).toInt()
            }

            if (matchScore >= 40) {
                Pair(prod, matchScore)
            } else {
                null
            }
        }.sortedByDescending { it.second }

        if (scoredCandidates.isEmpty()) {
            return ProductMatchResult(isUnmatched = true)
        }

        // If top candidate is significantly higher than second
        if (scoredCandidates.size == 1) {
            return ProductMatchResult(exactMatch = scoredCandidates.first().first)
        }

        val topScore = scoredCandidates[0].second
        val secondScore = scoredCandidates[1].second

        if (topScore >= 80 && (topScore - secondScore) >= 25) {
            return ProductMatchResult(exactMatch = scoredCandidates[0].first)
        }

        // Multiple close candidates -> Ambiguous choice
        val topCandidates = scoredCandidates.take(4).map { it.first }
        return ProductMatchResult(candidates = topCandidates, isAmbiguous = true)
    }

    private fun normalizeString(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(" gm ", " g ")
            .replace(" gms ", " g ")
            .replace(" gram ", " g ")
            .replace(" grams ", " g ")
            .replace(" ltr ", " l ")
            .replace(" litre ", " l ")
            .replace(" litres ", " l ")
            .replace(" packet ", " pkt ")
            .replace(" packets ", " pkt ")
            .replace(" pieces ", " pcs ")
            .replace(" piece ", " pcs ")
            .trim()
    }
}
