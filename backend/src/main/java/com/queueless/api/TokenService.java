package com.queueless.api;

import com.queueless.domain.*;
import com.queueless.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
  private final QueueRepository queues; private final TokenRepository tokens; private final Clock clock;
  public TokenService(QueueRepository queues,TokenRepository tokens,Clock clock){this.queues=queues;this.tokens=tokens;this.clock=clock;}

  @Transactional
  public Dtos.TokenResponse join(Long queueId,Dtos.JoinRequest r){
    if(r.customerName()==null||r.customerName().trim().isEmpty()) throw ApiException.badRequest("Customer name is required.");
    QueueEntity q=queues.findByIdForUpdate(queueId).orElseThrow(()->ApiException.notFound("Queue not found."));
    if(!q.isOpen()) throw ApiException.conflict("Queue is closed.");
    LocalDate today=LocalDate.now(clock);
    if(!today.equals(q.getNumberingDay())) q.resetNumbering(today);
    String name=r.customerName().trim(), phone=r.phone()==null?null:r.phone().trim();
    for(TokenEntity t:tokens.duplicateByName(queueId,name)){
      if(phone==null||phone.isEmpty()||Objects.equals(t.getPhone(),phone)) throw ApiException.conflict("This guest already has an active waiting token.");
    }
    q.incrementSequence();
    TokenEntity t=new TokenEntity(q,today,q.getLastSequenceNumber(),q.getPrefix()+"-"+q.getLastSequenceNumber(),name,phone);
    t=tokens.save(t);
    return snapshot(t);
  }

  @Transactional(readOnly=true)
  public Dtos.TokenResponse snapshot(Long tokenId){
    TokenEntity t=tokens.findById(tokenId).orElseThrow(()->ApiException.notFound("Token not found."));
    return snapshot(t);
  }
  private Dtos.TokenResponse snapshot(TokenEntity t){
    Integer position=null, estimate=null;
    if(t.getStatus()==TokenStatus.WAITING){
      position=1+tokens.precedingWaiting(t.getQueue().getId(),TokenStatus.WAITING,t.getSequenceNumber(),t.getId()).size();
      estimate=(position-1)*t.getQueue().getAverageServiceDurationMinutes();
    }
    return new Dtos.TokenResponse(t.getId(),t.getDisplayIdentifier(),t.getQueue().getId(),t.getStatus(),position,estimate);
  }

  @Transactional
  public Dtos.TokenResponse cancel(Long tokenId){
    TokenEntity t=tokens.findById(tokenId).orElseThrow(()->ApiException.notFound("Token not found."));
    QueueEntity q=queues.findByIdForUpdate(t.getQueue().getId()).orElseThrow(()->ApiException.notFound("Queue not found."));
    t=tokens.findById(tokenId).orElseThrow(()->ApiException.notFound("Token not found."));
    if(t.getStatus()!=TokenStatus.WAITING) throw ApiException.conflict("This token cannot be cancelled in its current state.");
    t.setStatus(TokenStatus.CANCELLED,Instant.now(clock));return snapshot(t);
  }

  @Transactional
  public Dtos.TokenResponse callNext(Long queueId){
    QueueEntity q=queues.findByIdForUpdate(queueId).orElseThrow(()->ApiException.notFound("Queue not found."));
    if(!tokens.findByQueueAndStatus(queueId,TokenStatus.CALLED).isEmpty()) throw ApiException.conflict("This queue already has a called token.");
    List<TokenEntity> waiting=tokens.waitingForQueue(queueId,TokenStatus.WAITING);
    if(waiting.isEmpty()) throw ApiException.conflict("There are no waiting tokens.");
    TokenEntity t=waiting.get(0);t.setStatus(TokenStatus.CALLED,Instant.now(clock));return snapshot(t);
  }

  @Transactional
  public Dtos.TokenResponse complete(Long tokenId){
    return terminal(tokenId,TokenStatus.COMPLETED);
  }
  @Transactional
  public Dtos.TokenResponse skip(Long tokenId){
    return terminal(tokenId,TokenStatus.SKIPPED);
  }
  private Dtos.TokenResponse terminal(Long tokenId,TokenStatus target){
    TokenEntity t=tokens.findById(tokenId).orElseThrow(()->ApiException.notFound("Token not found."));
    queues.findByIdForUpdate(t.getQueue().getId()).orElseThrow(()->ApiException.notFound("Queue not found."));
    t=tokens.findById(tokenId).orElseThrow(()->ApiException.notFound("Token not found."));
    if(t.getStatus()!=TokenStatus.CALLED) throw ApiException.conflict("Only a called token can be completed or skipped.");
    t.setStatus(target,Instant.now(clock));return snapshot(t);
  }

  @Transactional(readOnly=true)
  public Dtos.DashboardResponse dashboard(Long queueId){
    QueueEntity q=queues.findById(queueId).orElseThrow(()->ApiException.notFound("Queue not found."));
    Map<TokenStatus,Long> counts=new EnumMap<>(TokenStatus.class);
    for(TokenStatus s:TokenStatus.values()) counts.put(s,tokens.countByQueueIdAndStatus(queueId,s));
    List<Dtos.ActiveToken> active=tokens.waitingForQueue(queueId,TokenStatus.WAITING).stream()
      .map(t->new Dtos.ActiveToken(t.getId(),t.getDisplayIdentifier(),t.getCustomerName(),t.getStatus(),t.getSequenceNumber())).toList();
    List<Dtos.ActiveToken> called=tokens.findByQueueAndStatus(queueId,TokenStatus.CALLED).stream()
      .map(t->new Dtos.ActiveToken(t.getId(),t.getDisplayIdentifier(),t.getCustomerName(),t.getStatus(),t.getSequenceNumber())).toList();
    List<Dtos.ActiveToken> combined=new ArrayList<>(active);combined.addAll(called);
    return new Dtos.DashboardResponse(queueId,counts,combined);
  }
}
