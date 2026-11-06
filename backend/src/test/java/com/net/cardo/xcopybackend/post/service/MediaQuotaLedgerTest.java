package com.net.cardo.xcopybackend.post.service;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class MediaQuotaLedgerTest {
    @TempDir Path root;
    MockMultipartFile png() {return new MockMultipartFile("file","x.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0,1,2,3});}
    @Test void initializesFromLegacyFilesAndReconcilesOnRestart() throws Exception {
        UUID owner=UUID.randomUUID();Files.createDirectories(root.resolve(owner.toString()));
        Files.write(root.resolve(owner+"/legacy.png"),new byte[12]);
        var storage=new MediaStorageService(root.toString());ReflectionTestUtils.setField(storage,"ownerMaxFiles",1L);
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(png(),owner)).getStatusCode().value());
        Files.delete(root.resolve(owner+"/legacy.png"));
        var restarted=new MediaStorageService(root.toString());ReflectionTestUtils.setField(restarted,"ownerMaxFiles",1L);
        restarted.store(png(),owner);
        assertEquals("12 1",Files.readString(root.resolve(".quota-v1/total")).trim());
    }
    @Test void dirtyMarkerRepairsInterruptedCommitAndIgnoresStaging() throws Exception {
        var storage=new MediaStorageService(root.toString());UUID owner=UUID.randomUUID();storage.store(png(),owner);
        Files.write(root.resolve(owner+"/interrupted.png"),new byte[12]);
        Files.writeString(root.resolve(".quota-v1/dirty"),"");Files.writeString(root.resolve(".quota-v1/total"),"0 0");
        Files.write(root.resolve(".staging/abandoned.part"),new byte[50]);
        ReflectionTestUtils.setField(storage,"ownerMaxFiles",2L);
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(png(),owner)).getStatusCode().value());
        assertEquals("24 2",Files.readString(root.resolve(".quota-v1/total")).trim());
        assertFalse(Files.exists(root.resolve(".quota-v1/dirty")));
    }
    @Test void parallelInstancesCannotExceedTheLastAvailableSlot() throws Exception {
        var first=new MediaStorageService(root.toString());var second=new MediaStorageService(root.toString());
        ReflectionTestUtils.setField(first,"totalMaxFiles",1L);ReflectionTestUtils.setField(second,"totalMaxFiles",1L);
        var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var jobs=new ArrayList<Future<Integer>>();
            for(var storage:List.of(first,second))jobs.add(pool.submit(()->{
                assertTrue(gate.await(5,TimeUnit.SECONDS));
                try{storage.store(png(),UUID.randomUUID());return 201;}
                catch(ResponseStatusException error){return error.getStatusCode().value();}
            }));
            gate.countDown();var codes=new ArrayList<Integer>();
            for(var job:jobs)codes.add(job.get(10,TimeUnit.SECONDS));Collections.sort(codes);
            assertEquals(List.of(201,507),codes);assertEquals("12 1",Files.readString(root.resolve(".quota-v1/total")).trim());
        } finally {pool.shutdownNow();}
    }
    @Test void copyDoesNotHoldTheQuotaLock() throws Exception {
        var copying=new CountDownLatch(1);var release=new CountDownLatch(1);
        var slow=new MockMultipartFile("file","x.png","image/png",png().getBytes()) {
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                copying.countDown();try {assertTrue(release.await(5,TimeUnit.SECONDS));}
                catch(InterruptedException e){throw new RuntimeException(e);}return new java.io.ByteArrayInputStream(getBytes());
            }
        };
        var storage=new MediaStorageService(root.toString());var pool=Executors.newFixedThreadPool(2);
        try {
            var pending=pool.submit(()->storage.store(slow,UUID.randomUUID()));
            assertTrue(copying.await(5,TimeUnit.SECONDS));
            pool.submit(()->storage.store(png(),UUID.randomUUID())).get(3,TimeUnit.SECONDS);
            release.countDown();pending.get(5,TimeUnit.SECONDS);
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void doubleDeleteAndFailedValidationDoNotReleaseExtraQuota() throws Exception {
        var storage=new MediaStorageService(root.toString());UUID owner=UUID.randomUUID();
        var media=storage.store(png(),owner);storage.delete(media.url());storage.delete(media.url());
        assertThrows(ResponseStatusException.class,()->storage.store(new MockMultipartFile("file","bad.png","image/png",new byte[12]),owner));
        assertEquals("0 0",Files.readString(root.resolve(".quota-v1/total")).trim());
        try(var files=Files.list(root.resolve(".staging"))){assertEquals(0,files.count());}
    }
    @Test void ordinaryCommitDoesNotWalkUnrelatedDirectories() throws Exception {
        var storage=new MediaStorageService(root.toString());storage.store(png(),UUID.randomUUID());
        // An out-of-band file is deliberately not discovered until reconciliation.
        UUID other=UUID.randomUUID();Files.createDirectories(root.resolve(other.toString()));
        Files.write(root.resolve(other+"/external.png"),new byte[12]);
        storage.store(png(),UUID.randomUUID());
        assertEquals("24 2",Files.readString(root.resolve(".quota-v1/total")).trim());
        Files.writeString(root.resolve(".quota-v1/dirty"),"");storage.store(png(),UUID.randomUUID());
        assertEquals("48 4",Files.readString(root.resolve(".quota-v1/total")).trim());
    }
    @Test void fifthParallelCopyIsRejectedAndSlotsAreReleased() throws Exception {
        var entered=new CountDownLatch(4);var release=new CountDownLatch(1);
        var slow=new MockMultipartFile("file","x.png","image/png",png().getBytes()) {
            @Override public java.io.InputStream getInputStream() throws java.io.IOException {
                entered.countDown();try {assertTrue(release.await(5,TimeUnit.SECONDS));}
                catch(InterruptedException e){throw new RuntimeException(e);}return new java.io.ByteArrayInputStream(getBytes());
            }
        };
        var storage=new MediaStorageService(root.toString());var pool=Executors.newFixedThreadPool(4);
        var pending=new ArrayList<Future<MediaStorageService.StoredMedia>>();
        try {
            for(int i=0;i<4;i++)pending.add(pool.submit(()->storage.store(slow,UUID.randomUUID())));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertEquals(429,assertThrows(ResponseStatusException.class,()->storage.store(png(),UUID.randomUUID())).getStatusCode().value());
            release.countDown();for(var job:pending)job.get(10,TimeUnit.SECONDS);
            storage.store(png(),UUID.randomUUID());
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void missingOwnerCounterCannotResetAnExistingOwnersQuota() throws Exception {
        var storage=new MediaStorageService(root.toString());UUID owner=UUID.randomUUID();
        ReflectionTestUtils.setField(storage,"ownerMaxFiles",1L);storage.store(png(),owner);
        Files.delete(root.resolve(".quota-v1/owner-"+owner));
        assertEquals(507,assertThrows(ResponseStatusException.class,()->storage.store(png(),owner)).getStatusCode().value());
        assertEquals("12 1",Files.readString(root.resolve(".quota-v1/owner-"+owner)).trim());
    }
}
