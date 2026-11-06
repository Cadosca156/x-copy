package com.net.cardo.xcopybackend.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class MediaStorageService {
    private static final long MAX_FILE = 25L * 1024 * 1024;
    private static final int MAX_STAGED_UPLOADS = 4;
    private final Semaphore stagingSlots = new Semaphore(MAX_STAGED_UPLOADS);
    private static final Map<String,String> EXTENSIONS = Map.of(
        "image/jpeg","jpg","image/png","png","image/gif","gif","image/webp","webp",
        "video/mp4","mp4","video/webm","webm","video/quicktime","mov");
    private final Path uploadDirectory;
    @Value("${app.media.owner-max-bytes:524288000}") private long ownerMaxBytes=524288000L;
    @Value("${app.media.total-max-bytes:5368709120}") private long totalMaxBytes=5368709120L;
    @Value("${app.media.owner-max-files:500}") private long ownerMaxFiles=500;
    @Value("${app.media.total-max-files:10000}") private long totalMaxFiles=10000;
    public MediaStorageService(@Value("${app.media.upload-dir:uploads/posts}") String directory) {
        uploadDirectory=Path.of(directory).toAbsolutePath().normalize();
        quotas=new MediaQuotaLedger(uploadDirectory);
    }
    private final MediaQuotaLedger quotas;
    public StoredMedia store(MultipartFile file, UUID owner) {
        if(file==null || owner==null || file.isEmpty() || file.getSize()>MAX_FILE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Файл має містити до 25 МБ");
        String type=file.getContentType(), extension=EXTENSIONS.get(type==null?"":type);
        if(extension==null) throw unsupported();
        Path directory=uploadDirectory.resolve(owner.toString());
        Path target=directory.resolve(UUID.randomUUID()+"."+extension);
        Path staged=null;
        if(!stagingSlots.tryAcquire())
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Забагато одночасних завантажень. Спробуйте трохи пізніше.");
        try {
            Files.createDirectories(uploadDirectory);
            Path staging=uploadDirectory.resolve(".staging");
            Files.createDirectories(staging);
            if(Files.isSymbolicLink(staging) || Files.isSymbolicLink(directory))throw new IOException("Symbolic media directory");
            // Reserve headroom for the other bounded in-process staging operations.
            checkFreeSpace(file.getSize()+(MAX_STAGED_UPLOADS-1)*MAX_FILE);
            staged=Files.createTempFile(staging,"upload-",".part");
            // Slow copying and validation are independent of the short quota commit lock.
            try(InputStream input=new BufferedInputStream(file.getInputStream());
                OutputStream output=Files.newOutputStream(staged)) {
                input.mark(32);
                byte[] header=input.readNBytes(16);input.reset();
                if(!matches(type,header))throw unsupported();
                byte[] buffer=new byte[8192];long copied=0;int read;
                while((read=input.read(buffer))!=-1) {
                    copied+=read;
                    if(copied>file.getSize() || copied>MAX_FILE)
                        throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Завеликий файл");
                    output.write(buffer,0,read);
                }
                if(copied!=file.getSize())throw new IOException("Invalid upload length");
            }
            Path ready=staged;
            quotas.locked(() -> {
                var account=quotas.owner(owner.toString());var total=quotas.total();
                long size=Files.size(ready);
                if(size>totalMaxBytes-total.bytes() || size>ownerMaxBytes-account.bytes() ||
                        total.files()>=totalMaxFiles || account.files()>=ownerMaxFiles)
                    throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE,"Ліміт медіафайлів вичерпано. Видаліть зайві файли.");
                checkFreeSpace(0);
                Files.createDirectories(directory);
                quotas.begin();
                try {
                    Files.move(ready,target,StandardCopyOption.ATOMIC_MOVE);
                    quotas.finish(owner.toString(),total.add(size,1),account.add(size,1));
                } catch(IOException failure) {
                    try { Files.deleteIfExists(target); } catch(IOException cleanup) { failure.addSuppressed(cleanup); }
                    throw failure;
                }
                return null;
            });
        } catch(IOException failure) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Не вдалося зберегти медіафайл",failure);
        } finally {
            if(staged!=null)try { Files.deleteIfExists(staged); } catch(IOException ignored) { }
            stagingSlots.release();
        }
        return new StoredMedia("/media/posts/"+owner+"/"+target.getFileName(),type);
    }
    private void checkFreeSpace(long additional) throws IOException {
        if(Files.getFileStore(uploadDirectory).getUsableSpace()<additional+512L*1024*1024)
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE,"Недостатньо місця для медіафайлу");
    }
    private static boolean starts(byte[] h,int... values) {
        if(h.length<values.length)return false;
        for(int i=0;i<values.length;i++)if((h[i]&255)!=values[i])return false;
        return true;
    }
    private static String text(byte[] h,int start,int count) {
        return h.length<start+count?"":new String(h,start,count,java.nio.charset.StandardCharsets.US_ASCII);
    }
    private static boolean matches(String type,byte[] h) {
        return switch(type) {
            case "image/jpeg" -> starts(h,255,216,255);
            case "image/png" -> starts(h,137,80,78,71,13,10,26,10);
            case "image/gif" -> text(h,0,6).equals("GIF87a") || text(h,0,6).equals("GIF89a");
            case "image/webp" -> text(h,0,4).equals("RIFF") && text(h,8,4).equals("WEBP");
            case "video/mp4","video/quicktime" -> text(h,4,4).equals("ftyp");
            case "video/webm" -> starts(h,26,69,223,163);
            default -> false;
        };
    }
    private static ResponseStatusException unsupported() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Вміст файлу має відповідати формату JPEG, PNG, GIF, WebP, MP4, WebM або MOV");
    }
    public void delete(String url) {
        if(url==null || !url.startsWith("/media/posts/"))return;
        Path file=uploadDirectory.resolve(url.substring("/media/posts/".length())).normalize();
        if(!file.startsWith(uploadDirectory) || file.equals(uploadDirectory))return;
        Path relative=uploadDirectory.relativize(file);
        for(Path part:relative)if(part.toString().startsWith("."))return;
        // Current uploads have exactly UUID/filename; legacy flat files remain deletable.
        if(relative.getNameCount()>2)return;
        try {
            quotas.locked(() -> {
                if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file.getParent()))return null;
                long size=Files.size(file);
                String owner=relative.getNameCount()==2?relative.getName(0).toString():"legacy";
                var account=quotas.owner(owner);var total=quotas.total();
                quotas.begin();
                Files.delete(file);
                quotas.finish(owner,total.add(-size,-1),
                        relative.getNameCount()==2?account.add(-size,-1):account);
                return null;
            });
        } catch(IOException ignored) {
            // The file is retained on failure; dirty state forces reconciliation next time.
        }
    }
    public record StoredMedia(String url,String contentType) {}
}
