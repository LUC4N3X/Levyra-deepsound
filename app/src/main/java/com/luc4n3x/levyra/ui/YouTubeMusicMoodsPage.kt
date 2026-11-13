package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

private const val YouTubeMusicMoodColumns = 2
private val YouTubeMusicMoodGap = 12.dp

private val YouTubeMusicMoodStripes = listOf(
    Color(0xFFFF8A3D), Color(0xFFCC0000), Color(0xFF9CE7A0), Color(0xFFD0D0D0), Color(0xFFFFE57A),
    Color(0xFFA27BF0), Color(0xFF707070), Color(0xFF14A80F), Color(0xFF9AB7FF), Color(0xFF6E3CD8),
    Color(0xFF1E6BFF), Color(0xFFFFC107), Color(0xFFF46BEF), Color(0xFFE85D04), Color(0xFF42E8F5), Color(0xFF0B96B0)
)

private fun ExploreCategory.youTubeMusicStripe(): Color =
    if (stripeColor != 0L) Color(stripeColor.toInt())
    else YouTubeMusicMoodStripes[title.lowercase().hashCode().mod(YouTubeMusicMoodStripes.size)]

@Composable
internal fun YouTubeMusicMoodsPage(
    sections: List<ExploreCategorySection>,
    isLoading: Boolean,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gutter = YouTubeMusicDesign.gutter
    LazyColumn(
        Modifier.fillMaxSize().background(YouTubeMusicDesign.background),
        contentPadding = PaddingValues(top = statusTop, bottom = bottomInset + 24.dp)
    ) {
        item(key = "ytm-moods-bar", contentType = "ytm-moods-bar") {
            Row(Modifier.fillMaxWidth().height(YouTubeMusicDesign.topBarHeight).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onBack)
            }
        }
        item(key = "ytm-moods-title", contentType = "ytm-moods-title") {
            Text(
                strings.exploreMoods, color = YouTubeMusicDesign.text, fontSize = 40.sp, lineHeight = 44.sp,
                fontWeight = FontWeight.Medium, maxLines = 3,
                modifier = Modifier.padding(horizontal = gutter).padding(bottom = 8.dp).semantics { heading() }
            )
        }
        if (isLoading && sections.isEmpty()) {
            item(key = "ytm-moods-loading", contentType = "ytm-moods-loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(Modifier.size(32.dp), color = YouTubeMusicDesign.text)
                }
            }
        }
        sections.forEach { section ->
            item(key = "ytm-moods-header-${section.key}", contentType = "ytm-moods-header") {
                Text(
                    section.providerTitle.ifBlank { strings.exploreMoods }, color = YouTubeMusicDesign.text,
                    fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                    modifier = Modifier.padding(horizontal = gutter).padding(top = 32.dp, bottom = 16.dp).semantics { heading() }
                )
            }
            section.categories.chunked(YouTubeMusicMoodColumns).forEachIndexed { rowIndex, row ->
                item(key = "ytm-moods-row-${section.key}-$rowIndex", contentType = "ytm-moods-row") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = gutter).padding(bottom = YouTubeMusicMoodGap),
                        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicMoodGap)
                    ) {
                        row.forEach { category ->
                            YouTubeMusicMoodTile(category.title, category.youTubeMusicStripe(), { onOpenCategory(category) }, Modifier.weight(1f))
                        }
                        repeat(YouTubeMusicMoodColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
