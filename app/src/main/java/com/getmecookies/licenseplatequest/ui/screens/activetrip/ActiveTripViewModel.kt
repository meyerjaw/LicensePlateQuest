package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.CelebrationTracker
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.domain.model.StateSummary
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/** How the found-states list is ordered (SPEC section 6). */
enum class FoundSort { ORDER_FOUND, ALPHABETICAL }

/** The two top tabs on the Active Trip screen. */
enum class ActiveTripTab { MAP, LIST }

/**
 * One row in the Active Trip bottom-sheet list. [foundAt] is null for states the trip hasn't
 * found yet (shown only when the "show unfound" toggle is on).
 */
data class StateRow(
    val code: String,
    val name: String,
    val foundAt: Instant?,
) {
    val found: Boolean get() = foundAt != null
}

/**
 * At-a-glance stats shown in the strip beneath the map (playtest note #21). All derived from the
 * found states and the active trip's start; no extra data needed.
 */
data class MapStats(
    val foundCount: Int = 0,
    val percent: Int = 0,
    val lastFoundName: String? = null,
    val lastFoundAt: Instant? = null,
    val dayOfTrip: Int = 1,
    val foundToday: Int = 0,
)

data class ActiveTripUiState(
    val loading: Boolean = true,
    val tripId: UUID? = null,
    val tripName: String = "",
    val shapes: UsMapShapes? = null,
    val foundCodes: Set<String> = emptySet(),
    /** The (filtered + sorted) rows shown in the sheet — found states, plus unfound when toggled. */
    val states: List<StateRow> = emptyList(),
    val sort: FoundSort = FoundSort.ORDER_FOUND,
    val searchQuery: String = "",
    val showUnfound: Boolean = false,
    /** Which top tab is showing; restored from [UiPreferences] when the screen opens. */
    val selectedTab: ActiveTripTab = ActiveTripTab.MAP,
    /** At-a-glance stats for the strip beneath the map (playtest note #21). */
    val mapStats: MapStats = MapStats(),
    val showEndDialog: Boolean = false,
    /** One-shot: navigate to a celebration for (tripId, mode). Cleared via [ActiveTripViewModel.onCelebrationConsumed]. */
    val celebration: Celebration? = null,
) {
    val foundCount: Int get() = foundCodes.size
}

data class Celebration(val tripId: UUID, val mode: CelebrationMode)

/**
 * Drives the Active Trip View (SPEC section 6). Combines the active trip, its found states, and
 * the chosen sort order into one UI state; the bundled map shapes load once on the side.
 *
 * Celebrations (SPEC section 6/8): when the found count rises it fires a per-state confetti
 * tick; reaching 50 fires the 50/50 celebration exactly once per trip (tracked via
 * [CelebrationTracker] so re-marking can't replay it). Ending a trip routes through the
 * manual-end celebration, which finalizes the trip.
 */
