package ru.library.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import ru.library.kv.KeyValueStore;
import ru.library.kv.KvEntry;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final KeyValueStore store;

  public AdminController(KeyValueStore store) {
    this.store = store;
  }

  @GetMapping("/keys")
  public List<KvEntry> keys(@RequestParam(defaultValue = "/") String prefix) {
    return store.getPrefix(prefix);
  }

  @GetMapping("/status")
  public Map<String, Object> status() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("mode", "etcd");
    m.put("revision", store.currentRevision());
    m.put("keys", store.getPrefix("/").size());
    return m;
  }
}