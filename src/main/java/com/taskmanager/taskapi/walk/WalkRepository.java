package com.taskmanager.taskapi.walk;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalkRepository extends JpaRepository<Walk, Long> {
	Optional<Walk> findFirstByStatusInOrderByStartedAtDesc(Collection<WalkStatus> statuses);

	List<Walk> findAllByOrderByStartedAtDesc();
}