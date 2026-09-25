package ru.library.kv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KvTemplate {

  private final KeyValueStore store;
  private final ObjectMapper mapper;

  public KvTemplate(KeyValueStore store, ObjectMapper mapper) {
    this.store = store;
    this.mapper = mapper;
  }

  public KeyValueStore store() {
    return store;
  }

  public String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new KvException("serialize failed", e);
    }
  }

  public <T> T fromJson(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new KvException("deserialize failed", e);
    }
  }

  public <T> void put(String key, T value) {
    store.put(key, toJson(value));
  }

  public <T> Optional<Versioned<T>> get(String key, Class<T> type) {
    return store.get(key).map(e -> wrap(e, type));
  }

  public <T> List<Versioned<T>> getPrefix(String prefix, Class<T> type) {
    return store.getPrefix(prefix).stream().map(e -> wrap(e, type)).toList();
  }

  public List<String> keysWithPrefix(String prefix) {
    return store.getPrefix(prefix).stream().map(KvEntry::key).toList();
  }

  public long delete(String key) {
    return store.delete(key);
  }

  private <T> Versioned<T> wrap(KvEntry e, Class<T> type) {
    return new Versioned<>(fromJson(e.value(), type), e.modRevision(), e.version(), e.leaseId());
  }
}
