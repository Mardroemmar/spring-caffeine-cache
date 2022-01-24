package dev.mardroemmar.springcaffeine

import com.google.common.truth.Truth.assertThat
import dev.mardroemmar.springcaffeine.model.SpringCacheConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.annotation.CachingConfigurationSelector
import java.time.Duration
import kotlin.random.Random
import kotlin.random.nextULong

class SpringCacheConfigurationTest {
    private val contextWithoutManager = ApplicationContextRunner()
        .withUserConfiguration(CachingConfigurationSelector::class.java) // Pretend adding @EnableCaching
        .withUserConfiguration(CaffeineCacheConfiguration::class.java)!!
    private val context = contextWithoutManager.withBean(CaffeineCacheManager::class.java)!!

    @Test
    fun standardContextLoads() {
        assertDoesNotThrow {
            context.run { }
        }
    }

    @Test
    fun defaultCacheExists() {
        context.run {
            assertThat(
                it.getBean<CaffeineCacheManager>()
                    .getCache("default")
            ).isNotNull()
        }
    }

    @Test
    fun newDefaultCacheCreated() {
        context
            .withPropertyValues("cache.caffeine.can-create-default-caches=true")
            .run {
                assertThat(
                    it.getBean<CaffeineCacheManager>()
                        .getCache("some-value")
                ).isNotNull()
            }
    }

    @Test
    fun creatingFromDefaultCacheReturnsNull() {
        context
            .withPropertyValues("cache.caffeine.can-create-default-caches=false")
            .run {
                assertThat(
                    it.getBean<CaffeineCacheManager>()
                        .getCache("some-value")
                ).isNull()
            }
    }

    @Test
    fun maximumSizeWorks() {
        val name = "cache-test${Random.Default.nextULong()}"
        context
            .withPropertyValues(
                "cache.caffeine.caches[0].name=$name",
                "cache.caffeine.caches[0].maximum-size=3",
            )
            .run {
                val cache = it.getBean<CaffeineCacheManager>().getCache(name)
                assertThat(cache).isNotNull()
                repeat(4) { cache!!.put(it, it) }
                cache!!.nativeCache.cleanUp()
                assertThat(cache.get(0)).isNull()
                repeat(3) {
                    assertThat(cache.get(it + 1)).isNotNull()
                    assertThat(cache.get(it + 1)!!.get()).isEqualTo(it + 1)
                }
            }
    }

    @Test
    fun propertySettingWorks() {
        val name = "cache-test${Random.Default.nextULong()}"
        val initialCapacity = Random.Default.nextInt(1, 100)
        val maxSize = initialCapacity + Random.Default.nextLong(1L, 100L)
        val maxWeight = Random.Default.nextLong(1L, Long.MAX_VALUE)
        val expireAfterAccess = Duration.ofSeconds(Random.Default.nextLong(1L, 10_000L))
        val expireAfterWrite = Duration.ofSeconds(Random.Default.nextLong(1L, 10_000L))
        val weigher = "Weigher${Random.Default.nextULong()}"
        contextWithoutManager
            .withPropertyValues(
                "cache.caffeine.caches[0].name=$name",
                "cache.caffeine.caches[0].initial-capacity=$initialCapacity",
                "cache.caffeine.caches[0].maximum-size=$maxSize",
                "cache.caffeine.caches[0].maximum-weight=$maxWeight",
                "cache.caffeine.caches[0].expire-after-access=$expireAfterAccess",
                "cache.caffeine.caches[0].expire-after-write=$expireAfterWrite",
                "cache.caffeine.caches[0].weak-keys=true",
                "cache.caffeine.caches[0].weak-values=true",
                "cache.caffeine.caches[0].soft-values=true",
                "cache.caffeine.caches[0].weigher=$weigher",
                "cache.caffeine.caches[0].statistics=true",
            )
            .run {
                val cache = it.getBean<SpringCacheConfiguration>().findCache(name)
                assertThat(cache).isNotNull()
                cache!!
                assertThat(cache.isNameSet).isTrue()
                assertThat(cache.initialCapacity).isEqualTo(initialCapacity)
                assertThat(cache.maximumSize).isEqualTo(maxSize)
                assertThat(cache.maximumWeight).isEqualTo(maxWeight)
                assertThat(cache.expireAfterAccess).isEqualTo(expireAfterAccess)
                assertThat(cache.expireAfterWrite).isEqualTo(expireAfterWrite)
                assertThat(cache.weakKeys).isTrue()
                assertThat(cache.weakValues).isTrue()
                assertThat(cache.softValues).isTrue()
                assertThat(cache.weigher).isEqualTo(weigher)
                assertThat(cache.statistics).isTrue()
            }
    }
}
