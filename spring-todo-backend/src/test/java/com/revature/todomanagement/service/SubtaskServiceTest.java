package com.revature.todomanagement.service;

import com.revature.todomanagement.entity.Subtask;
import com.revature.todomanagement.entity.Task;
import com.revature.todomanagement.exception.SubtaskNotFoundException;
import com.revature.todomanagement.exception.TaskNotFoundException;
import com.revature.todomanagement.exception.TaskOwnershipException;
import com.revature.todomanagement.repository.SubtaskRepository;
import com.revature.todomanagement.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private SubtaskService subtaskService;

    // ---- 3.1 Create subtask — valid title ----

    @Test
    void createSubtask_validTitle_savesAndReturnsSubtask() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask input = new Subtask(null, null, "Write unit tests", false);
        Subtask saved = new Subtask(subtaskId, taskId, "Write unit tests", false);
        when(subtaskRepository.save(any(Subtask.class))).thenReturn(saved);

        Subtask result = subtaskService.createSubtask(userId, taskId, input);

        verify(subtaskRepository, times(1)).save(any(Subtask.class));
        assertEquals(taskId, result.getTaskId());
        assertEquals(subtaskId, result.getId());
    }

    // ---- 3.2 Create subtask — blank title ----

    @Test
    void createSubtask_blankTitle_throwsIllegalArgumentException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask input = new Subtask(null, null, "   ", false);

        assertThrows(IllegalArgumentException.class,
                () -> subtaskService.createSubtask(userId, taskId, input));

        verify(subtaskRepository, never()).save(any(Subtask.class));
    }

    // ---- 3.3 Create subtask — unknown parent task ----

    @Test
    void createSubtask_unknownParentTask_throwsTaskNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        Subtask input = new Subtask(null, null, "Some subtask", false);

        assertThrows(TaskNotFoundException.class,
                () -> subtaskService.createSubtask(userId, taskId, input));
    }

    // ---- 3.4 Create subtask — wrong owner ----

    @Test
    void createSubtask_wrongOwner_throwsTaskOwnershipException() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Task task = new Task(taskId, otherUserId, "Someone else's task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask input = new Subtask(null, null, "Some subtask", false);

        assertThrows(TaskOwnershipException.class,
                () -> subtaskService.createSubtask(userId, taskId, input));
    }

    // ---- 3.5 List subtasks ----

    @Test
    void getSubtasksForTask_returnsListFromRepository() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        List<Subtask> expected = List.of(
                new Subtask(UUID.randomUUID(), taskId, "Sub 1", false),
                new Subtask(UUID.randomUUID(), taskId, "Sub 2", true)
        );
        when(subtaskRepository.findAllByTaskId(taskId)).thenReturn(expected);

        List<Subtask> result = subtaskService.getSubtasksForTask(userId, taskId);

        assertEquals(expected, result);
    }

    // ---- 3.6 Get subtask by ID — not found ----

    @Test
    void getSubtaskById_notFound_throwsSubtaskNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(subtaskRepository.findById(subtaskId)).thenReturn(Optional.empty());

        assertThrows(SubtaskNotFoundException.class,
                () -> subtaskService.getSubtaskById(userId, taskId, subtaskId));
    }

    // ---- 3.7 Get subtask by ID — wrong parent task ----

    @Test
    void getSubtaskById_wrongParentTask_throwsSubtaskNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID otherTaskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask subtask = new Subtask(subtaskId, otherTaskId, "Wrong parent", false);
        when(subtaskRepository.findById(subtaskId)).thenReturn(Optional.of(subtask));

        assertThrows(SubtaskNotFoundException.class,
                () -> subtaskService.getSubtaskById(userId, taskId, subtaskId));
    }

    // ---- 3.8 Update subtask — valid fields ----

    @Test
    void updateSubtask_validFields_savesUpdatedSubtask() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask existing = new Subtask(subtaskId, taskId, "Old title", false);
        when(subtaskRepository.findById(subtaskId)).thenReturn(Optional.of(existing));

        Subtask updates = new Subtask(null, null, "New title", true);
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        Subtask result = subtaskService.updateSubtask(userId, taskId, subtaskId, updates);

        verify(subtaskRepository, times(1)).save(any(Subtask.class));
        assertEquals("New title", result.getTitle());
        assertTrue(result.isCompleted());
    }

    // ---- 3.9 Update subtask — blank title ----

    @Test
    void updateSubtask_blankTitle_throwsIllegalArgumentException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask existing = new Subtask(subtaskId, taskId, "Existing title", false);
        when(subtaskRepository.findById(subtaskId)).thenReturn(Optional.of(existing));

        Subtask updates = new Subtask(null, null, "   ", false);

        assertThrows(IllegalArgumentException.class,
                () -> subtaskService.updateSubtask(userId, taskId, subtaskId, updates));

        verify(subtaskRepository, never()).save(any(Subtask.class));
    }

    // ---- 3.10 Delete subtask — success ----

    @Test
    void deleteSubtask_success_deletesSubtask() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, userId, "Parent Task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Subtask subtask = new Subtask(subtaskId, taskId, "Subtask to delete", false);
        when(subtaskRepository.findById(subtaskId)).thenReturn(Optional.of(subtask));

        subtaskService.deleteSubtask(userId, taskId, subtaskId);

        verify(subtaskRepository, times(1)).delete(subtask);
    }

    // ---- 3.11 Delete subtask — unknown parent task ----

    @Test
    void deleteSubtask_unknownParentTask_throwsTaskNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> subtaskService.deleteSubtask(userId, taskId, subtaskId));
    }

    // ---- 3.12 Delete subtask — wrong owner ----

    @Test
    void deleteSubtask_wrongOwner_throwsTaskOwnershipException() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subtaskId = UUID.randomUUID();

        Task task = new Task(taskId, otherUserId, "Someone else's task", false);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(TaskOwnershipException.class,
                () -> subtaskService.deleteSubtask(userId, taskId, subtaskId));
    }
}
