package com.net.cardo.xcopybackend.post.service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Filesystem-local ledger. All public operations run under JVM and OS locks. */
final class MediaQuotaLedger {
    private static final Map<Path,ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final Path root, ledger, dirty;
    private boolean initialized;
    MediaQuotaLedger(Path root) {
        this.root=root; ledger=root.resolve(".quota-v1"); dirty=ledger.resolve("dirty");
    }
    record Usage(long bytes,long files) {
        Usage add(long bytes,long files) { return new Usage(Math.addExact(this.bytes,bytes),Math.addExact(this.files,files)); }
    }
    interface Operation<T> { T run() throws IOException; }
    <T> T locked(Operation<T> operation) throws IOException {
        Files.createDirectories(root);
        ReentrantLock mutex=LOCKS.computeIfAbsent(root,key->new ReentrantLock());
        mutex.lock();
        try(var channel=FileChannel.open(root.resolve(".quota.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=channel.lock()) {
            if(!initialized || !Files.exists(ledger.resolve("total")) || Files.exists(dirty)) rebuild();
            initialized=true;
            return operation.run();
        } finally { mutex.unlock(); }
    }
    Usage total() throws IOException { return read(ledger.resolve("total")); }
    Usage owner(String owner) throws IOException {
        Path counter=ledger.resolve("owner-"+owner);
        if(!Files.exists(counter) && Files.isDirectory(root.resolve(owner),LinkOption.NOFOLLOW_LINKS))rebuild();
        return read(counter);
    }
    void begin() throws IOException {
        // A durable marker makes an interrupted file/counter update recoverable.
        Files.createDirectories(ledger);
        try(var channel=FileChannel.open(dirty,StandardOpenOption.CREATE,StandardOpenOption.WRITE)) { channel.force(true); }
    }
    void finish(String owner,Usage total,Usage account) throws IOException {
        write(ledger.resolve("owner-"+owner),account);
        write(ledger.resolve("total"),total);
        Files.delete(dirty);
    }
    private Usage read(Path path) throws IOException {
        if(!Files.exists(path)) return new Usage(0,0);
        try {
            String[] parts=Files.readString(path).trim().split(" ");
            if(parts.length!=2) throw new IllegalArgumentException();
            long bytes=Long.parseLong(parts[0]),files=Long.parseLong(parts[1]);
            if(bytes<0 || files<0)throw new IllegalArgumentException();
            return new Usage(bytes,files);
        } catch(IllegalArgumentException invalid) { throw new IOException("Invalid media quota ledger",invalid); }
    }
    private void write(Path target,Usage usage) throws IOException {
        Path temp=ledger.resolve("counter.tmp");
        try(var channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
            var buffer=java.nio.ByteBuffer.wrap((usage.bytes()+" "+usage.files()+"\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            while(buffer.hasRemaining())channel.write(buffer);
            channel.force(true);
        }
        Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private void rebuild() throws IOException {
        begin();
        var owners=new HashMap<String,Usage>();
        Usage total=new Usage(0,0);
        try(var paths=Files.walk(root)) {
            var iterator=paths.iterator();
            while(iterator.hasNext()) {
                Path path=iterator.next(),relative=root.relativize(path);
                boolean hidden=false;
                for(Path part:relative)if(part.toString().startsWith("."))hidden=true;
                if(hidden || !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))continue;
                long size=Files.size(path);total=total.add(size,1);
                if(relative.getNameCount()==2) {
                    String owner=relative.getName(0).toString();
                    try { UUID.fromString(owner); }
                    catch(IllegalArgumentException legacy) { continue; }
                    owners.put(owner,owners.getOrDefault(owner,new Usage(0,0)).add(size,1));
                }
            }
        }
        try(var files=Files.list(ledger)) {
            for(Path path:files.filter(p->p.getFileName().toString().startsWith("owner-")).toList())Files.delete(path);
        }
        for(var entry:owners.entrySet())write(ledger.resolve("owner-"+entry.getKey()),entry.getValue());
        write(ledger.resolve("total"),total);
        Files.delete(dirty);
    }
}
