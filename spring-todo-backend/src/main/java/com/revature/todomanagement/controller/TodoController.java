package com.revature.todomanagement.controller;

import com.revature.todomanagement.dto.SubtaskRequest;
import com.revature.todomanagement.dto.SubtaskResponse;
import com.revature.todomanagement.dto.TodoRequest;
import com.revature.todomanagement.dto.TodoResponse;
import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.entity.User;
import com.revature.todomanagement.service.SubtaskService;
import com.revature.todomanagement.service.TaskService;
import com.revature.todomanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TaskService taskService;
    private final SubtaskService subtaskService;
    private final UserService userService;

    // Helper to get authenticated user (Mocking for now as per phase 1 focus)
    private User getAuthenticatedUser() {
        // In a real app, this would come from SecurityContext
        // For testing/initial setup, we might need a way to pass user id
        return userService.findById(1L); 
    }

    @GetMapping
    public ResponseEntity<List<TodoResponse>> getAllTodos() {
        User user = getAuthenticatedUser();
        List<TodoResponse> responses = taskService.getAllTasks(user).stream()
                .map(this::mapToTodoResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@RequestBody TodoRequest todoRequest) {
        User user = getAuthenticatedUser();
        Task task = Task.builder()
                .title(todoRequest.getTitle())
                .description(todoRequest.getDescription())
                .completed(todoRequest.isCompleted())
                .build();
        Task createdTask = taskService.createTask(task, user);
        return ResponseEntity.ok(mapToTodoResponse(createdTask));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodoById(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        Task task = taskService.getTaskById(id, user);
        return ResponseEntity.ok(mapToTodoResponse(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(@PathVariable Long id, @RequestBody TodoRequest todoRequest) {
        User user = getAuthenticatedUser();
        Task taskDetails = Task.builder()
                .title(todoRequest.getTitle())
                .description(todoRequest.getDescription())
                .completed(todoRequest.isCompleted())
                .build();
        Task updatedTask = taskService.updateTask(id, taskDetails, user);
        return ResponseEntity.ok(mapToTodoResponse(updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subtask")
    public ResponseEntity<SubtaskResponse> createSubtask(@PathVariable Long id, @RequestBody SubtaskRequest subtaskRequest) {
        User user = getAuthenticatedUser();
        Subtask subtask = Subtask.builder()
                .title(subtaskRequest.getTitle())
                .completed(subtaskRequest.isCompleted())
                .build();
        Subtask createdSubtask = subtaskService.createSubtask(id, subtask, user);
        return ResponseEntity.ok(mapToSubtaskResponse(createdSubtask));
    }

    @PutMapping("/{id}/{subtaskId}")
    public ResponseEntity<SubtaskResponse> updateSubtask(@PathVariable Long id, @PathVariable Long subtaskId, @RequestBody SubtaskRequest subtaskRequest) {
        User user = getAuthenticatedUser();
        Subtask subtaskDetails = Subtask.builder()
                .title(subtaskRequest.getTitle())
                .completed(subtaskRequest.isCompleted())
                .build();
        Subtask updatedSubtask = subtaskService.updateSubtask(id, subtaskId, subtaskDetails, user);
        return ResponseEntity.ok(mapToSubtaskResponse(updatedSubtask));
    }

    @DeleteMapping("/{id}/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable Long id, @PathVariable Long subtaskId) {
        User user = getAuthenticatedUser();
        subtaskService.deleteSubtask(id, subtaskId, user);
        return ResponseEntity.noContent().build();
    }

    private TodoResponse mapToTodoResponse(Task task) {
        return TodoResponse.builder()
                .todoId(task.getId())
                .accountId(task.getUser().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .build();
    }

    private SubtaskResponse mapToSubtaskResponse(Subtask subtask) {
        return SubtaskResponse.builder()
                .id(subtask.getId())
                .todoId(subtask.getTask().getId())
                .title(subtask.getTitle())
                .completed(subtask.isCompleted())
                .build();
    }
}
