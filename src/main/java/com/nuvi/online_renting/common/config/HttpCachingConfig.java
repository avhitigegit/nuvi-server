package com.nuvi.online_renting.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Enables HTTP caching for public, read-only item endpoints.
 *
 * Two mechanisms work together:
 *
 * 1. Cache-Control headers (set per-endpoint in ItemController):
 *    Tell browsers and CDNs how long to cache the response.
 *    - Item detail:  max-age=60  (1 minute)
 *    - Item list:    max-age=30  (30 seconds — search results change more often)
 *    - Image redirect: max-age=300 (5 minutes — safe, S3 pre-signed URL valid for 60 min)
 *
 * 2. ShallowEtagHeaderFilter (registered here):
 *    Computes an ETag (MD5 of the response body) and adds it to the response.
 *    On subsequent requests the browser sends If-None-Match: "etag-value".
 *    If the body has not changed, the filter returns 304 Not Modified with no body,
 *    saving bandwidth even when the max-age has expired.
 *
 * Only applied to /api/v1/items/** — authenticated endpoints are excluded
 * (Spring Security already sets Cache-Control: no-store on those).
 */
@Configuration
public class HttpCachingConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/items", "/api/v1/items/*");
        registration.setName("shallowEtagFilter");
        return registration;
    }
}
