package ru.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library")
public record AppProperties(Kv kv, Cache cache, boolean demoData, Etcd etcd) {
  public record Kv(
      String keyPrefix,
      long draftTtlSeconds) {}

  public record Cache(long userSettingsTtlSeconds, long userSettingsMaxSize) {}

  public record Etcd(String endpoint) {}
}