class ActiveTripViewModel(
    mapRepository: MapRepository,
    private val tripRepository: TripRepository,
    spottingRepository: SpottingRepository,
    private val celebrationTracker: CelebrationTracker,
    private val uiPreferences: UiPreferences,
) : ViewModel() {

    private val sort = MutableStateFlow(FoundSort.ORDER_FOUND)
    private val searchQuery = MutableStateFlow("")
    private val showUnfound = MutableStateFlow(false)
    // Restore the last-used tab so re-entering a trip shows Map or List as the user left it.
    private val initialTab = ActiveTripTab.entries.getOrElse(uiPreferences.activeTripTab) { ActiveTripTab.MAP }
    private val _uiState = MutableStateFlow(ActiveTripUiState(selectedTab = initialTab))
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    /**
     * One-shot per-state confetti signals. A Channel (not state) so each burst fires exactly
     * once and is never replayed when the screen re-enters composition (e.g. returning from
     * State Detail without marking anything).
     */
    private val _confettiEvents = Channel<Unit>(Channel.BUFFERED)
    val confettiEvents: Flow<Unit> = _confettiEvents.receiveAsFlow()

    // Track the previous found count per trip so we only react to genuine increases.
    private var prevTripId: UUID? = null
    private var prevCount: Int? = null

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            // The trip + its spottings + the full state list on one side; the sheet's view
            // controls (sort/search/show-unfound) on the other. Combined so the list re-filters
            // live as the user types or toggles.
            val tripData = combine(
                tripRepository.observeActiveTrip(),
                spottingRepository.observeFoundStatesForActiveTrip(),
                spottingRepository.observeAllStates(),
            ) { trip, found, allStates -> Triple(trip, found, allStates) }
            val controls = combine(sort, searchQuery, showUnfound) { s, q, u -> Triple(s, q, u) }

            combine(tripData, controls) { data, control -> data to control }
                .collect { (data, control) ->
                    val (trip, found, allStates) = data
                    val (sortMode, query, showUnfoundSel) = control
                    detectCelebrations(trip?.id, found.size)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            tripId = trip?.id,
                            tripName = trip?.name ?: "",
                            foundCodes = found.map { f -> f.code }.toSet(),
                            states = buildRows(found, allStates, sortMode, query, showUnfoundSel),
                            sort = sortMode,
                            searchQuery = query,
                            showUnfound = showUnfoundSel,
                            mapStats = buildMapStats(trip?.createdAt, found),
                        )
                    }
                }
        }
    }

    /**
     * Builds the sheet rows: found states (in [FoundSort] order), optionally followed by unfound
     * states (alphabetical), then narrowed by the search query against the state name.
     */
    private fun buildRows(
        found: List<FoundState>,
        allStates: List<StateSummary>,
        sortMode: FoundSort,
        query: String,
        showUnfound: Boolean,
    ): List<StateRow> {
        val foundRows = found.map { StateRow(it.code, it.name, it.foundAt) } // newest-first
        val foundCodes = found.mapTo(HashSet()) { it.code }
        val unfoundRows = allStates
            .filterNot { it.code in foundCodes }
            .map { StateRow(it.code, it.name, null) }
            .sortedBy { it.name }
        val ordered = when (sortMode) {
            FoundSort.ORDER_FOUND -> foundRows + unfoundRows
            FoundSort.ALPHABETICAL -> (foundRows + unfoundRows).sortedBy { it.name }
        }
        val visible = if (showUnfound) ordered else ordered.filter { it.found }
        val q = query.trim()
        return if (q.isEmpty()) visible else visible.filter { it.name.contains(q, ignoreCase = true) }
    }

    /** Derive the at-a-glance map stats (playtest note #21) from the found states and trip start. */
    private fun buildMapStats(startInstant: Instant?, found: List<FoundState>): MapStats {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val last = found.maxByOrNull { it.foundAt }
        val dayOfTrip = if (startInstant != null) {
            (ChronoUnit.DAYS.between(startInstant.atZone(zone).toLocalDate(), today) + 1)
                .toInt().coerceAtLeast(1)
        } else {
            1
        }
        return MapStats(
            foundCount = found.size,
            percent = if (found.isEmpty()) 0 else found.size * 100 / TOTAL_STATES,
            lastFoundName = last?.name,
            lastFoundAt = last?.foundAt,
            dayOfTrip = dayOfTrip,
            foundToday = found.count { it.foundAt.atZone(zone).toLocalDate() == today },
        )
    }

    /** Fire per-state confetti and the once-per-trip 50/50 celebration on real count increases. */
    private fun detectCelebrations(tripId: UUID?, count: Int) {
        // Reset the baseline when the active trip changes (avoids cross-trip false positives).
        if (tripId != prevTripId) {
            prevTripId = tripId
            prevCount = count
            return
        }
        val previous = prevCount
        prevCount = count
        if (tripId == null || previous == null || count <= previous) return

        // A new state was just marked found. Clear any active list search so the freshly found
        // state is visible and the box is ready for the next spot (only on a find, never on an
        // unmark, since count only rises here). Playtest note #1.
        if (searchQuery.value.isNotEmpty()) searchQuery.value = ""

        if (count >= TOTAL_STATES && !celebrationTracker.hasCelebratedFifty(tripId)) {
            celebrationTracker.markFiftyCelebrated(tripId)
            _uiState.update {
                it.copy(celebration = Celebration(tripId, CelebrationMode.FIFTY_FIFTY))
            }
        } else {
            _confettiEvents.trySend(Unit)
        }
    }

    /** Switch tabs and remember the choice for next time the user opens a trip. */
    fun onTabSelected(tab: ActiveTripTab) {
        uiPreferences.activeTripTab = tab.ordinal
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSortChange(newSort: FoundSort) {
        sort.value = newSort
    }

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun onToggleShowUnfound(show: Boolean) {
        showUnfound.value = show
    }

    fun onEndTripClick() {
        _uiState.update { it.copy(showEndDialog = true) }
    }

    fun onDismissEndDialog() {
        _uiState.update { it.copy(showEndDialog = false) }
    }

    /**
     * Confirming "End trip" finalizes the trip immediately — so it survives the app being closed
     * before the celebration's "Done" is tapped — and routes to the manual-end celebration, which
     * reads its stats from the now-completed trip. [TripRepository.endTrip] is idempotent, so the
     * celebration's own finalize on "Done" is a harmless no-op.
     */
    fun onConfirmEndTrip() {
        val tripId = _uiState.value.tripId
        _uiState.update {
            it.copy(
                showEndDialog = false,
                celebration = tripId?.let { id -> Celebration(id, CelebrationMode.MANUAL_END) },
            )
        }
        if (tripId != null) {
            viewModelScope.launch { tripRepository.endTrip(tripId) }
        }
    }

    fun onCelebrationConsumed() {
        _uiState.update { it.copy(celebration = null) }
    }

    private companion object {
        const val TOTAL_STATES = 50
    }
}
