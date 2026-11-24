package com.queueless;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QueueLessApplication {
  public static void main(String[] args) { SpringApplication.run(QueueLessApplication.class, args); }

  @Bean
  Clock applicationClock(@Value("${queueless.local-date-zone:UTC}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }
}
