package ru.library.kv;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class EtcdKeyValueStore implements KeyValueStore {

  private final KV kv;
  private final Lease lease;
  private final Watch watch;

  public EtcdKeyValueStore(Client client) {
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    this.watch = client.getWatchClient();
  }

  private static ByteSequence toByteSequence(String value) {
    return ByteSequence.from(value, StandardCharsets.UTF_8);
  }

  private static String toString(ByteSequence value) {
    return value.toString(StandardCharsets.UTF_8);
  }

  private <T> T waitFor(CompletableFuture<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new KvException("Операция etcd была прервана", e);
    } catch (ExecutionException e) {
      throw new KvException("Ошибка при выполнении операции etcd", e.getCause());
    }
  }

  private KvEntry toEntry(io.etcd.jetcd.KeyValue value) {
    return new KvEntry(
        toString(value.getKey()),
        toString(value.getValue()),
        value.getCreateRevision(),
        value.getModRevision(),
        value.getVersion(),
        value.getLease());
  }

  @Override
  public Optional<KvEntry> get(String key) {
    GetResponse response = waitFor(kv.get(toByteSequence(key)));

    if (response.getKvs().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(toEntry(response.getKvs().get(0)));
  }

  @Override
  public List<KvEntry> getPrefix(String prefix) {
    GetOption option = GetOption.newBuilder().withPrefix(toByteSequence(prefix)).build();

    GetResponse response = waitFor(kv.get(toByteSequence(prefix), option));

    return response.getKvs().stream().map(this::toEntry).toList();
  }

  @Override
  public long put(String key, String value) {
    return put(key, value, 0);
  }

  @Override
  public long put(String key, String value, long leaseId) {
    PutOption.Builder builder = PutOption.newBuilder();

    if (leaseId != 0) {
      builder.withLeaseId(leaseId);
    }

    return waitFor(kv.put(toByteSequence(key), toByteSequence(value), builder.build()))
        .getHeader()
        .getRevision();
  }

  @Override
  public long delete(String key) {
    return waitFor(kv.delete(toByteSequence(key))).getHeader().getRevision();
  }

  @Override
  public TxnResult txn(List<Compare> conditions, List<KvOp> thenOps, List<KvOp> elseOps) {
    Cmp[] comparisons = conditions.stream().map(this::toCmp).toArray(Cmp[]::new);

    Op[] thenOperations = thenOps.stream().map(this::toOp).toArray(Op[]::new);

    Op[] elseOperations = elseOps.stream().map(this::toOp).toArray(Op[]::new);

    TxnResponse response =
        waitFor(kv.txn().If(comparisons).Then(thenOperations).Else(elseOperations).commit());

    return new TxnResult(response.isSucceeded(), response.getHeader().getRevision());
  }

  private Cmp toCmp(Compare compare) {
    CmpTarget target =
        switch (compare.target()) {
          case VERSION -> CmpTarget.version(compare.number());

          case MOD_REVISION -> CmpTarget.modRevision(compare.number());
        };

    Cmp.Op operation =
        switch (compare.op()) {
          case EQUAL -> Cmp.Op.EQUAL;
          case NOT_EQUAL -> Cmp.Op.NOT_EQUAL;
          case GREATER -> Cmp.Op.GREATER;
          case LESS -> Cmp.Op.LESS;
        };

    return new Cmp(toByteSequence(compare.key()), operation, target);
  }

  private Op toOp(KvOp operation) {
    KvOp.Put put = (KvOp.Put) operation;
    PutOption.Builder builder = PutOption.newBuilder();

    if (put.leaseId() != 0) {
      builder.withLeaseId(put.leaseId());
    }

    return Op.put(toByteSequence(put.key()), toByteSequence(put.value()), builder.build());
  }

  @Override
  public long leaseGrant(long ttl) {
    return waitFor(lease.grant(ttl)).getID();
  }

  @Override
  public void leaseRevoke(long id) {
    waitFor(lease.revoke(id));
  }

  @Override
  public long leaseKeepAlive(long id) {
    return waitFor(lease.keepAliveOnce(id)).getTTL();
  }

  @Override
  public long leaseTimeToLive(long id) {
    try {
      return waitFor(lease.timeToLive(id, LeaseOption.DEFAULT)).getTTL();
    } catch (Exception e) {
      return -1;
    }
  }

  @Override
  public AutoCloseable watch(String prefix, Consumer<ru.library.kv.WatchEvent> listener) {
    WatchOption option = WatchOption.newBuilder().withPrefix(toByteSequence(prefix)).build();

    return watch.watch(
        toByteSequence(prefix),
        option,
        response -> {
          for (WatchEvent event : response.getEvents()) {
            io.etcd.jetcd.KeyValue keyValue = event.getKeyValue();

            ru.library.kv.WatchEvent.Type type =
                event.getEventType() == WatchEvent.EventType.PUT
                    ? ru.library.kv.WatchEvent.Type.PUT
                    : ru.library.kv.WatchEvent.Type.DELETE;

            listener.accept(new ru.library.kv.WatchEvent(type, toEntry(keyValue), null));
          }
        });
  }

  @Override
  public long currentRevision() {
    return waitFor(kv.get(toByteSequence("/library/__revision_probe__"))).getHeader().getRevision();
  }
}
