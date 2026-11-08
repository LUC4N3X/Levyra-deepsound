package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.ui.i18n.isTrendingSongsForYouTitle

@Composable
internal fun rememberSkinHomeSections(sections: List<IndexedValue<HomeSection>>): List<IndexedValue<HomeSection>> =
    remember(sections) { skinHomeSections(sections) }

internal fun skinHomeSections(sections: List<IndexedValue<HomeSection>>): List<IndexedValue<HomeSection>> =
    sections.filterNot { (_, section) -> isTrendingSongsForYouTitle(section.title) }
