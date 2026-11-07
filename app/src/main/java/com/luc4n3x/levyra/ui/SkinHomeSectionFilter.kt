package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.ui.i18n.isTrendingSongsForYouTitle
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque

@Composable
internal fun rememberSkinHomeSections(sections: List<IndexedValue<HomeSection>>): List<IndexedValue<HomeSection>> {
    val torque = LevyraIsTorque
    return remember(sections, torque) { skinHomeSections(sections, hideTrending = torque) }
}

internal fun skinHomeSections(sections: List<IndexedValue<HomeSection>>, hideTrending: Boolean): List<IndexedValue<HomeSection>> =
    if (hideTrending) sections.filterNot { (_, section) -> isTrendingSongsForYouTitle(section.title) } else sections
