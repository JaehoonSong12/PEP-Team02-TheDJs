package com.revature.todomanagement.controller;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.exception.SubtaskNotFoundException;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.service.SubtaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/todos/{id}/subtasks")
@RequiredArgsConstructor
public class SubtaskController {

    private final SubtaskService subtaskService;

    // ------------------------------------------------------------------ //
    //  Endpoints                                                           //
    // ------------------------------------------------------------------ //

    @GetMapping
    public ResponseEntity<List<Subtask>> getSubtasks(@RequestAttribute UUID userId,
                                                     @PathVariable UUID id) {
        List<Subtask> subtasks = subtaskService.getSubtasksForTask(userId, id);
        return ResponseEntity.ok(subtasks);
    }

    @PostMapping
    public ResponseEntity<Subtask> createSubtask(@RequestAttribute UUID userId,
                                                 @PathVariable UUID id,
                                                 @RequestBody Subtask subtask) {
        Subtask savedSubtask = subtaskService.createSubtask(userId, id, subtask);
        return ResponseEntity.ok(savedSubtask);
    }

    @GetMapping("/{subtaskId}")
    public ResponseEntity<Subtask> getSubtaskById(@RequestAttribute UUID userId,
                                                  @PathVariable UUID id,
                                                  @PathVariable UUID subtaskId) {
        Subtask subtask = subtaskService.getSubtaskById(userId, id, subtaskId);
        return ResponseEntity.ok(subtask);
    }

    @PutMapping("/{subtaskId}")
    public ResponseEntity<Subtask> updateSubtask(@RequestAttribute UUID userId,
                                                 @PathVariable UUID id,
                                                 @PathVariable UUID subtaskId,
                                                 @RequestBody Subtask updates) {
        Subtask updatedSubtask = subtaskService.updateSubtask(userId, id, subtaskId, updates);
        return ResponseEntity.ok(updatedSubtask);
    }

    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@RequestAttribute UUID userId,
                                              @PathVariable UUID id,
                                              @PathVariable UUID subtaskId) {
        subtaskService.deleteSubtask(userId, id, subtaskId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ //
    //  Exception Handlers                                                  //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(SubtaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubtaskNotFound(SubtaskNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("status", 404, "message", ex.getMessage()));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("status", 404, "message", ex.getMessage()));
    }

    @ExceptionHandler(TaskOwnershipException.class)
    public ResponseEntity<Map<String, Object>> handleTaskOwnership(TaskOwnershipException ex) {
        return ResponseEntity.status(403)
                .body(Map.of("status", 403, "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400)
                .body(Map.of("status", 400, "message", ex.getMessage()));
    }
}
