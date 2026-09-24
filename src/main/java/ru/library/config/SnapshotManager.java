package ru.library.config;

import jakarta.annotation.PreDestroy;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.library.kv.KeyValueStore;
import ru.library.kv.Keys;
import ru.library.kv.KvTemplate;
import ru.library.kv.Versioned;
import ru.library.model.OrderDraft;

@Component
public class SnapshotManager {
  private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);
  private final KeyValueStore store;
  private final KvTemplate kv;
  private final Keys keys;
  private final AppProperties props;

  public SnapshotManager(KeyValueStore store, KvTemplate kv, Keys keys, AppProperties props) {
    this.store = store;
    this.kv = kv;
    this.keys = keys;
    this.props = props;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(1)
  public void restoreOnStart() {
    if (!"memory".equals(props.kv().mode())) return;
    String path = props.kv().snapshotFile();
    if (props.kv().restoreOnStart() && new File(path).exists())
      log.info("restored memory snapshot, orphan drafts removed: {}", restore(path));
  }

  public int restore(String path) {
    if (!"memory".equals(props.kv().mode()))
      throw new IllegalStateException("For real etcd use etcdctl snapshot restore");
    store.snapshotRestore(path);
    return sweepOrphanDrafts();
  }

  public int sweepOrphanDrafts() {
    int n = 0;
    for (Versioned<OrderDraft> v : kv.getPrefix(keys.drafts(), OrderDraft.class)) {
      if (v.leaseId() == 0) {
        kv.delete(keys.draft(v.value().id()));
        kv.delete(keys.draftByEvent(v.value().eventId(), v.value().id()));
        n++;
      }
    }
    return n;
  }

  @PreDestroy
  public void saveOnShutdown() {
    if (!"memory".equals(props.kv().mode())) return;
    try {
      store.snapshotSave(props.kv().snapshotFile());
    } catch (Exception e) {
      log.warn("snapshot failed: {}", e.toString());
    }
  }
}