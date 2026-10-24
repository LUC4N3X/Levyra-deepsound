package com.luc4n3x.levyra.ui.rewind

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.RewindRepository
import com.luc4n3x.levyra.domain.RewindSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RewindState(
    val years: List<Int> = emptyList(),
    val year: Int = LocalDate.now().year,
    val summary: RewindSummary? = null,
    val loading: Boolean = true,
    val failed: Boolean = false
)

class RewindViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewindRepository(application)
    private val mutableState = MutableStateFlow(RewindState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun load(year: Int? = null) {
        job?.cancel()
        mutableState.value = mutableState.value.copy(loading = true, failed = false)
        job = viewModelScope.launch {
            try {
                val years = repository.years()
                val selected = year?.takeIf { it in years } ?: years.firstOrNull() ?: LocalDate.now().year
                mutableState.value = RewindState(years, selected, mutableState.value.summary?.takeIf { it.year == selected }, loading = true)
                val summary = repository.summary(selected)
                mutableState.value = RewindState(years, selected, summary, loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, failed = true)
            }
        }
    }
}
