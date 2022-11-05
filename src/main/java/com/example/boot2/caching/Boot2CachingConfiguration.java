package com.example.boot2.caching;

import com.hazelcast.com.google.common.cache.CacheBuilder;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The caching configuration for this application.
 * Controls which cache names are to be used and also the size and life of those maps.
 */
@Configuration
@EnableCaching
public class Boot2CachingConfiguration extends CachingConfigurerSupport {
  @Bean
  @Override
  public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {

      @Override
      protected Cache createConcurrentMapCache(final String name) {
        return new ConcurrentMapCache(name,
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).maximumSize(10000)
                .build().asMap(), false);
      }
    };

    cacheManager.setCacheNames(Arrays.asList("email", "status"));
    return cacheManager;
  }
}