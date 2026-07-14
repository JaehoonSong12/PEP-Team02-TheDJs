package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.repository.SubtaskRepository;
import com.revature.todomanagement.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    SubtaskRepository subtaskRepository;

    @InjectMocks
    TaskService taskService;

    // ------------------------------------------------------------------ //
    //  createTask                                                          //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("valid title sets userId and saves")
        void createTask_validTitle_setsUserIdAndSaves() {
            UUID userId = UUID.randomUUID();
            Task task = new Task(null, null, "Buy groceries", false);
            Task savedTask = new Task(UUID.randomUUID(), userId, "Buy groceries", false);

            when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

            Task result = taskService.createTask(userId, task);

            assertEquals(userId, task.getUserId());
            assertNull(task.getId());
            verify(taskRepository).save(task);
            assertEquals(savedTask, result);
        }

        @Test
        @DisplayName("null title throws IllegalArgumentException and no save")
        void createTask_nullTitle_throwsIllegalArgAndNoSave() {
            UUID userId = UUID.randomUUID();
            Task task = new Task(null, null, null, false);

            assertThrows(IllegalArgumentException.class, () -> taskService.createTask(userId, task));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("blank title throws IllegalArgumentException and no save")
        void createTask_blankTitle_throwsIllegalArgAndNoSave() {
            UUID userId = UUID.randomUUID();
            Task task = new Task(null, null, "   ", false);

            assertThrows(IllegalArgumentException.class, () -> taskService.createTask(userId, task));
            verify(taskRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  getTasksForUser                                                     //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getTasksForUser")
    class GetTasksForUser {

        @Test
        @DisplayName("delegates to repository")
        void getTasksForUser_delegatesToRepository() {
            UUID userId = UUID.randomUUID();
            List<Task> expected = List.of(
                    new Task(UUID.randomUUID(), userId, "Task 1", false),
                    new Task(UUID.randomUUID(), userId, "Task 2", true)
            );

            when(taskRepository.findAllByUserId(userId)).thenReturn(expected);

            List<Task> result = taskService.getTasksForUser(userId);

            assertEquals(expected, result);
            verify(taskRepository).findAllByUserId(userId);
        }
    }

    // ------------------------------------------------------------------ //
    //  getTaskById                                                         //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("owned task returns the task")
        void getTaskById_ownedTask_returnsTask() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task task = new Task(taskId, userId, "My Task", false);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            Task result = taskService.getTaskById(userId, taskId);

            assertEquals(task, result);
            verify(taskRepository).findById(taskId);
        }

        @Test
        @DisplayName("non-existent task throws TaskNotFoundException")
        void getTaskById_nonExistentTask_throwsTaskNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();

            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(userId, taskId));
        }

        @Test
        @DisplayName("wrong owner throws TaskOwnershipException")
        void getTaskById_wrongOwner_throwsTaskOwnershipException() {
            UUID requestingUserId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task task = new Task(taskId, ownerUserId, "Someone else's task", false);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            assertThrows(TaskOwnershipException.class, () -> taskService.getTaskById(requestingUserId, taskId));
        }
    }

    // ------------------------------------------------------------------ //
    //  updateTask                                                          //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("valid title updates and saves")
        void updateTask_validTitle_updatesAndSaves() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task existing = new Task(taskId, userId, "Old Title", false);
            Task updates = new Task(null, null, "New Title", null);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
            when(taskRepository.save(existing)).thenReturn(existing);

            Task result = taskService.updateTask(userId, taskId, updates);

            assertEquals("New Title", result.getTitle());
            verify(taskRepository).save(existing);
        }

        @Test
        @DisplayName("blank title throws IllegalArgumentException and no save")
        void updateTask_blankTitle_throwsIllegalArgAndNoSave() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task existing = new Task(taskId, userId, "Original", false);
            Task updates = new Task(null, null, "   ", null);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(userId, taskId, updates));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("null title preserves original title")
        void updateTask_nullTitle_preservesOriginalTitle() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task existing = new Task(taskId, userId, "Original Title", false);
            Task updates = new Task(null, null, null, null);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
            when(taskRepository.save(existing)).thenReturn(existing);

            Task result = taskService.updateTask(userId, taskId, updates);

            assertEquals("Original Title", result.getTitle());
            verify(taskRepository).save(existing);
        }

        @Test
        @DisplayName("wrong owner throws TaskOwnershipException")
        void updateTask_wrongOwner_throwsTaskOwnershipException() {
            UUID requestingUserId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task existing = new Task(taskId, ownerUserId, "Not yours", false);
            Task updates = new Task(null, null, "Hacked", null);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));

            assertThrows(TaskOwnershipException.class, () -> taskService.updateTask(requestingUserId, taskId, updates));
            verify(taskRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  deleteTask                                                          //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("owned task deletes subtasks then task in order")
        void deleteTask_ownedTask_deletesSubtasksThenTask() {
            UUID userId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task task = new Task(taskId, userId, "To Delete", false);
            List<Subtask> subtasks = List.of(
                    new Subtask(UUID.randomUUID(), taskId, "Sub 1", false),
                    new Subtask(UUID.randomUUID(), taskId, "Sub 2", false)
            );

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(subtaskRepository.findAllByTaskId(taskId)).thenReturn(subtasks);

            taskService.deleteTask(userId, taskId);

            InOrder inOrder = inOrder(subtaskRepository, taskRepository);
            inOrder.verify(subtaskRepository).findAllByTaskId(taskId);
            inOrder.verify(subtaskRepository).deleteAll(subtasks);
            inOrder.verify(taskRepository).delete(task);
        }

        @Test
        @DisplayName("wrong owner throws TaskOwnershipException and no deletes")
        void deleteTask_wrongOwner_throwsTaskOwnershipExceptionAndNoDeletes() {
            UUID requestingUserId = UUID.randomUUID();
            UUID ownerUserId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Task task = new Task(taskId, ownerUserId, "Not yours", false);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            assertThrows(TaskOwnershipException.class, () -> taskService.deleteTask(requestingUserId, taskId));
            verify(subtaskRepository, never()).findAllByTaskId(any());
            verify(subtaskRepository, never()).deleteAll(anyList());
            verify(taskRepository, never()).delete(any());
        }
    }
}
