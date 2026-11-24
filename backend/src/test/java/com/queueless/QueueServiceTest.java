package com.queueless;

import com.queueless.api.Dtos;
import com.queueless.api.QueueService;
import com.queueless.persistence.QueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {
  @Mock QueueRepository queues;
  @Test void rejectsInvalidDurationWithoutSaving(){
    QueueService service=new QueueService(queues,Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"),ZoneOffset.UTC));
    assertThrows(RuntimeException.class,()->service.create(new Dtos.QueueRequest("A","A",0,true)));
    verify(queues,never()).save(any());
  }
}
