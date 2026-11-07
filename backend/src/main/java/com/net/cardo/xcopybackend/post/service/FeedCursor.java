package com.net.cardo.xcopybackend.post.service;

import java.io.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Public position, not an authorization token. Query filters always come from the server. */
public final class FeedCursor {
    private FeedCursor() {}
    public record Position(LocalDateTime createdAt,String id) {}
    public static String encode(String scope,Position position) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeByte(1);out.writeUTF(scope);
            // Round-trip the local calendar value; MongoDB dates have millisecond precision.
            out.writeLong(position.createdAt().truncatedTo(ChronoUnit.MILLIS).toInstant(ZoneOffset.UTC).toEpochMilli());
            out.writeUTF(position.id());out.close();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch(IOException impossible) {throw new IllegalStateException(impossible);}
    }
    public static Position decode(String scope,String cursor) {
        if(cursor==null)return null;
        if(cursor.length()>600 || !cursor.matches("[A-Za-z0-9_-]+"))throw invalid();
        try(var input=new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(cursor)))) {
            if(input.readUnsignedByte()!=1 || !input.readUTF().equals(scope))throw invalid();
            var date=LocalDateTime.ofInstant(Instant.ofEpochMilli(input.readLong()),ZoneOffset.UTC);
            String id=input.readUTF();
            if(id.isBlank() || id.length()>128 || input.available()!=0)throw invalid();
            var position=new Position(date,id);
            if(!encode(scope,position).equals(cursor))throw invalid();
            return position;
        } catch(IOException | IllegalArgumentException | DateTimeException malformed) {throw invalid();}
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Неправильний курсор стрічки. Оновіть сторінку.");
    }
}
