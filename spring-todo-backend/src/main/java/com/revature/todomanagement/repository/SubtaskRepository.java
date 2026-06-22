package com.revature.todomanagement.repository;

import com.revature.todomanagement.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubtaskRepository extends JpaRepository<Subtask, UUID> {

    List<Subtask> findAllByTaskId(UUID taskId);
}
