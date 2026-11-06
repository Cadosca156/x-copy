package com.net.cardo.xcopybackend.config;

import jakarta.servlet.*;
import java.io.IOException;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class CapacityFilterTest {
    MockHttpServletRequest request(String method,String path) {return new MockHttpServletRequest(method,path);}
    @Test void busyAuthDoesNotOccupyReadCapacityAndAdmissionRecovers() throws Exception {
        var filter=new CapacityFilter(3,1,1,1,1);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var pool=Executors.newSingleThreadExecutor();
        try {
            var first=pool.submit(()->{
                try {filter.doFilter(request("POST","/api/auth/login"),new MockHttpServletResponse(),(a,b)->{
                    entered.countDown();try {assertTrue(release.await(5,TimeUnit.SECONDS));}
                    catch(InterruptedException e){throw new RuntimeException(e);}
                });} catch(Exception e){throw new RuntimeException(e);}
            });
            assertTrue(entered.await(5,TimeUnit.SECONDS));var rejected=new MockHttpServletResponse();
            filter.doFilter(request("POST","/api/auth/login"),rejected,(a,b)->fail("Rejected auth reached application"));
            assertEquals(503,rejected.getStatus());assertEquals("2",rejected.getHeader("Retry-After"));
            var read=new MockHttpServletResponse();filter.doFilter(request("GET","/api/posts"),read,(a,b)->b.getWriter().write("ok"));
            assertEquals("ok",read.getContentAsString());
            release.countDown();first.get(5,TimeUnit.SECONDS);
            filter.doFilter(request("POST","/api/auth/login"),new MockHttpServletResponse(),(a,b)->b.getWriter().write("recovered"));
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void globalLimitAndExceptionPathsAlwaysReleasePermits() throws Exception {
        var filter=new CapacityFilter(1,1,1,1,1);var rejected=new MockHttpServletResponse();
        filter.doFilter(request("GET","/api/users/me"),new MockHttpServletResponse(),(a,b)->
                filter.doFilter(request("GET","/api/other"),rejected,(c,d)->fail("Global limit bypassed")));
        assertEquals(503,rejected.getStatus());
        assertThrows(IOException.class,()->filter.doFilter(request("GET","/api/posts"),new MockHttpServletResponse(),(a,b)->{throw new IOException("downstream");}));
        var recovered=new MockHttpServletResponse();filter.doFilter(request("GET","/api/posts"),recovered,(a,b)->b.getWriter().write("ok"));
        assertEquals("ok",recovered.getContentAsString());
    }
    @Test void exhaustedUploadCapacityRejectsBeforeReadingBody() throws Exception {
        var filter=new CapacityFilter(2,1,1,1,1);var first=request("POST","/api/posts");first.setContentType("multipart/form-data; boundary=x");
        var second=new MockHttpServletRequest("POST","/api/posts") {
            @Override public jakarta.servlet.ServletInputStream getInputStream(){throw new AssertionError("Body was read");}
        };second.setContentType("multipart/form-data; boundary=x");var response=new MockHttpServletResponse();
        filter.doFilter(first,new MockHttpServletResponse(),(a,b)->filter.doFilter(second,response,(c,d)->fail("Upload limit bypassed")));
        assertEquals(503,response.getStatus());
    }
    @Test void staticNonMediaAndPreflightDoNotConsumeApiSlots() throws Exception {
        var filter=new CapacityFilter(1,1,1,1,1);
        filter.doFilter(request("GET","/api/posts"),new MockHttpServletResponse(),(a,b)->{
            filter.doFilter(request("OPTIONS","/api/posts"),new MockHttpServletResponse(),(c,d)->d.getWriter().write("ok"));
            filter.doFilter(request("GET","/favicon.ico"),new MockHttpServletResponse(),(c,d)->d.getWriter().write("ok"));
        });
        assertThrows(IllegalArgumentException.class,()->new CapacityFilter(0,1,1,1,1));
    }
}
