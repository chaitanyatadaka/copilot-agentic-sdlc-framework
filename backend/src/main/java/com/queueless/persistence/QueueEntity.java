package com.queueless.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity @Table(name="queues", uniqueConstraints=@UniqueConstraint(name="uk_queue_prefix", columnNames="prefix"))
public class QueueEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String name;
  @Column(nullable=false) private String prefix;
  @Column(name="average_service_duration_minutes", nullable=false) private int averageServiceDurationMinutes;
  @Column(nullable=false) private boolean open;
  @Column(name="numbering_day", nullable=false) private LocalDate numberingDay;
  @Column(name="last_sequence_number", nullable=false) private int lastSequenceNumber;
  @Column(nullable=false) private Instant createdAt = Instant.now();
  @Column(nullable=false) private Instant updatedAt = Instant.now();

  protected QueueEntity() {}
  public QueueEntity(String name,String prefix,int duration,boolean open,LocalDate day) {
    this.name=name; this.prefix=prefix; this.averageServiceDurationMinutes=duration; this.open=open;
    this.numberingDay=day; this.lastSequenceNumber=0;
  }
  @PreUpdate void touch(){ updatedAt=Instant.now(); }
  public Long getId(){return id;} public String getName(){return name;} public String getPrefix(){return prefix;}
  public int getAverageServiceDurationMinutes(){return averageServiceDurationMinutes;} public boolean isOpen(){return open;}
  public LocalDate getNumberingDay(){return numberingDay;} public int getLastSequenceNumber(){return lastSequenceNumber;}
  public void setName(String v){name=v;} public void setPrefix(String v){prefix=v;}
  public void setAverageServiceDurationMinutes(int v){averageServiceDurationMinutes=v;} public void setOpen(boolean v){open=v;}
  public void resetNumbering(LocalDate day){numberingDay=day;lastSequenceNumber=0;}
  public void incrementSequence(){lastSequenceNumber++;}
}
