package dev.mardroemmar.springcaffeine.ext

import org.apiguardian.api.API
import java.time.Duration

@API(status = API.Status.INTERNAL)
internal fun Duration.min(other: Duration): Duration = if (this > other) this else other
