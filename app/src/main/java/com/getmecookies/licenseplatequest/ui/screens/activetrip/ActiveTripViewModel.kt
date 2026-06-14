package com.getmecookies.licenseplatequest.ui.screens.activetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getmecookies.licenseplatequest.data.map.MapRepository
import com.getmecookies.licenseplatequest.data.repository.AchievementRepository
import com.getmecookies.licenseplatequest.data.repository.RegionRepository
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.data.repository.SpottingRepository
import com.getmecookies.licenseplatequest.data.repository.TripRepository
import com.getmecookies.licenseplatequest.domain.AlbersUsaProjection
import com.getmecookies.licenseplatequest.domain.Analytics
import com.getmecookies.licenseplatequest.domain.NoOpAnalytics
import com.getmecookies.licenseplatequest.domain.CelebrationTracker
import com.getmecookies.licenseplatequest.domain.CityLocator
import com.getmecookies.licenseplatequest.domain.MapPoint
import com.getmecookies.licenseplatequest.domain.UiPreferences
import com.getmecookies.licenseplatequest.domain.model.FoundState
import com.getmecookies.licenseplatequest.domain.model.StateSummary
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import com.getmecookies.licenseplatequest.ui.screens.celebration.CelebrationMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
 * found yet (hidden when the "only show found" toggle is on).
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
    /** Ordered state codes of the trip's stops, for the map route overlay (playtest #11). */
    val routeStops: List<String> = emptyList(),
    /**
     * Per-stop real-city map positions (parallel to [routeStops]); null = not resolved/geocoded,
     * so the map falls back to that state's center. Lets the route pin actual cities.
     */
    val routeCityPoints: List<MapPoint?> = emptyList(),
    /** Found states whose fill animation hasn't played yet — the map animates these (playtest #20). */
    val pendingCelebrations: Set<String> = emptySet(),
    /** The rare-plate state codes, for the "Rare" badge on the list rows (rare-plate moments). */
    val rareCodes: Set<String> = emptySet(),
    /** The (filtered + sorted) rows shown in the sheet, per the Found/Unfound section toggles. */
    val states: List<StateRow> = emptyList(),
    val sort: FoundSort = FoundSort.ORDER_FOUND,
    val searchQuery: String = "",
    /** Section toggles (both default on, remembered across sessions). */
    val showFound: Boolean = true,
    val showUnfound: Boolean = true,
    /** Counts of states matching the search that are hidden by a section toggle (drives the hint). */
    val hiddenFoundMatches: Int = 0,
    val hiddenUnfoundMatches: Int = 0,
    /** Which top tab is showing; restored from [UiPreferences] when the screen opens. */
    val selectedTab: ActiveTripTab = ActiveTripTab.MAP,
    /** One-time first-run tip on the map ("tap a state to mark it"); dismissed on tap or first find. */
    val showMapHint: Boolean = false,
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
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveTripViewModel(
    mapRepository: MapRepository,
    private val tripRepository: TripRepository,
    private val spottingRepository: SpottingRepository,
    private val regionRepository: RegionRepository,
    private val achievementRepository: AchievementRepository,
    private val cityLocator: CityLocator,
    private val celebrationTracker: CelebrationTracker,
    private val uiPreferences: UiPreferences,
    settingsRepository: SettingsRepository,
    private val analytics: Analytics = NoOpAnalytics,
) : ViewModel() {

    /** Whether the per-find haptic should fire (Settings toggle). */
    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled

    private val sort = MutableStateFlow(FoundSort.ORDER_FOUND)
    private val searchQuery = MutableStateFlow("")

    // Section toggles restored from prefs so the list opens as the user last left it.
    private val showFound = MutableStateFlow(uiPreferences.listShowFound)
    private val showUnfound = MutableStateFlow(uiPreferences.listShowUnfound)
    // Restore the last-used tab so re-entering a trip shows Map or List as the user left it.
    private val initialTab = ActiveTripTab.entries.getOrElse(uiPreferences.activeTripTab) { ActiveTripTab.MAP }
    private val _uiState = MutableStateFlow(
        ActiveTripUiState(
            selectedTab = initialTab,
            showMapHint = !uiPreferences.onboardingMapHintSeen,
        ),
    )
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    /**
     * One-shot per-state confetti signals. A Channel (not state) so each burst fires exactly
     * once and is never replayed when the screen re-enters composition (e.g. returning from
     * State Detail without marking anything).
     */
    private val _confettiEvents = Channel<Unit>(Channel.BUFFERED)
    val confettiEvents: Flow<Unit> = _confettiEvents.receiveAsFlow()

    /** One-shot: a rare plate was just marked — emits the state name for an extra flourish. */
    private val _rareFindEvents = Channel<String>(Channel.BUFFERED)
    val rareFindEvents: Flow<String> = _rareFindEvents.receiveAsFlow()

    /** One-shot: a state brand-new to the lifetime collection was just caught — emits its name. */
    private val _newCollectionEvents = Channel<String>(Channel.BUFFERED)
    val newCollectionEvents: Flow<String> = _newCollectionEvents.receiveAsFlow()

    /**
     * Newly-found (code to name) pairs awaiting a lifetime-collection check. Consumed by a single
     * collector (see init) so the (DB) check runs off the find-processing pipeline, mirroring the
     * achievement decoupling.
     */
    private val _newCollectionCandidates = Channel<Pair<String, String>>(Channel.BUFFERED)

    /** One-shot: ids of achievements just unlocked, for a celebratory toast. */
    private val _achievementEvents = Channel<List<String>>(Channel.BUFFERED)
    val achievementEvents: Flow<List<String>> = _achievementEvents.receiveAsFlow()

    /**
     * Requests for an achievement re-evaluation. Conflated + consumed by a single collector (see
     * init) so the (DB-touching) evaluation always runs on its own coroutine — never inline inside
     * the state pipeline that processes finds — and never overlaps itself. This keeps the reactive
     * found-states pipeline isolated from achievement bookkeeping.
     */
    private val _achievementTrigger = Channel<Unit>(Channel.CONFLATED)

    // Geocoded + projected city positions cached per "city|code" so re-entering a trip (or a
    // pending-celebration refresh) never re-geocodes the same stop. Null = a known miss (fall back).
    private val cityPointCache = mutableMapOf<String, MapPoint?>()

    /** Geocode a stop's city within its state and project it to map space, or null to fall back. */
    private suspend fun resolveCityPoint(city: String, code: String): MapPoint? {
        if (city.isBlank()) return null
        val key = "${city.trim().lowercase()}|$code"
        if (cityPointCache.containsKey(key)) return cityPointCache[key]
        val point = try {
            cityLocator.locate(city, code)?.let { AlbersUsaProjection.project(it.lat, it.lng) }
        } catch (e: Exception) {
            null
        }
        cityPointCache[key] = point
        return point
    }

    /** Signal that something may have unlocked an achievement; the dedicated collector handles it. */
    private fun reevaluateAchievements() {
        _achievementTrigger.trySend(Unit)
    }

    // Track the previous found count per trip so we only react to genuine increases.
    private var prevTripId: UUID? = null
    private var prevCount: Int? = null

    // The rare-plate codes (static), and the previous found set per trip for rare-find detection.
    private var rareCodes: Set<String> = emptySet()
    private var rareBaselineTripId: UUID? = null
    private var prevRareFound: Set<String>? = null

    // Per-trip baseline for detecting newly-found codes to check against the lifetime collection.
    private var collectionBaselineTripId: UUID? = null
    private var prevCollectionFound: Set<String>? = null

    init {
        viewModelScope.launch {
            val shapes = mapRepository.loadShapes()
            _uiState.update { it.copy(shapes = shapes) }
        }
        viewModelScope.launch {
            rareCodes = regionRepository.getRareCodes()
            _uiState.update { it.copy(rareCodes = rareCodes) }
        }
        viewModelScope.launch {
            // Single consumer for achievement re-evaluations: keeps the (DB) work off the
            // find-processing pipeline and contains any failure so it can't disturb gameplay.
            _achievementTrigger.receiveAsFlow().collect {
                val newly = try {
                    achievementRepository.evaluateAndPersist()
                } catch (e: Exception) {
                    emptySet()
                }
                if (newly.isNotEmpty()) _achievementEvents.send(newly.toList())
            }
        }
        viewModelScope.launch {
            // Single consumer for the lifetime-collection check: keeps the (DB) read off the
            // find-processing pipeline; emits a flourish only for a brand-new lifetime catch.
            _newCollectionCandidates.receiveAsFlow().collect { (code, name) ->
                val isNew = try {
                    spottingRepository.isNewToCollection(code)
                } catch (e: Exception) {
                    false
                }
                if (isNew) _newCollectionEvents.send(name)
            }
        }
        viewModelScope.launch {
            // The active trip's ordered stops (code + typed city) for the map route overlay
            // (playtest #11). The codes drive the line/centroid fallback immediately; each city is
            // then geocoded + projected to pin its real location (collectLatest cancels stale work).
            tripRepository.observeActiveTrip()
                .flatMapLatest { trip ->
                    if (trip == null) flowOf(emptyList()) else tripRepository.observeStopPlacesForTrip(
                        trip.id
                    )
                }
                .collectLatest { places ->
                    _uiState.update {
                        it.copy(
                            routeStops = places.map { p -> p.code },
                            routeCityPoints = List<MapPoint?>(places.size) { null }, // reset; resolved below
                        )
                    }
                    val points = ArrayList<MapPoint?>(places.size)
                    for (place in places) points.add(resolveCityPoint(place.city, place.code))
                    _uiState.update { it.copy(routeCityPoints = points) }
                }
        }
        viewModelScope.launch {
            // Found states still awaiting their map animation (playtest #20) — fed straight to the
            // map, which animates them then acknowledges via [onCelebrationsAnimated].
            spottingRepository.observeUncelebratedCodesForActiveTrip()
                .collect { codes -> _uiState.update { it.copy(pendingCelebrations = codes) } }
        }
        viewModelScope.launch {
            // The trip + its spottings + the full state list on one side; the sheet's view
            // controls (sort/search/section toggles) on the other. Combined so the list re-filters
            // live as the user types or toggles.
            val tripData = combine(
                tripRepository.observeActiveTrip(),
                spottingRepository.observeFoundStatesForActiveTrip(),
                spottingRepository.observeAllStates(),
            ) { trip, found, allStates -> Triple(trip, found, allStates) }
            val controls = combine(sort, searchQuery, showFound, showUnfound) { s, q, f, u ->
                Controls(s, q, f, u)
            }

            combine(tripData, controls) { data, control -> data to control }
                .collect { (data, control) ->
                    val (trip, found, allStates) = data
                    detectCelebrations(trip?.id, found.size)
                    detectRareFinds(trip?.id, found)
                    detectNewCollection(trip?.id, found)
                    val list = buildList(found, allStates, control)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            tripId = trip?.id,
                            tripName = trip?.name ?: "",
                            foundCodes = found.map { f -> f.code }.toSet(),
                            states = list.rows,
                            sort = control.sort,
                            searchQuery = control.query,
                            showFound = control.showFound,
                            showUnfound = control.showUnfound,
                            hiddenFoundMatches = list.hiddenFound,
                            hiddenUnfoundMatches = list.hiddenUnfound,
                            mapStats = buildMapStats(trip?.createdAt, found),
                        )
                    }
                }
        }
    }

    /** The sheet's live view controls, combined into one value. */
    private data class Controls(
        val sort: FoundSort,
        val query: String,
        val showFound: Boolean,
        val showUnfound: Boolean,
    )

    /** The visible rows plus the count of search matches hidden by each section toggle. */
    private data class ListResult(
        val rows: List<StateRow>,
        val hiddenFound: Int,
        val hiddenUnfound: Int,
    )

    /**
     * Builds the sheet rows: found states (in [FoundSort] order) followed by unfound states
     * (alphabetical), narrowed by the search query, then by the Found/Unfound section toggles. Also
     * counts how many *matches* are hidden by a toggle so the UI can hint that results exist in a
     * switched-off section.
     */
    private fun buildList(
        found: List<FoundState>,
        allStates: List<StateSummary>,
        controls: Controls,
    ): ListResult {
        val foundRows = found.map { StateRow(it.code, it.name, it.foundAt) } // newest-first
        val foundCodes = found.mapTo(HashSet()) { it.code }
        val unfoundRows = allStates
            .filterNot { it.code in foundCodes }
            .map { StateRow(it.code, it.name, null) }
            .sortedBy { it.name }
        val ordered = when (controls.sort) {
            FoundSort.ORDER_FOUND -> foundRows + unfoundRows
            FoundSort.ALPHABETICAL -> (foundRows + unfoundRows).sortedBy { it.name }
        }
        val q = controls.query.trim()
        val matches =
            if (q.isEmpty()) ordered else ordered.filter { it.name.contains(q, ignoreCase = true) }
        val rows =
            matches.filter { (it.found && controls.showFound) || (!it.found && controls.showUnfound) }
        // Hints only matter while searching: how many matches sit in a switched-off section.
        val hiddenFound = if (q.isEmpty() || controls.showFound) 0 else matches.count { it.found }
        val hiddenUnfound =
            if (q.isEmpty() || controls.showUnfound) 0 else matches.count { !it.found }
        return ListResult(rows, hiddenFound, hiddenUnfound)
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

    /**
     * Emit a one-shot rare-find event when a newly-found state is a rare plate (rare-plate moments).
     * Baseline resets per trip so switching trips never mis-fires.
     */
    private fun detectRareFinds(tripId: UUID?, found: List<FoundState>) {
        val codes = found.mapTo(HashSet()) { it.code }
        if (tripId != rareBaselineTripId) {
            rareBaselineTripId = tripId
            prevRareFound = codes
            return
        }
        val prev = prevRareFound
        prevRareFound = codes
        if (tripId == null || prev == null) return
        (codes - prev).forEach { code ->
            if (code in rareCodes) {
                _rareFindEvents.trySend(found.firstOrNull { it.code == code }?.name ?: code)
            }
        }
    }

    /**
     * Hand each newly-found state to the lifetime-collection check (off-pipeline), which fires a
     * "new for your collection!" flourish for a brand-new lifetime catch. Baseline resets per trip.
     */
    private fun detectNewCollection(tripId: UUID?, found: List<FoundState>) {
        val codes = found.mapTo(HashSet()) { it.code }
        if (tripId != collectionBaselineTripId) {
            collectionBaselineTripId = tripId
            prevCollectionFound = codes
            return
        }
        val prev = prevCollectionFound
        prevCollectionFound = codes
        if (tripId == null || prev == null) return
        (codes - prev).forEach { code ->
            val name = found.firstOrNull { it.code == code }?.name ?: code
            _newCollectionCandidates.trySend(code to name)
        }
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

        // They clearly know how to mark a state now — retire the first-run map hint.
        if (_uiState.value.showMapHint) onDismissMapHint()

        // A new find may unlock achievements (collection/geo/day/rarity milestones).
        reevaluateAchievements()

        if (count >= TOTAL_STATES && !celebrationTracker.hasCelebratedFifty(tripId)) {
            celebrationTracker.markFiftyCelebrated(tripId)
            _uiState.update {
                it.copy(celebration = Celebration(tripId, CelebrationMode.FIFTY_FIFTY))
            }
        } else {
            _confettiEvents.trySend(Unit)
        }
    }

    /**
     * The map finished animating these finds — stamp them celebrated so they won't replay on the
     * next visit (playtest #20). Clears them from [ActiveTripUiState.pendingCelebrations] via the DB.
     */
    fun onCelebrationsAnimated(codes: Set<String>) {
        if (codes.isEmpty()) return
        viewModelScope.launch { spottingRepository.markCelebrated(codes) }
    }

    /** Dismiss the one-time first-run map hint and remember it so it never shows again. */
    fun onDismissMapHint() {
        if (!uiPreferences.onboardingMapHintSeen) uiPreferences.onboardingMapHintSeen = true
        _uiState.update { it.copy(showMapHint = false) }
    }

    /** Switch tabs and remember the choice for next time the user opens a trip. */
    fun onTabSelected(tab: ActiveTripTab) {
        uiPreferences.activeTripTab = tab.ordinal
        analytics.event("tab_selected", mapOf("tab" to tab.name.lowercase()))
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSortChange(newSort: FoundSort) {
        sort.value = newSort
    }

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun onToggleShowFound(show: Boolean) {
        showFound.value = show
        uiPreferences.listShowFound = show
    }

    fun onToggleShowUnfound(show: Boolean) {
        showUnfound.value = show
        uiPreferences.listShowUnfound = show
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
            viewModelScope.launch {
                tripRepository.endTrip(tripId)
                // Completing a trip can unlock achievements (first trip, team effort, 50/50).
                reevaluateAchievements()
            }
        }
    }

    fun onCelebrationConsumed() {
        _uiState.update { it.copy(celebration = null) }
    }

    private companion object {
        const val TOTAL_STATES = 50
    }
}
