package com.net.cardo.xcopybackend.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

@Service
public class MediaStorageService {
    private static final long MAX_FILE = 25L * 1024 * 1024;
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
    }
    public synchronized StoredMedia store(MultipartFile file, UUID owner) {
        if(file==null || owner==null || file.isEmpty() || file.getSize()>MAX_FILE)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Файл має містити до 25 МБ");
        String type=file.getContentType(), extension=EXTENSIONS.get(type==null?"":type);
        if(extension==null) throw unsupported();
        Path directory=uploadDirectory.resolve(owner.toString());
        Path target=directory.resolve(UUID.randomUUID()+"."+extension);
        try {
            Files.createDirectories(directory);
            // Lock across application processes before checking disk quotas and writing.
            try(var channel=FileChannel.open(uploadDirectory.resolve(".quota.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                var lock=channel.lock()) {
                long[] total=usage(uploadDirectory), account=usage(directory);
                long size=file.getSize();
                if(total[0]+size>totalMaxBytes || account[0]+size>ownerMaxBytes ||
                   total[1]>=totalMaxFiles || account[1]>=ownerMaxFiles ||
                   Files.getFileStore(directory).getUsableSpace()<size+512L*1024*1024)
                    throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE,"Ліміт медіафайлів вичерпано. Видаліть зайві файли.");
                try(InputStream input=new BufferedInputStream(file.getInputStream())) {
                    input.mark(32);
                    byte[] header=input.readNBytes(16); input.reset();
                    if(!matches(type,header)) throw unsupported();
                    try(OutputStream output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer=new byte[8192]; long copied=0; int read;
                        while((read=input.read(buffer))!=-1) {
                            copied+=read;
                            if(copied>size || copied>MAX_FILE) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Завеликий файл");
                            output.write(buffer,0,read);
                        }
                        if(copied!=size) throw new IOException("Invalid upload length");
                    }
                } catch(IOException | RuntimeException failure) { Files.deleteIfExists(target); throw failure; }
            }
        } catch(IOException failure) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Не вдалося зберегти медіафайл",failure);
        }
        return new StoredMedia("/media/posts/"+owner+"/"+target.getFileName(),type);
    }
    private long[] usage(Path directory) throws IOException {
        long bytes=0, count=0;
        try(var files=Files.walk(directory)) {
            for(Path path:files.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS) && !p.getFileName().toString().startsWith(".")).toList()) {
                bytes+=Files.size(path); count++;
            }
        }
        return new long[]{bytes,count};
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
        if(!file.startsWith(uploadDirectory) || file.equals(uploadDirectory) || file.getFileName().toString().startsWith("."))return;
        try { Files.deleteIfExists(file); } catch(IOException ignored) { }
    }
    public record StoredMedia(String url,String contentType) {}
}
