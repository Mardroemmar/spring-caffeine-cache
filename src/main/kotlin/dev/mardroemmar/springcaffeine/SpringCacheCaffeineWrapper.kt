package dev.mardroemmar.springcaffeine

import mu.KotlinLogging
import org.apiguardian.api.API
import org.springframework.cache.Cache.ValueWrapper
import org.springframework.cache.support.SimpleValueWrapper
import java.util.concurrent.Callable
import com.github.benmanes.caffeine.cache.Cache as CaffeineCache
import org.springframework.cache.Cache as SpringCache

private val klogger = KotlinLogging.logger { }

@API(status = API.Status.INTERNAL)
class SpringCacheCaffeineWrapper internal constructor(
    private val name: String,
    private val cache: CaffeineCache<Any, Any>,
) : SpringCache {
    override fun getName(): String = name
    override fun getNativeCache(): CaffeineCache<*, *> = cache

    override fun get(key: Any?): ValueWrapper? {
        val value = cache.getIfPresent(key.maskNull)
        if (value == null) {
            klogger.trace { "Cache '$name' miss for key '$key'" }
        } else {
            klogger.trace { "Cache '$name' hit for key '$key': $value" }
        }
        return value?.let { SimpleValueWrapper(it.unmaskNull) }
    }

    override fun <T : Any> get(key: Any?, type: Class<T>?): T? {
        val value = cache.getIfPresent(key.maskNull)
        if (value == null) {
            klogger.trace { "Cache '$name' miss for key '$key'" }
            return null
        }
        klogger.trace { "Cache '$name' hit for key '$key': $value" }
        if (value.unmaskNull == null) {
            return null
        }
        if (type == null) {
            @Suppress("UNCHECKED_CAST") // That's what the caller wanted.
            return value as T
        }
        check(type.isInstance(value)) { "Cached value for key [$key] is not of required type [$type]: $value" }

        return type.cast(value)
    }

    override fun <T : Any> get(key: Any?, valueLoader: Callable<T?>): T? {
        var miss = false
        val value = cache.get(key.maskNull) {
            miss = true
            valueLoader.call().maskNull
        }
        if (miss) {
            klogger.trace { "Cache '$name' miss for key '$key'; loaded from value loader" }
        } else {
            klogger.trace { "Cache '$name' hit for key '$key': $value" }
        }
        @Suppress("UNCHECKED_CAST") // That's what the caller wanted...
        return value.unmaskNull as T?
    }

    override fun put(key: Any?, value: Any?) {
        cache.put(key.maskNull, value.maskNull)
        klogger.trace { "Cache '$name' updated with key '$key': $value" }
    }

    override fun putIfAbsent(key: Any?, value: Any?): ValueWrapper? {
        val existing = get(key)
        if (existing != null) {
            klogger.trace { "Cache '$name' hit for key '$key': $existing" }
            return existing
        }

        cache.put(key.maskNull, value.maskNull)
        klogger.trace { "Cache '$name' miss for key '$key'; updated with: $value" }
        return null
    }

    override fun evict(key: Any?) {
        cache.invalidate(key.maskNull)
        klogger.trace { "Cache '$name' invalidated key '$key'" }
    }

    override fun evictIfPresent(key: Any?): Boolean {
        if (cache.getIfPresent(key.maskNull) != null) {
            cache.invalidate(key.maskNull)
            klogger.trace { "Cache '$name' had key '$key'; it is now invalidated" }
            return true
        }
        klogger.trace { "Cache '$name' did not have key '$key'; no invalidation performed." }
        return false
    }

    override fun clear() {
        cache.invalidateAll()
        klogger.trace { "Cache '$name' invalidated all keys" }
    }

    override fun invalidate(): Boolean {
        val wasEmpty = cache.estimatedSize() == 0L
        cache.invalidateAll()
        klogger.trace { "Cache '$name' invalidated all keys; wasEmpty=$wasEmpty" }
        return wasEmpty
    }
}

private object NullMask

private val <T : Any> T?.maskNull: Any
    get() = this ?: NullMask

private val Any.unmaskNull: Any?
    get() = if (this === NullMask) null else this
