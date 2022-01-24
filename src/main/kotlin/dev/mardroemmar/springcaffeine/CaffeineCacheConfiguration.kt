package dev.mardroemmar.springcaffeine

import dev.mardroemmar.springcaffeine.model.SpringCacheConfiguration
import org.apiguardian.api.API
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SpringCacheConfiguration::class)
@ConditionalOnProperty(
    "cache.caffeine.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@API(status = API.Status.INTERNAL)
open class CaffeineCacheConfiguration
