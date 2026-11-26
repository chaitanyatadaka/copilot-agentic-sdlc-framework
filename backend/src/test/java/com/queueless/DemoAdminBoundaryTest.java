package com.queueless;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.api.Dtos;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-admin-boundary;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@ActiveProfiles("demo")
class DemoAdminBoundaryTest {
  @LocalServerPort int port;
  @Autowired Environment environment;
  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper json;

  @Test void demoServerBindsToLoopbackAndLocalAdminOperationsRemainAvailable() throws Exception {
    assertTrue(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("demo")));
    String bindAddress = environment.getRequiredProperty("server.address");
    assertTrue(InetAddress.getByName(bindAddress).isLoopbackAddress());

    String baseUrl = "http://127.0.0.1:" + port;
    ResponseEntity<String> created = postJson(baseUrl + "/api/queues",
        new Dtos.QueueRequest("Local demo", "LOCAL", 5, true));
    assertEquals(CREATED, created.getStatusCode());
    Dtos.QueueResponse queue = json.readValue(created.getBody(), Dtos.QueueResponse.class);

    ResponseEntity<String> joined = postJson(baseUrl + "/api/queues/" + queue.queueId() + "/join",
        new Dtos.JoinRequest("Guest", null));
    assertEquals(CREATED, joined.getStatusCode());
    Dtos.TokenResponse token = json.readValue(joined.getBody(), Dtos.TokenResponse.class);

    ResponseEntity<String> called = postJson(baseUrl + "/api/queues/" + queue.queueId() + "/call-next", null);
    assertEquals(OK, called.getStatusCode());
    assertEquals(token.tokenId(), json.readValue(called.getBody(), Dtos.TokenResponse.class).tokenId());

    ResponseEntity<String> completed = postJson(baseUrl + "/api/tokens/" + token.tokenId() + "/complete", null);
    assertEquals(OK, completed.getStatusCode());
    assertEquals("COMPLETED", json.readTree(completed.getBody()).get("status").asText());
  }

  private ResponseEntity<String> postJson(String url, Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return http.exchange(url, POST, new HttpEntity<>(body, headers), String.class);
  }
}
