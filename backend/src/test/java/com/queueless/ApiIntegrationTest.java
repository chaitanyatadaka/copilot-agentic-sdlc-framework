package com.queueless;

import com.queueless.api.Dtos;
import com.queueless.persistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired QueueRepository queues;

  @Test void joinDuplicateCancelAndLifecycleStatuses() throws Exception {
    String queue=mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Dtos.QueueRequest("Returns","R",5,true))))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    Dtos.QueueResponse created=json.readValue(queue,Dtos.QueueResponse.class);
    String joined=mvc.perform(post("/api/queues/"+created.queueId()+"/join").contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Sam\",\"phone\":\"123\"}"))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.displayIdentifier").value("R-1")).andReturn().getResponse().getContentAsString();
    Dtos.TokenResponse token=json.readValue(joined,Dtos.TokenResponse.class);
    mvc.perform(post("/api/queues/"+created.queueId()+"/join").contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Sam\",\"phone\":\"123\"}"))
      .andExpect(status().isConflict());
    mvc.perform(post("/api/tokens/"+token.tokenId()+"/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test void validationAndNotFoundUseFixedStatusMapping() throws Exception {
    mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"prefix\":\"\",\"averageServiceDurationMinutes\":0,\"open\":true}"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION"));
    mvc.perform(get("/api/queues/99999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test void callCompletesInFifoAndRejectsSecondCall() throws Exception {
    String queue=mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Desk\",\"prefix\":\"D\",\"averageServiceDurationMinutes\":4,\"open\":true}"))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    Dtos.QueueResponse q=json.readValue(queue,Dtos.QueueResponse.class);
    for(String name:new String[]{"A","B"}) mvc.perform(post("/api/queues/"+q.queueId()+"/join").contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\""+name+"\"}")).andExpect(status().isCreated());
    String first=mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isOk()).andExpect(jsonPath("$.displayIdentifier").value("D-1")).andReturn().getResponse().getContentAsString();
    Dtos.TokenResponse called=json.readValue(first,Dtos.TokenResponse.class);
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isConflict());
    mvc.perform(post("/api/tokens/"+called.tokenId()+"/complete")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  // T-P2-07 / REQ-004, REQ-005, REQ-017, REQ-045, REQ-046
  @Test void positionAndEstimateFollowOnlyCurrentWaitingTokens() throws Exception {
    Dtos.QueueResponse q=createQueue("P",7,true);
    Dtos.TokenResponse first=join(q.queueId(),"First");
    Dtos.TokenResponse second=join(q.queueId(),"Second");
    Dtos.TokenResponse third=join(q.queueId(),"Third");

    mvc.perform(get("/api/tokens/"+first.tokenId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1))
      .andExpect(jsonPath("$.estimatedWaitMinutes").value(0));
    mvc.perform(get("/api/tokens/"+second.tokenId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(2))
      .andExpect(jsonPath("$.estimatedWaitMinutes").value(7));
    mvc.perform(get("/api/tokens/"+third.tokenId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(3))
      .andExpect(jsonPath("$.estimatedWaitMinutes").value(14));

    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.tokenId").value(first.tokenId()));
    mvc.perform(post("/api/tokens/"+second.tokenId()+"/cancel"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    mvc.perform(get("/api/tokens/"+third.tokenId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(1))
      .andExpect(jsonPath("$.estimatedWaitMinutes").value(0));
    mvc.perform(get("/api/tokens/"+first.tokenId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.position").doesNotExist())
      .andExpect(jsonPath("$.estimatedWaitMinutes").doesNotExist());
  }

  // T-P2-05 / T-P2-08 / REQ-010, REQ-018, REQ-047, REQ-048, REQ-049
  @Test void skippedTokensAreTerminalAndARejoinGetsANewToken() throws Exception {
    Dtos.QueueResponse q=createQueue("S",5,true);
    Dtos.TokenResponse original=join(q.queueId(),"Guest");
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isOk());
    mvc.perform(post("/api/tokens/"+original.tokenId()+"/skip"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SKIPPED"));
    mvc.perform(post("/api/tokens/"+original.tokenId()+"/complete")).andExpect(status().isConflict());
    mvc.perform(post("/api/tokens/"+original.tokenId()+"/skip")).andExpect(status().isConflict());
    mvc.perform(post("/api/tokens/"+original.tokenId()+"/cancel")).andExpect(status().isConflict());

    mvc.perform(post("/api/queues/"+q.queueId()+"/join")
        .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Guest\"}"))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.displayIdentifier").value("S-2"))
      .andExpect(jsonPath("$.status").value("WAITING"));
  }

  // T-P2-02 / T-P3-02 / REQ-016, REQ-049, REQ-052
  @Test void rejectedClosedQueueJoinDoesNotConsumeANumber() throws Exception {
    Dtos.QueueResponse q=createQueue("C",5,false);
    mvc.perform(post("/api/queues/"+q.queueId()+"/join")
        .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Guest\"}"))
      .andExpect(status().isConflict());
    mvc.perform(patch("/api/queues/"+q.queueId()+"/status")
        .contentType(MediaType.APPLICATION_JSON).content("{\"open\":true}"))
      .andExpect(status().isOk());
    mvc.perform(post("/api/queues/"+q.queueId()+"/join")
        .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Guest\"}"))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.displayIdentifier").value("C-1"));
  }

  // T-P2-01 / T-P3-01 / T-P3-04 / REQ-011, REQ-024, REQ-049, REQ-050–052, REQ-058
  @Test void invalidOrConflictingQueueConfigurationDoesNotPartiallyUpdate() throws Exception {
    Dtos.QueueResponse original=createQueue("CFG1",5,true);
    Dtos.QueueResponse other=createQueue("CFG2",8,false);

    mvc.perform(put("/api/queues/"+original.queueId()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"\",\"prefix\":\"CFG1\",\"averageServiceDurationMinutes\":0,\"open\":false}"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION"));
    mvc.perform(get("/api/queues/"+original.queueId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Queue CFG1"))
      .andExpect(jsonPath("$.prefix").value("CFG1"))
      .andExpect(jsonPath("$.averageServiceDurationMinutes").value(5))
      .andExpect(jsonPath("$.open").value(true));

    mvc.perform(put("/api/queues/"+original.queueId()).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(new Dtos.QueueRequest("Changed","CFG2",12,false))))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
    mvc.perform(get("/api/queues/"+original.queueId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Queue CFG1"))
      .andExpect(jsonPath("$.prefix").value("CFG1"))
      .andExpect(jsonPath("$.averageServiceDurationMinutes").value(5))
      .andExpect(jsonPath("$.open").value(true));
    mvc.perform(get("/api/queues/"+other.queueId()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.prefix").value("CFG2"))
      .andExpect(jsonPath("$.averageServiceDurationMinutes").value(8))
      .andExpect(jsonPath("$.open").value(false));
  }

  // T-P2-03 / T-P3-02 / REQ-038–040, REQ-049
  @Test void duplicateIdentityUsesOptionalPhoneAndOnlyActiveWaitingTokens() throws Exception {
    Dtos.QueueResponse q=createQueue("DUP",5,true);
    Dtos.TokenResponse first=joinWithPhone(q.queueId(),"Same guest","111");
    Dtos.TokenResponse differentPhone=joinWithPhone(q.queueId(),"Same guest","222");
    assertEquals("DUP-1",first.displayIdentifier());
    assertEquals("DUP-2",differentPhone.displayIdentifier());

    mvc.perform(post("/api/queues/"+q.queueId()+"/join")
        .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Same guest\"}"))
      .andExpect(status().isConflict());
    mvc.perform(post("/api/queues/"+q.queueId()+"/join")
        .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\" Same guest \",\"phone\":\"111\"}"))
      .andExpect(status().isConflict());
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.tokenId").value(first.tokenId()));
    mvc.perform(post("/api/tokens/"+first.tokenId()+"/complete")).andExpect(status().isOk());

    Dtos.TokenResponse rejoin=joinWithPhone(q.queueId(),"Same guest","111");
    assertEquals("DUP-3",rejoin.displayIdentifier());
  }

  // T-P1-03 / T-P3-03 / REQ-007, REQ-012, REQ-017, REQ-053–054
  @Test void dashboardCountsAllStatusesAndListsOnlyWaitingAndCalledTokens() throws Exception {
    Dtos.QueueResponse q=createQueue("DASH",5,true);
    Dtos.TokenResponse completed=join(q.queueId(),"Completed");
    Dtos.TokenResponse skipped=join(q.queueId(),"Skipped");
    Dtos.TokenResponse cancelled=join(q.queueId(),"Cancelled");
    Dtos.TokenResponse waiting=join(q.queueId(),"Waiting");
    Dtos.TokenResponse called=join(q.queueId(),"Called");

    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isOk())
      .andExpect(jsonPath("$.tokenId").value(completed.tokenId()));
    mvc.perform(post("/api/tokens/"+completed.tokenId()+"/complete")).andExpect(status().isOk());
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isOk())
      .andExpect(jsonPath("$.tokenId").value(skipped.tokenId()));
    mvc.perform(post("/api/tokens/"+skipped.tokenId()+"/skip")).andExpect(status().isOk());
    mvc.perform(post("/api/tokens/"+cancelled.tokenId()+"/cancel")).andExpect(status().isOk());
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isOk())
      .andExpect(jsonPath("$.tokenId").value(waiting.tokenId()));
    mvc.perform(post("/api/queues/"+q.queueId()+"/call-next")).andExpect(status().isConflict());

    mvc.perform(get("/api/queues/"+q.queueId()+"/dashboard"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.counts.WAITING").value(1))
      .andExpect(jsonPath("$.counts.CALLED").value(1))
      .andExpect(jsonPath("$.counts.COMPLETED").value(1))
      .andExpect(jsonPath("$.counts.SKIPPED").value(1))
      .andExpect(jsonPath("$.counts.CANCELLED").value(1))
      .andExpect(jsonPath("$.activeTokens.length()").value(2))
      .andExpect(jsonPath("$.activeTokens[0].tokenId").value(called.tokenId()))
      .andExpect(jsonPath("$.activeTokens[0].status").value("WAITING"))
      .andExpect(jsonPath("$.activeTokens[1].tokenId").value(waiting.tokenId()))
      .andExpect(jsonPath("$.activeTokens[1].status").value("CALLED"));
  }

  // T-P2-02 / T-P6-01 / REQ-015, REQ-022, REQ-041
  @Test void numberingResetsLazilyAndIndependentlyForEachQueue() throws Exception {
    Dtos.QueueResponse resetQueue=createQueue("DAY1",5,true);
    Dtos.QueueResponse independentQueue=createQueue("DAY2",5,true);
    LocalDate today=LocalDate.now(ZoneOffset.UTC);

    QueueEntity reset=queues.findById(resetQueue.queueId()).orElseThrow();
    reset.resetNumbering(today.minusDays(1));
    for(int i=0;i<6;i++) reset.incrementSequence();
    QueueEntity independent=queues.findById(independentQueue.queueId()).orElseThrow();
    independent.resetNumbering(today);
    for(int i=0;i<4;i++) independent.incrementSequence();
    queues.saveAndFlush(reset);
    queues.saveAndFlush(independent);

    Dtos.TokenResponse resetJoin=join(resetQueue.queueId(),"Reset guest");
    assertEquals("DAY1-1",resetJoin.displayIdentifier());
    assertEquals(1,resetJoin.position());
    assertEquals(0,resetJoin.estimatedWaitMinutes());
    Dtos.TokenResponse independentJoin=join(independentQueue.queueId(),"Other guest");
    assertEquals("DAY2-5",independentJoin.displayIdentifier());
    Dtos.QueueResponse resetAfter=json.readValue(mvc.perform(get("/api/queues/"+resetQueue.queueId()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Dtos.QueueResponse.class);
    Dtos.QueueResponse independentAfter=json.readValue(mvc.perform(get("/api/queues/"+independentQueue.queueId()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Dtos.QueueResponse.class);
    assertEquals(today,resetAfter.dailySequenceResetOn());
    assertEquals(1,resetAfter.lastSequenceNumber());
    assertEquals(today,independentAfter.dailySequenceResetOn());
    assertEquals(5,independentAfter.lastSequenceNumber());
  }

  // T-P6-03 / REQ-013, REQ-014, REQ-022, REQ-026, REQ-040, REQ-049
  @Test void concurrentJoinsAndCallsPreservePerQueueInvariants() throws Exception {
    Dtos.QueueResponse q=createQueue("XCON",3,true);
    int workers=8;
    ExecutorService pool=Executors.newFixedThreadPool(workers);
    try {
      CountDownLatch ready=new CountDownLatch(workers), start=new CountDownLatch(1);
      var joins=IntStream.range(0,workers).mapToObj(i->pool.submit(()->{
        ready.countDown();
        if(!start.await(5,TimeUnit.SECONDS)) throw new TimeoutException("join start timed out");
        return mvc.perform(post("/api/queues/"+q.queueId()+"/join")
            .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Guest"+i+"\"}"))
          .andReturn().getResponse();
      })).toList();
      assertTrue(ready.await(5,TimeUnit.SECONDS),"join workers did not become ready");
      start.countDown();
      var identifiers=new HashSet<String>();
      for(var future:joins) {
        var response=future.get(30,TimeUnit.SECONDS);
        assertEquals(201,response.getStatus());
        Dtos.TokenResponse token=json.readValue(response.getContentAsString(),Dtos.TokenResponse.class);
        identifiers.add(token.displayIdentifier());
      }
      assertEquals(workers,identifiers.size(),"each concurrent join must allocate a unique token");

      CountDownLatch callReady=new CountDownLatch(workers), callStart=new CountDownLatch(1);
      var calls=IntStream.range(0,workers).mapToObj(i->pool.submit(()->{
        callReady.countDown();
        if(!callStart.await(5,TimeUnit.SECONDS)) throw new TimeoutException("call start timed out");
        return mvc.perform(post("/api/queues/"+q.queueId()+"/call-next"))
          .andReturn().getResponse().getStatus();
      })).toList();
      assertTrue(callReady.await(5,TimeUnit.SECONDS),"call workers did not become ready");
      callStart.countDown();
      var statuses=new ArrayList<Integer>();
      for(var future:calls) statuses.add(future.get(30,TimeUnit.SECONDS));
      assertEquals(1,statuses.stream().filter(s->s==200).count(),"only one concurrent call may succeed");
      assertEquals(workers-1,statuses.stream().filter(s->s==409).count(),"other calls must conflict");

      CountDownLatch duplicateReady=new CountDownLatch(workers), duplicateStart=new CountDownLatch(1);
      var duplicateJoins=IntStream.range(0,workers).mapToObj(i->pool.submit(()->{
        duplicateReady.countDown();
        if(!duplicateStart.await(5,TimeUnit.SECONDS)) throw new TimeoutException("duplicate join start timed out");
        return mvc.perform(post("/api/queues/"+q.queueId()+"/join")
            .contentType(MediaType.APPLICATION_JSON).content("{\"customerName\":\"Same guest\"}"))
          .andReturn().getResponse().getStatus();
      })).toList();
      assertTrue(duplicateReady.await(5,TimeUnit.SECONDS),"duplicate join workers did not become ready");
      duplicateStart.countDown();
      statuses.clear();
      for(var future:duplicateJoins) statuses.add(future.get(30,TimeUnit.SECONDS));
      assertEquals(1,statuses.stream().filter(s->s==201).count(),"only one concurrent duplicate join may succeed");
      assertEquals(workers-1,statuses.stream().filter(s->s==409).count(),"duplicate joins must conflict");
    } finally {
      pool.shutdownNow();
      assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS),"worker pool did not stop");
    }
  }

  private Dtos.QueueResponse createQueue(String prefix,int duration,boolean open) throws Exception {
    String body=mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(new Dtos.QueueRequest("Queue "+prefix,prefix,duration,open))))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return json.readValue(body,Dtos.QueueResponse.class);
  }

  private Dtos.TokenResponse join(Long queueId,String name) throws Exception {
    String body=mvc.perform(post("/api/queues/"+queueId+"/join").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(new Dtos.JoinRequest(name,null))))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return json.readValue(body,Dtos.TokenResponse.class);
  }

  private Dtos.TokenResponse joinWithPhone(Long queueId,String name,String phone) throws Exception {
    String body=mvc.perform(post("/api/queues/"+queueId+"/join").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(new Dtos.JoinRequest(name,phone))))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return json.readValue(body,Dtos.TokenResponse.class);
  }
}
