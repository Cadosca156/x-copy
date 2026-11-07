package com.net.cardo.xcopybackend.config;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "app.capacity.requests=2","app.capacity.auth=2","app.capacity.reads=2","app.capacity.uploads=2","app.capacity.media=2"})
@Import(CapacityIntegrationTests.Fixture.class)
class CapacityIntegrationTests {
    static class Gate {final CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);}
    @TestConfiguration static class Fixture {
        @Bean Gate gate(){return new Gate();}
        @Bean ServletRegistrationBean<HttpServlet> capacityProbe(Gate gate) {
            return new ServletRegistrationBean<>(new HttpServlet(){
                @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
                    gate.entered.countDown();
                    try {if(!gate.release.await(10,TimeUnit.SECONDS))throw new IOException("Probe deadline");}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
                    response.getWriter().write("ok");
                }
            },"/api/auth/capacity-probe");
        }
    }
    @Value("${local.server.port}") int port;
    @Autowired Gate gate;
    @Autowired JdbcTemplate jdbc;
    @Test void realServerRejectsExcessRequestAndRecovers() throws Exception {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/capacity-probe")).timeout(Duration.ofSeconds(10)).build();
        var first=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        var second=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        try {
            assertTrue(gate.entered.await(5,TimeUnit.SECONDS));
            var rejected=client.send(request,HttpResponse.BodyHandlers.ofString());
            assertEquals(503,rejected.statusCode());assertEquals("2",rejected.headers().firstValue("Retry-After").orElseThrow());
        } finally {gate.release.countDown();}
        assertEquals(200,first.get(5,TimeUnit.SECONDS).statusCode());assertEquals(200,second.get(5,TimeUnit.SECONDS).statusCode());
        assertEquals(200,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void postgresActuallyCancelsSlowStatements() {
        assertEquals("5s",jdbc.queryForObject("SHOW statement_timeout",String.class));
        assertEquals("1s",jdbc.queryForObject("SHOW lock_timeout",String.class));
        assertThrows(org.springframework.dao.QueryTimeoutException.class,()->jdbc.execute("SELECT pg_sleep(6)"));
    }
}
