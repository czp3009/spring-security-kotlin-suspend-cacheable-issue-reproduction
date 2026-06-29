package com.example.repro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
@EnableCaching
@EnableReactiveMethodSecurity
class Application {
    @Bean
    fun cacheManager(): CacheManager = ConcurrentMapCacheManager("demo")
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
class SecurityConfiguration {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { }
            .authorizeExchange {
                it.pathMatchers(HttpMethod.GET, "/demo").authenticated()
                it.anyExchange().denyAll()
            }
            .build()

    @Bean
    fun users(): MapReactiveUserDetailsService =
        MapReactiveUserDetailsService(
            User.withUsername("user")
                .password("{noop}password")
                .authorities("demo.read")
                .build()
        )
}

@RestController
class DemoController {
    private val invocationCounter = AtomicInteger()

    @GetMapping("/demo")
    @Cacheable("demo", key = "'fixed'")
    @PreAuthorize("hasAuthority('demo.read')")
    suspend fun demo(): DemoResponse =
        DemoResponse(
            message = "If caching worked, the second request would return this same payload.",
            invocation = invocationCounter.incrementAndGet(),
        )
}

data class DemoResponse(
    val message: String,
    val invocation: Int,
)
