package com.contrastsecurity.plugin.service;

import com.contrastsecurity.plugin.configuration.ContrastCacheConfiguration;
import com.contrastsecurity.plugin.configuration.SubMenuCacheConfiguration;
import java.util.Objects;
import org.ehcache.Cache;

public class SubMenuCacheService {
  private Cache<String, Object> cache;
  private SubMenuCacheConfiguration cacheConfiguration;

  public SubMenuCacheService() {
    cacheConfiguration = SubMenuCacheConfiguration.getInstance();
    this.cache = ContrastCacheConfiguration.getCache();
  }

  public Object get(String key) {
    return cache.get(key);
  }

  public void addAll(String key, Object data) {
    // remove the previous old content
    if (Objects.isNull(cache)) {
      this.cache = SubMenuCacheConfiguration.getCache();
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

  public void close() {
    SubMenuCacheConfiguration.close();
  }
}
