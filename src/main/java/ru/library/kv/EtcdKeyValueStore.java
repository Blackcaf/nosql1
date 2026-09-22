package ru.library.kv;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name="library.kv.mode",havingValue="etcd")
public class EtcdKeyValueStore implements KeyValueStore {
    private final KV kv; private final io.etcd.jetcd.Lease lease; private final Watch watch;
    public EtcdKeyValueStore(Client client){kv=client.getKVClient();lease=client.getLeaseClient();watch=client.getWatchClient();}
    private static ByteSequence bs(String s){return ByteSequence.from(s,StandardCharsets.UTF_8);}
    private static String str(ByteSequence b){return b.toString(StandardCharsets.UTF_8);}
    private <T>T waitFor(java.util.concurrent.CompletableFuture<T> f){
        try{return f.get();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new KvException("etcd interrupted",e);}
        catch(ExecutionException e){throw new KvException("etcd failed",e.getCause());}
    }
    private KvEntry entry(io.etcd.jetcd.KeyValue e){return new KvEntry(str(e.getKey()),str(e.getValue()),e.getCreateRevision(),e.getModRevision(),e.getVersion(),e.getLease());}
    public Optional<KvEntry> get(String key){GetResponse r=waitFor(kv.get(bs(key)));return r.getKvs().isEmpty()?Optional.empty():Optional.of(entry(r.getKvs().get(0)));}
    public List<KvEntry> getPrefix(String prefix){return waitFor(kv.get(bs(prefix),GetOption.newBuilder().withPrefix(bs(prefix)).build())).getKvs().stream().map(this::entry).toList();}
    public long put(String key,String value){return put(key,value,0);}
    public long put(String key,String value,long leaseId){var b=PutOption.newBuilder();if(leaseId!=0)b.withLeaseId(leaseId);return waitFor(kv.put(bs(key),bs(value),b.build())).getHeader().getRevision();}
    public long delete(String key){return waitFor(kv.delete(bs(key))).getHeader().getRevision();}
    public long deletePrefix(String prefix){return waitFor(kv.delete(bs(prefix),DeleteOption.newBuilder().withPrefix(bs(prefix)).build())).getHeader().getRevision();}
    public TxnResult txn(List<Compare> cs,List<KvOp> thenOps,List<KvOp> elseOps){
        Cmp[] c=cs.stream().map(this::cmp).toArray(Cmp[]::new);Op[] t=thenOps.stream().map(this::op).toArray(Op[]::new);Op[] e=elseOps.stream().map(this::op).toArray(Op[]::new);
        TxnResponse r=waitFor(kv.txn().If(c).Then(t).Else(e).commit());return new TxnResult(r.isSucceeded(),r.getHeader().getRevision());
    }
    private Cmp cmp(Compare c){CmpTarget t=switch(c.target()){case VERSION->CmpTarget.version(c.number());case MOD_REVISION->CmpTarget.modRevision(c.number());case VALUE->CmpTarget.value(bs(c.text()));};Cmp.Op o=switch(c.op()){case EQUAL->Cmp.Op.EQUAL;case NOT_EQUAL->Cmp.Op.NOT_EQUAL;case GREATER->Cmp.Op.GREATER;case LESS->Cmp.Op.LESS;};return new Cmp(bs(c.key()),o,t);}
    private Op op(KvOp x){if(x instanceof KvOp.Put p){var b=PutOption.newBuilder();if(p.leaseId()!=0)b.withLeaseId(p.leaseId());return Op.put(bs(p.key()),bs(p.value()),b.build());}KvOp.Delete d=(KvOp.Delete)x;return Op.delete(bs(d.key()),DeleteOption.newBuilder().withPrefix(d.prefix()?bs(d.key()):null).build());}
    public long leaseGrant(long ttl){return waitFor(lease.grant(ttl)).getID();}
    public void leaseRevoke(long id){waitFor(lease.revoke(id));}
    public long leaseKeepAlive(long id){return waitFor(lease.keepAliveOnce(id)).getTTL();}
    public long leaseTimeToLive(long id){try{return waitFor(lease.timeToLive(id)).getTTl();}catch(Exception e){return -1;}}
    public AutoCloseable watch(String prefix,Consumer<ru.library.kv.WatchEvent> listener){
        return watch.watch(bs(prefix),WatchOption.newBuilder().withPrefix(bs(prefix)).build(),Watch.listener(r->{for(WatchEvent e:r.getEvents()){var k=e.getKeyValue();listener.accept(new ru.library.kv.WatchEvent(e.getEventType()==WatchEvent.EventType.PUT?ru.library.kv.WatchEvent.Type.PUT:ru.library.kv.WatchEvent.Type.DELETE,entry(k),null));}}));
    }
    public void snapshotSave(String path){throw new KvException("Use etcdctl snapshot save for real etcd");}
    public void snapshotRestore(String path){throw new KvException("Use etcdctl snapshot restore for real etcd");}
    public long currentRevision(){return waitFor(kv.get(bs("/library/__revision_probe__"))).getHeader().getRevision();}
}