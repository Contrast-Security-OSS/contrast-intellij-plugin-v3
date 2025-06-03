/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.configuration;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

@Slf4j
public class ContrastCacheConfiguration {

  @Getter @Setter public static CacheManager cacheManager;
  private static ContrastCacheConfiguration cacheConfiguration;

  private ContrastCacheConfiguration() {}

  public static ContrastCacheConfiguration getInstance() {
    if (cacheConfiguration == null) {
      cacheConfiguration = new ContrastCacheConfiguration();
    }
    return cacheConfiguration;
  }

  public static Cache<String, Object> getCache() {
    if (Objects.isNull(cacheManager)) {
      initializeCache();
    }
    return cacheManager.getCache("myCache", String.class, Object.class);
  }

  public static void close() {
    cacheManager.close();
  }

  private static void initializeCache() {
    try {
      cacheManager =
          CacheManagerBuilder.newCacheManagerBuilder()
              .withCache(
                  "myCache",
                  CacheConfigurationBuilder.newCacheConfigurationBuilder(
                      String.class, Object.class, ResourcePoolsBuilder.heap(10000)))
              .build(true); // Initialize
    } catch (Exception e) {
      log.error("Cache 'myCache' creation in EhcacheManager failed: " + e.getMessage());
    }
  }
}
