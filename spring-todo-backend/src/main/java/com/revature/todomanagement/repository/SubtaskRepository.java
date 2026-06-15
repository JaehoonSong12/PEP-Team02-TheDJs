package com.revature.todomanagement.repository;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTask(Task task);
}
