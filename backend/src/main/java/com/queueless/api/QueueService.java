package com.queueless.api;

import com.queueless.domain.ApiException;
import com.queueless.persistence.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueService {
  private final QueueRepository queues;
  private final Clock clock;
  public QueueService(QueueRepository queues, Clock clock){this.queues=queues;this.clock=clock;}

  @Transactional(readOnly=true)
  public List<Dtos.QueueSummary> list(){return queues.findAll().stream().map(q->new Dtos.QueueSummary(q.getId(),q.getName(),q.getPrefix(),q.isOpen())).toList();}

  @Transactional(readOnly=true)
  public Dtos.QueueResponse get(Long id){return toResponse(require(id));}

  @Transactional
  public Dtos.QueueResponse create(Dtos.QueueRequest r){validate(r.name(),r.prefix(),r.averageServiceDurationMinutes());
    if(queues.existsByPrefixIgnoreCase(r.prefix().trim())) throw ApiException.conflict("Queue prefix already exists.");
    QueueEntity q=new QueueEntity(r.name().trim(),r.prefix().trim(),r.averageServiceDurationMinutes(),r.open()!=null&&r.open(),LocalDate.now(clock));
    return toResponse(queues.save(q));}

  @Transactional
  public Dtos.QueueResponse update(Long id,Dtos.QueueRequest r){validate(r.name(),r.prefix(),r.averageServiceDurationMinutes());
    QueueEntity q=queues.findByIdForUpdate(id).orElseThrow(()->ApiException.notFound("Queue not found."));
    if(queues.existsByPrefixIgnoreCaseAndIdNot(r.prefix().trim(),id)) throw ApiException.conflict("Queue prefix already exists.");
    q.setName(r.name().trim());q.setPrefix(r.prefix().trim());q.setAverageServiceDurationMinutes(r.averageServiceDurationMinutes());q.setOpen(r.open()!=null&&r.open());
    return toResponse(q);}

  @Transactional
  public Dtos.QueueResponse setStatus(Long id,boolean open){QueueEntity q=queues.findByIdForUpdate(id).orElseThrow(()->ApiException.notFound("Queue not found."));
    q.setOpen(open);return toResponse(q);}

  private void validate(String name,String prefix,int duration){
    if(name==null||name.trim().isEmpty()||prefix==null||prefix.trim().isEmpty()) throw ApiException.badRequest("Name and prefix are required.");
    if(duration<=0) throw ApiException.badRequest("Average service duration must be positive.");
  }
  private QueueEntity require(Long id){return queues.findById(id).orElseThrow(()->ApiException.notFound("Queue not found."));}
  private Dtos.QueueResponse toResponse(QueueEntity q){return new Dtos.QueueResponse(q.getId(),q.getName(),q.getPrefix(),q.getAverageServiceDurationMinutes(),q.isOpen(),q.getNumberingDay(),q.getLastSequenceNumber());}
}
