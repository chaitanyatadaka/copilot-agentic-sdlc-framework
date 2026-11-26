package com.queueless.persistence;

import com.queueless.domain.TokenStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name="tokens",
 uniqueConstraints=@UniqueConstraint(name="uk_queue_day_sequence", columnNames={"queue_id","numbering_day","sequence_number"}),
 indexes={
  @Index(name="idx_queue_status_sequence", columnList="queue_id,status,sequence_number"),
  @Index(name="idx_queue_status", columnList="queue_id,status"),
  @Index(name="idx_queue_day_sequence", columnList="queue_id,numbering_day,sequence_number")
 })
public class TokenEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="queue_id", nullable=false) private QueueEntity queue;
  @Column(name="numbering_day",nullable=false) private LocalDate numberingDay;
  @Column(name="sequence_number",nullable=false) private int sequenceNumber;
  @Column(name="display_identifier",nullable=false) private String displayIdentifier;
  @Column(name="customer_name",nullable=false) private String customerName;
  @Column private String phone;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private TokenStatus status;
  @Column(nullable=false) private Instant joinedAt;
  private Instant calledAt, completedAt, skippedAt, cancelledAt;
  @Version private long version;

  protected TokenEntity(){}
  public TokenEntity(QueueEntity queue,LocalDate day,int sequence,String identifier,String name,String phone) {
    this.queue=queue; this.numberingDay=day; this.sequenceNumber=sequence; this.displayIdentifier=identifier;
    this.customerName=name; this.phone=phone; this.status=TokenStatus.WAITING; this.joinedAt=Instant.now();
  }
  public Long getId(){return id;} public QueueEntity getQueue(){return queue;} public int getSequenceNumber(){return sequenceNumber;}
  public String getDisplayIdentifier(){return displayIdentifier;} public String getCustomerName(){return customerName;}
  public String getPhone(){return phone;} public TokenStatus getStatus(){return status;} public Instant getJoinedAt(){return joinedAt;}
  public void setStatus(TokenStatus status,Instant now){
    this.status=status;
    if(status==TokenStatus.CALLED) calledAt=now;
    if(status==TokenStatus.COMPLETED) completedAt=now;
    if(status==TokenStatus.SKIPPED) skippedAt=now;
    if(status==TokenStatus.CANCELLED) cancelledAt=now;
  }
}
