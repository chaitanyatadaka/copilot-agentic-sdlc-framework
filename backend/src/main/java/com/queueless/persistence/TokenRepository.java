package com.queueless.persistence;

import com.queueless.domain.TokenStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
  long countByQueueIdAndStatus(Long queueId, TokenStatus status);
  @Query("select t from TokenEntity t where t.queue.id=:queueId and t.status=:status order by t.sequenceNumber asc, t.id asc")
  List<TokenEntity> findByQueueAndStatus(Long queueId, TokenStatus status);
  @Query("select t from TokenEntity t where t.queue.id=:queueId and t.status=:status order by t.sequenceNumber asc, t.id asc")
  List<TokenEntity> waitingForQueue(Long queueId, TokenStatus status);
  @Query("select t from TokenEntity t where t.queue.id=:queueId and t.status='WAITING' and lower(trim(t.customerName))=:name")
  List<TokenEntity> activeByName(Long queueId, String name);
  @Query("select t from TokenEntity t where t.queue.id=:queueId and t.status=:status and (t.sequenceNumber < :sequence or (t.sequenceNumber=:sequence and t.id<:id))")
  List<TokenEntity> precedingWaiting(Long queueId, TokenStatus status, int sequence, Long id);
  @Query("select t from TokenEntity t where t.queue.id=:queueId and t.status='WAITING' and t.customerName=:name")
  List<TokenEntity> duplicateByName(Long queueId, String name);
  Optional<TokenEntity> findById(Long id);
}
