/*******************************************************************************
 * Copyright © 2025 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.configuration;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class SubMenuCacheConfiguration {
  @Getter @Setter public static CacheManager cacheManager;
  private static SubMenuCacheConfiguration cacheConfiguration;

  private SubMenuCacheConfiguration() {}

  public static SubMenuCacheConfiguration getInstance() {
    if (cacheConfiguration == null) {
      cacheConfiguration = new SubMenuCacheConfiguration();
    }
    return cacheConfiguration;
  }

  public static Cache<String, Object> getCache() {
    if (Objects.isNull(cacheManager)) {
      initializeCache();
    }
    return cacheManager.getCache("overview", String.class, Object.class);
  }

  public static void close() {
    cacheManager.close();
  }

  private static void initializeCache() {
    try {
      cacheManager =
          CacheManagerBuilder.newCacheManagerBuilder()
              .withCache(
                  "overview",
                  CacheConfigurationBuilder.newCacheConfigurationBuilder(
                      String.class, Object.class, ResourcePoolsBuilder.heap(10000)))
              .build(true); // Initialize
    } catch (Exception e) {
      throw new RuntimeException(
          "Cache 'overview' creation in EhcacheManager failed: " + e.getMessage(), e);
    }
  }
}
