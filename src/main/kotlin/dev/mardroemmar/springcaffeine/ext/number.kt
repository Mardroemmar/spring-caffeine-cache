package dev.mardroemmar.springcaffeine.ext

import org.apiguardian.api.API

@API(status = API.Status.INTERNAL)
internal fun Int.min(other: Int): Int = if (this > other) this else other

@API(status = API.Status.INTERNAL)
internal fun Long.min(other: Long): Long = if (this > other) this else other

internal val Long?.isNonNegative: Boolean
    @API(status = API.Status.INTERNAL)
    get() = this != null && this >= 0L
