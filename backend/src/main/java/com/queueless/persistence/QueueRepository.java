package com.queueless.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface QueueRepository extends JpaRepository<QueueEntity, Long> {
  boolean existsByPrefixIgnoreCase(String prefix);
  boolean existsByPrefixIgnoreCaseAndIdNot(String prefix, Long id);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select q from QueueEntity q where q.id=:id")
  Optional<QueueEntity> findByIdForUpdate(Long id);
}
