package dev.mardroemmar.springcaffeine.model

import dev.mardroemmar.springcaffeine.ext.isNonNegative
import dev.mardroemmar.springcaffeine.ext.min
import org.apiguardian.api.API
import java.time.Duration
import kotlin.random.Random
import kotlin.random.nextULong

@API(status = API.Status.INTERNAL)
internal val DEFAULT_CACHE_NAME = "%%default_cache%%${Random.Default.nextULong()}"

@API(status = API.Status.INTERNAL)
internal val DEFAULT_CACHE_DEFINITION: CacheDefinition = CacheDefinition().also {
    it.name = DEFAULT_CACHE_NAME
    it.baseDefinition = null
}

@API(status = API.Status.INTERNAL)
internal data class ImmutableCacheDefinition(
    val name: String,
    val initialCapacity: Int?,
    val maximumSize: Long?,
    val maximumWeight: Long?,
    val expireAfterAccess: Duration?,
    val expireAfterWrite: Duration?,
    val weakKeys: Boolean?,
    val weakValues: Boolean?,
    val softValues: Boolean?,
    val weigher: String?,
    val statistics: Boolean?,
) {
    constructor(other: CacheDefinition) : this(
        name = other.name,
        initialCapacity = other.initialCapacity,
        maximumSize = other.maximumSize,
        maximumWeight = other.maximumWeight,
        expireAfterAccess = other.expireAfterAccess,
        expireAfterWrite = other.expireAfterWrite,
        weakKeys = other.weakKeys,
        weakValues = other.weakValues,
        softValues = other.softValues,
        weigher = other.weigher,
        statistics = other.statistics,
    )
}

@API(status = API.Status.INTERNAL)
class CacheDefinition {
    /**
     * The name of the cache. It must be unique amongst all caches in the cache manager.
     */
    lateinit var name: String

    /**
     * The name of the cache to base this cache upon. If this is `null`, no base is used. If the base does not exist, the default cache
     * is used instead.
     */
    var baseDefinition: String? = DEFAULT_CACHE_NAME

    /**
     * The initial capacity of the cache.
     */
    var initialCapacity: Int? = null
        set(value) {
            field = value?.min(-1)

            val max = maximumSize ?: return
            if (max > 0 && max < (value ?: return)) {
                maximumSize = value.toLong()
            }
        }

    /**
     * The maximum size to use for the cache.
     */
    var maximumSize: Long? = null
        set(value) {
            field = value?.min(-1)

            val init = initialCapacity ?: return
            if (init > 0 && init > (value ?: return)) {
                initialCapacity = value.min(Int.MAX_VALUE.toLong()).toInt()
            }
        }

    /**
     * The maximum weight the entire cache can have. This requires `weigher` to be set.
     */
    var maximumWeight: Long? = null
        set(value) {
            field = if (value != null && value < 0) null else value
        }

    /**
     * The duration since last time accessing an entry, after which a cache entry is considered expired.
     */
    var expireAfterAccess: Duration? = null
        set(value) {
            field = value?.min(Duration.ZERO)
        }

    /**
     * The duration since last time writing an entry, after which a cache entry is considered expired.
     */
    var expireAfterWrite: Duration? = null
        set(value) {
            field = value?.min(Duration.ZERO)
        }

    /**
     * Whether to use [java.lang.ref.WeakReference]s for keys and values.
     *
     * Note that this will make the key use referential equality instead of calling [Object.equals].
     */
    var weakKeys: Boolean? = null

    /**
     * Whether to use [java.lang.ref.WeakReference]s for values.
     */
    var weakValues: Boolean? = null

    /**
     * Whether to use [java.lang.ref.SoftReference]s for values.
     */
    var softValues: Boolean? = null

    /**
     * The name of the [Weigher][com.github.benmanes.caffeine.cache.Weigher] to use. This must be a
     * [Bean][org.springframework.context.annotation.Bean] in the current application context.
     */
    var weigher: String? = null

    /**
     * Whether to enable statistics collection. There are no stable interfaces to this.
     */
    var statistics: Boolean? = null

    val isNameSet: Boolean
        get() = ::name.isInitialized

    fun copyFrom(other: CacheDefinition) {
        if (initialCapacity == null) {
            initialCapacity = other.initialCapacity
        }
        if (maximumSize == null) {
            maximumSize = other.maximumSize
        }
        if (maximumWeight == null) {
            maximumWeight = other.maximumWeight
        }
        if (expireAfterAccess == null) {
            expireAfterAccess = other.expireAfterAccess
        }
        if (expireAfterWrite == null) {
            expireAfterWrite = other.expireAfterWrite
        }
        if (weakKeys == null) {
            weakKeys = other.weakKeys
        }
        if (weakValues == null) {
            weakValues = other.weakValues
        }
        if (softValues == null) {
            softValues = other.softValues
        }
        if (weigher == null) {
            weigher = other.weigher
        }
        if (statistics == null) {
            statistics = other.statistics
        }
    }

    fun validate() {
        check(isNameSet) { "cache name must be set" }
        check(!(maximumWeight.isNonNegative && maximumSize.isNonNegative)) { "caches cannot have both a maximum size and maximum weight >= 0" }
        check(!(softValues == true && weakValues == true)) { "caches cannot have both soft and weak reference values at the same time" }
        check(!(maximumWeight.isNonNegative && weigher == null)) { "caches cannot have a maximum weight without a weigher" }
    }

    override fun toString(): String {
        return "CacheDefinition(" +
            "name='$name'," +
            " baseDefinition=$baseDefinition," +
            " initialCapacity=$initialCapacity," +
            " maximumSize=$maximumSize," +
            " maximumWeight=$maximumWeight," +
            " expireAfterAccess=$expireAfterAccess," +
            " expireAfterWrite=$expireAfterWrite," +
            " weakKeys=$weakKeys," +
            " weakValues=$weakValues," +
            " softValues=$softValues," +
            " weigher=$weigher" +
            " statistics=$statistics" +
            ")"
    }
}
