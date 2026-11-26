package com.queueless;

import com.queueless.api.Dtos;
import com.queueless.api.QueueService;
import com.queueless.api.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceRestartTest {

  // T-P0-02 / T-P1-01–02 / REQ-019, REQ-030, REQ-033–034
  @Test
  void queueAndTokenSurviveClosingAndRestartingBackendContext(@TempDir Path tempDir) {
    String databaseFile=tempDir.resolve("queueless").toString().replace('\\','/');
    String databaseUrl="jdbc:h2:file:"+databaseFile+";AUTO_SERVER=FALSE";
    Long queueId;
    Long tokenId;

    try(ConfigurableApplicationContext first=startBackend(databaseUrl)) {
      QueueService queues=first.getBean(QueueService.class);
      TokenService tokens=first.getBean(TokenService.class);
      Dtos.QueueResponse queue=queues.create(
          new Dtos.QueueRequest("Restart test queue","RST",6,true));
      Dtos.TokenResponse token=tokens.join(queue.queueId(),new Dtos.JoinRequest("Persisted guest","555-0101"));
      queueId=queue.queueId();
      tokenId=token.tokenId();
    }

    try(ConfigurableApplicationContext restarted=startBackend(databaseUrl)) {
      Dtos.QueueResponse queue=restarted.getBean(QueueService.class).get(queueId);
      Dtos.TokenResponse token=restarted.getBean(TokenService.class).snapshot(tokenId);
      assertEquals("Restart test queue",queue.name());
      assertEquals("RST",queue.prefix());
      assertEquals("RST-1",token.displayIdentifier());
      assertEquals("WAITING",token.status().name());
      assertEquals(1,token.position());
      assertEquals(0,token.estimatedWaitMinutes());
    }
  }

  private ConfigurableApplicationContext startBackend(String databaseUrl) {
    return new SpringApplicationBuilder(QueueLessApplication.class)
        .web(WebApplicationType.NONE)
        .run("--spring.profiles.active=demo",
            "--spring.main.banner-mode=off",
            "--spring.datasource.url="+databaseUrl,
            "--spring.datasource.username=sa",
            "--spring.datasource.password=",
            "--spring.jpa.hibernate.ddl-auto=update",
            "--spring.jpa.open-in-view=false",
            "--queueless.local-date-zone=UTC");
  }
}
