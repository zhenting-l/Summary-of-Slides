package com.lzt.summaryofslides.daily

import kotlinx.serialization.Serializable

@Serializable
data class DailyIndex(
    val updatedAt: String = "",
    val dates: List<String> = emptyList(),
)

@Serializable
data class DailyReport(
    val reportDate: String = "",
    val exportedAt: String = "",
    val papers: List<DailyPaper> = emptyList(),
)

@Serializable
data class DailyPaper(
    val id: String = "",
    val arxivId: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val abstract: String = "",
    val categories: List<String> = emptyList(),
    val primaryCategory: String = "",
    val publishedAt: String = "",
    val updatedAt: String = "",
    val arxivUrl: String = "",
    val pdfUrl: String = "",
    val keywords: List<String> = emptyList(),
    val subfield: String = "",
    val score: Double = 0.0,
    val recommendationReason: String = "",
    val status: String = "unread",
    val isStarred: Boolean = false,
    val notes: String = "",
)

