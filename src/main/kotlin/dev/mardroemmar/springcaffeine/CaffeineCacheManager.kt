package dev.mardroemmar.springcaffeine

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Weigher
import dev.mardroemmar.springcaffeine.ext.min
import dev.mardroemmar.springcaffeine.model.ImmutableCacheDefinition
import dev.mardroemmar.springcaffeine.model.SpringCacheConfiguration
import mu.KotlinLogging
import org.apiguardian.api.API
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val klogger = KotlinLogging.logger { }

@API(status = API.Status.INTERNAL)
@Component
@ConditionalOnProperty(
    "cache.caffeine.enabled",
    havingValue = "true",
    matchIfMissing = true
)
class CaffeineCacheManager(
    private val springCacheConfiguration: SpringCacheConfiguration,
    private val beanFactory: BeanFactory,
) : CacheManager {
    // Only internal for testing; otherwise private.
    internal val caches: ConcurrentMap<String, SpringCacheCaffeineWrapper> = ConcurrentHashMap()
    private val keysImmutableView: Set<String> = Collections.unmodifiableSet(caches.keys)
    private val valuesImmutableView: Collection<SpringCacheCaffeineWrapper> = Collections.unmodifiableCollection(caches.values)

    init {
        springCacheConfiguration.calculateEffectiveCacheDefinitions()
        springCacheConfiguration.cachesByName.forEach { (name, def) ->
            try {
                caches[name] = createCache(ImmutableCacheDefinition(def))
                klogger.debug { "Created and stored cache '$name'" }
            } catch (ex: Exception) {
                throw IllegalStateException("Could not create cache '$name'", ex)
            }
        }
        if (springCacheConfiguration.defaultCacheName !in caches) {
            try {
                caches[springCacheConfiguration.defaultCacheName] =
                    createCache(ImmutableCacheDefinition(springCacheConfiguration.defaultCacheDefinition))
                klogger.debug { "Created and stored default cache '${springCacheConfiguration.defaultCacheName}'" }
            } catch (ex: Exception) {
                throw IllegalStateException("Could not create default cache '${springCacheConfiguration.defaultCacheName}'", ex)
            }
        }
    }

    override fun getCache(name: String): SpringCacheCaffeineWrapper? {
        val existing = caches[name]
        if (existing != null) {
            klogger.trace { "Cache '$name' already existed; returning it" }
            return existing
        }

        if (!springCacheConfiguration.canCreateDefaultCaches) {
            // We can't create new caches with the default spec.
            klogger.trace { "Cannot create new caches, and couldn't find one for '$name'; returning null" }
            return null
        }

        return caches.computeIfAbsent(name) {
            klogger.trace { "Creating new cache for '$name' based on default" }
            createCache(
                ImmutableCacheDefinition(
                    springCacheConfiguration.findCache(name) ?: springCacheConfiguration.defaultCacheDefinition
                ),
                name = name,
            )
        }
    }

    override fun getCacheNames(): Set<String> = keysImmutableView

    fun getCaches(): Collection<SpringCacheCaffeineWrapper> = valuesImmutableView

    @Throws(ClassNotFoundException::class)
    private fun createCache(def: ImmutableCacheDefinition, name: String = def.name): SpringCacheCaffeineWrapper {
        klogger.trace { "Creating cache '$name' from: $def" }
        val builder = Caffeine.newBuilder()
        if (def.initialCapacity != null) {
            builder.initialCapacity(def.initialCapacity.min(0))
        }
        if (def.maximumSize != null) {
            builder.maximumSize(def.maximumSize.min(0))
        }
        if (def.maximumWeight != null) {
            builder.maximumWeight(def.maximumWeight.min(0))
        }
        if (def.expireAfterWrite != null) {
            builder.expireAfterWrite(def.expireAfterWrite)
        }
        if (def.expireAfterAccess != null) {
            builder.expireAfterAccess(def.expireAfterAccess)
        }
        if (def.weakKeys == true) {
            builder.weakKeys()
        }
        if (def.weakValues == true) {
            builder.weakValues()
        }
        if (def.softValues == true) {
            builder.softValues()
        }
        if (def.weigher != null) {
            val weigherClass = Class.forName(def.weigher)
            if (!Weigher::class.java.isAssignableFrom(weigherClass)) {
                throw IllegalArgumentException("Weigher '${def.weigher}' class of cache '$name' must implement ${Weigher::class.java}")
            }
            @Suppress("UNCHECKED_CAST") // It is checked above.
            val weigher = beanFactory.getBean(weigherClass as Class<out Weigher<*, *>>)
            builder.weigher(weigher)
        }
        if (def.statistics == true) {
            builder.recordStats()
        }

        return SpringCacheCaffeineWrapper(name, builder.build())
    }
}
