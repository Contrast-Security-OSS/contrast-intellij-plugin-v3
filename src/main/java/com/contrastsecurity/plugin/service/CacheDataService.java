/*******************************************************************************
 * Copyright © 2024 Contrast Security, Inc.
 * See https://www.contrastsecurity.com/enduser-terms-0317a for more details.
 *******************************************************************************/

package com.contrastsecurity.plugin.service;

import com.contrastsecurity.plugin.configuration.ContrastCacheConfiguration;
import java.io.Serializable;
import java.util.Objects;
import org.ehcache.Cache;

public class CacheDataService implements Serializable {
  private Cache<String, Object> cache;
  private ContrastCacheConfiguration cacheConfiguration;

  public CacheDataService() {
    cacheConfiguration = ContrastCacheConfiguration.getInstance();
    this.cache = ContrastCacheConfiguration.getCache();
  }

  public Object get(String key) {
    return cache.get(key);
  }

  public void addAll(String key, Object data) {
    // remove the previous old content
    if (Objects.isNull(cache)) {
      this.cache = ContrastCacheConfiguration.getCache();
    }
    if (Objects.nonNull(cache) && cache.iterator().hasNext()) {
      cache.clear();
    }
    if (Objects.nonNull(cache)) {
      cache.put(key, data);
    }
  }

  public void add(String key, Object trace) {
    cache.put(key, trace);
  }

  public void delete(String key) {
    cache.remove(key);
  }

  public void clear() {
    if (cache.iterator().hasNext()) {
      cache.clear();
    }
  }

  public void clearCacheById(String key) {
    if (Objects.nonNull(cache) && cache.containsKey(key)) {
      cache.remove(key);
    }
  }

  public void close() {
    ContrastCacheConfiguration.close();
  }
}
