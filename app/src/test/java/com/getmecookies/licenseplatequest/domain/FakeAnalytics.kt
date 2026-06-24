package com.getmecookies.licenseplatequest.domain

/**
 * In-memory [Analytics] test double — records screen/event/user-property calls so tests can assert
 * that the right analytics fire (and that none fire when consent is off). No network, no SDK.
 */
class FakeAnalytics : Analytics {
    data class Record(val name: String, val params: Map<String, Any?>)

    val screens = mutableListOf<Record>()
    val events = mutableListOf<Record>()
    val userProperties = mutableMapOf<String, String?>()

    override fun screen(name: String, params: Map<String, Any?>) {
        screens += Record(name, params)
    }

    override fun event(name: String, params: Map<String, Any?>) {
        events += Record(name, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties[name] = value
    }

    /** Convenience: the names of all recorded events, in order. */
    fun eventNames(): List<String> = events.map { it.name }

    /** The params of the first event with [name], or null if none recorded. */
    fun paramsOf(name: String): Map<String, Any?>? = events.firstOrNull { it.name == name }?.params
}
