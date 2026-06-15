package com.revature.todomanagement.repository;

import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
}
