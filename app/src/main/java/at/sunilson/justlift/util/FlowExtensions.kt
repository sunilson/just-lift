package at.sunilson.justlift.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Runs [block] while the flow has active subscribers. Only stops after [debounceMillis] if no subscribers are present.
 *
 * @param debounceMillis Used to account for quick subscriber fluctuations. Not needed if already using `stateIn` with `WhileSubscribed`
 */
@OptIn(FlowPreview::class)
suspend inline fun MutableSharedFlow<*>.runWhileSubscribed(
    debounceMillis: Long,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    subscriptionCount
        .map { it > 0 }
        .debounce { hasActiveSubscribers -> if (hasActiveSubscribers) 0L else debounceMillis }
        .distinctUntilChanged()
        .collectLatest { hasActiveSubscribers -> if (hasActiveSubscribers) coroutineScope { block() } }
}

/**
 * Collects the [flowToCollect] while the [MutableSharedFlow] has active subscribers. Only stops after [debounceMillis] if no subscribers are present.
 *
 * @param debounceMillis Used to account for quick subscriber fluctuations. Not needed if already using `stateIn` with `WhileSubscribed`
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
suspend inline fun <T> MutableSharedFlow<*>.collectWhileSubscribed(
    flowToCollect: Flow<T>,
    debounceMillis: Long,
    collector: FlowCollector<T>
) {
    subscriptionCount
        .map { it > 0 }
        .debounce { hasActiveSubscribers -> if (hasActiveSubscribers) 0L else debounceMillis }
        .distinctUntilChanged()
        .flatMapLatest { hasActiveSubscribers -> if (hasActiveSubscribers) flowToCollect else emptyFlow() }
        .collect(collector)
}
