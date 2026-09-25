package ru.library.kv;

public sealed interface KvOp permits KvOp.Put {
  record Put(String key, String value, long leaseId) implements KvOp {}

  static Put put(String key, String value) {
    return new Put(key, value, 0);
  }

  static Put put(String key, String value, long leaseId) {
    return new Put(key, value, leaseId);
  }
}
