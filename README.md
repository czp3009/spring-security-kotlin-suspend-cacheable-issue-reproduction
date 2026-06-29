# Spring Security Kotlin Suspend Cacheable Reproduction

Minimal reproduction for a `ClassCastException` when these three pieces are combined:

- Spring WebFlux with Kotlin `suspend` controller methods
- Spring Security reactive method security via `@PreAuthorize`
- Spring Framework cache annotations via `@Cacheable`

The application uses Spring Boot `4.1.0`, Kotlin `2.4.0`, HTTP Basic authentication, and an in-memory `ConcurrentMapCacheManager`.

## Run

```bash
./gradlew bootRun
```

## Reproduce

In another shell, call the same endpoint twice:

```bash
curl -i -u user:password http://localhost:8080/demo
curl -i -u user:password http://localhost:8080/demo
```

Expected behavior:

- The first request returns `200 OK`.
- The second request should also return `200 OK`, reusing the cached value. If caching is working, the JSON field `invocation` should remain `1`.

Actual behavior with the affected Spring combination:

- The second request fails with a `500 Internal Server Error`.
- The application log shows a `ClassCastException`, because the cached `DemoResponse` object is returned where the reactive method-security path expects a `Mono`.

Observed stack trace excerpt:

```text
java.lang.ClassCastException: class com.example.repro.DemoResponse cannot be cast to class reactor.core.publisher.Mono
    at org.springframework.security.authorization.method.AuthorizationManagerBeforeReactiveMethodInterceptor.lambda$invoke$5(AuthorizationManagerBeforeReactiveMethodInterceptor.java:138)
```

Cache logs show that the first request creates the cache entry and the second request hits it:

```text
No cache entry for key 'fixed' in cache(s) [demo]
Creating cache entry for key 'fixed' in cache(s) [demo]
Cache entry for key 'fixed' found in cache(s) [demo]
```

The relevant controller method is:

```kotlin
@GetMapping("/demo")
@Cacheable("demo", key = "'fixed'")
@PreAuthorize("hasAuthority('demo.read')")
suspend fun demo(): DemoResponse = DemoResponse(...)
```

Observed dependency versions from Spring Boot `4.1.0` dependency management:

- Spring Framework `7.0.8`
- Spring Security `7.1.0`
- Reactor `3.8.6`
