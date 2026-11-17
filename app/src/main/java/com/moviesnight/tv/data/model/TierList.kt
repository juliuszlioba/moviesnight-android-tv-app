package com.moviesnight.tv.data.model

/** Tier grades, best first (same order as the website's lib/tiers.ts). */
val Tiers = listOf("S", "A", "B", "C", "D", "F")

/**
 * One section of the anime tier list.
 *
 * @param tier S/A/B/C/D/F, or null for anime that haven't been graded yet.
 */
data class TierGroup(val tier: String?, val records: List<RecordSummary>)

/**
 * Splits anime into tier sections, best tier first, like the website's /anime/tier.
 * [records] must already be in anime-list order; each section keeps that order.
 * Ungraded (or unknown) tiers go last; empty sections are left out.
 */
fun groupByTier(records: List<RecordSummary>): List<TierGroup> {
    val byTier = records.groupBy { it.tier?.takeIf { tier -> tier in Tiers } }
    return (Tiers + listOf(null)).mapNotNull { tier ->
        byTier[tier]?.let { TierGroup(tier, it) }
    }
}
