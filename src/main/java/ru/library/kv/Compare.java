package ru.library.kv;

public record Compare(String key, Target target, Op op, long number) {
  public enum Target {
    VERSION,
    MOD_REVISION
  }

  public enum Op {
    EQUAL,
    NOT_EQUAL,
    GREATER,
    LESS
  }

  public static Compare version(String key, Op op, long v) {
    return new Compare(key, Target.VERSION, op, v);
  }

  public static Compare modRevision(String key, Op op, long v) {
    return new Compare(key, Target.MOD_REVISION, op, v);
  }
}
