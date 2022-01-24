package dev.mardroemmar.springcaffeine

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.truth.Truth.assertThat
import dev.mardroemmar.springcaffeine.model.SpringCacheConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.stereotype.Component
import kotlin.random.Random

private const val CACHE_NAME: String = "CacheName"

@SpringBootTest(classes = [CaffeineCacheConfiguration::class, CaffeineCacheManager::class, CacheableTest.CachingComponent::class])
@EnableCaching
class CacheableTest {
    @Autowired
    lateinit var cachingComponent: CachingComponent

    @Autowired
    lateinit var caffeineCacheManager: CaffeineCacheManager

    @Autowired
    lateinit var springCacheConfiguration: SpringCacheConfiguration

    @BeforeEach
    fun setUp() {
        caffeineCacheManager.getCaches().forEach { it.invalidate() }
    }

    @Test
    fun cachingWorks() {
        caffeineCacheManager.caches[CACHE_NAME] = SpringCacheCaffeineWrapper(CACHE_NAME, Caffeine.newBuilder().build())

        val a = Random.Default.nextInt(10, 100)
        val b = Random.Default.nextInt(10, 100)

        val first = cachingComponent.add(a, b)
        val second = cachingComponent.add(a, b)
        assertThat(first).isSameInstanceAs(second)

        val third = cachingComponent.add(a + 1, b)
        val fourth = cachingComponent.add(a + 1, b)
        assertThat(third).isNotEqualTo(first)
        assertThat(third).isSameInstanceAs(fourth)

        val fifth = cachingComponent.add(a, b + 1)
        val sixth = cachingComponent.add(a, b + 1)
        assertThat(fifth).isNotEqualTo(first)
        assertThat(fifth).isNotEqualTo(third)
        assertThat(fifth).isSameInstanceAs(sixth)

        // The default cache is unbounded and eternal.
        val seventh = cachingComponent.add(a, b)
        assertThat(seventh).isSameInstanceAs(first)
    }

    @Test
    fun unknownCacheThrows() {
        caffeineCacheManager.caches.remove(CACHE_NAME)
        springCacheConfiguration.canCreateDefaultCaches = false

        val a = Random.Default.nextInt(10, 100)
        val b = Random.Default.nextInt(10, 100)
        assertThrows<IllegalArgumentException> {
            cachingComponent.add(a, b)
        }
    }

    @Test
    fun defaultCacheCreated() {
        caffeineCacheManager.caches.remove(CACHE_NAME)
        springCacheConfiguration.canCreateDefaultCaches = true

        val a = Random.Default.nextInt(10, 100)
        val b = Random.Default.nextInt(10, 100)
        assertDoesNotThrow {
            cachingComponent.add(a, b)
        }
    }

    @Component
    open class CachingComponent {
        @Cacheable(CACHE_NAME)
        open fun add(a: Int, b: Int): Addition = Addition(a, b, a + b)
    }

    data class Addition(val a: Int, val b: Int, val result: Int)
}
