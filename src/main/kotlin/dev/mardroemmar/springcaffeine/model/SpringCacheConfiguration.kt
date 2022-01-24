package dev.mardroemmar.springcaffeine.model

import mu.KotlinLogging
import org.apiguardian.api.API
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

private val klogger = KotlinLogging.logger { }

@API(status = API.Status.INTERNAL)
@ConfigurationProperties("cache.caffeine")
open class SpringCacheConfiguration {
    var caches: Set<CacheDefinition> = setOf(DEFAULT_CACHE_DEFINITION)
        set(value) {
            require(value.all { it.isNameSet }) { "all cache configurations must have names set" }
            value.groupingBy { it.name }
                .eachCount()
                .asSequence()
                .filter { it.value > 1 }
                .map { (name, count) -> "$name [x$count]" }
                .joinToString(separator = ", ")
                .ifBlank { null }
                ?.let { throw IllegalArgumentException("found duplicate cache configuration names: $it") }

            field = value
            cachesByName = value.associateBy { it.name }
        }
    var cachesByName: Map<String, CacheDefinition> = emptyMap()
        private set
    var canCreateDefaultCaches: Boolean = false
    var defaultCacheName: String = "default"

    internal fun findCache(name: String): CacheDefinition? {
        if (name != defaultCacheName) {
            return cachesByName[name]
        }

        return defaultCacheDefinition
    }

    internal val defaultCacheDefinition: CacheDefinition
        get() = cachesByName[defaultCacheName] ?: DEFAULT_CACHE_DEFINITION

    internal fun calculateEffectiveCacheDefinitions() {
        val defaultDef = defaultCacheDefinition

        val finishedParents = mutableSetOf<String?>()
        val definitionsByParent = mutableMapOf<String?, MutableList<CacheDefinition>>()
        // All caches whose parents are simply null are considered to be top-level caches and have their dependencies fulfilled already.
        finishedParents.add(null)
        // Also, the "implicit" (programmatic) default cache should also be done already.
        finishedParents.add(DEFAULT_CACHE_NAME)

        caches.forEach {
            definitionsByParent.computeIfAbsent(it.baseDefinition) { LinkedList() }
                .add(it)
        }

        // We do however still need to handle the "implicit" default cache:
        definitionsByParent.computeIfAbsent(null) { LinkedList() }.add(defaultDef)

        // We want to know whether we finished any new parents last round.
        // This is to now throw us into an infinite loop, so we can throw if we get stuck.
        var finishedLastRound = true

        var iterator = definitionsByParent.iterator()
        while (iterator.hasNext() || definitionsByParent.isNotEmpty()) {
            klogger.trace { "Current finishedParents: $finishedParents" }
            klogger.trace { "Current definitionsByParent: $definitionsByParent" }

            if (!iterator.hasNext()) {
                if (!finishedLastRound) {
                    // We went an entire iteration without any changes.
                    // We therefore know now that we'll just continue doing that, and we're stuck in a loop.
                    throw IllegalStateException("cannot resolve cache dependencies: infinite loop of parents detected")
                }
                iterator = definitionsByParent.iterator()
                finishedLastRound = false
            }

            val (parent, definitions) = iterator.next()
            if (parent !in finishedParents) {
                // We can't handle this parent yet; it does not have its base cache resolved yet.
                klogger.trace { "Skipping handling definitions of parent '$parent': $definitions" }
                continue
            }
            val base = cachesByName[parent] ?: run {
                klogger.warn { "No base definition '$parent' found. Defaulting to default cache definition (named '$defaultCacheName') instead." }
                defaultDef
            }

            definitions.forEach {
                it.copyFrom(base)
                it.validate()

                finishedParents.add(it.name)
            }

            iterator.remove() // We don't need to work on this anymore.
            finishedLastRound = true
        }
    }
}
