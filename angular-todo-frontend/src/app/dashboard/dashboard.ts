import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { TaskService } from '../services/task.service';
import { SubtaskService } from '../services/subtask.service';
import { AuthService } from '../auth/auth';
import { Task } from '../models/task.model';
import { Subtask } from '../models/subtask.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly subtaskService = inject(SubtaskService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Component state signals
  tasks = signal<Task[]>([]);
  errorMessage = signal<string>('');
  newTaskTitle = signal<string>('');
  expandedTaskIds = signal<Set<string>>(new Set());
  subtasksByTaskId = signal<Map<string, Subtask[]>>(new Map());

  // Inline edit state for tasks
  editingTaskId = signal<string | null>(null);
  editingTaskTitle = signal<string>('');

  // Inline edit state for subtasks
  editingSubtaskId = signal<string | null>(null);
  editingSubtaskTitle = signal<string>('');

  // New subtask title per task
  newSubtaskTitles = signal<Map<string, string>>(new Map());

  ngOnInit(): void {
    this.loadTasks();
  }

  // --- Task CRUD ---

  loadTasks(): void {
    this.taskService.getTasks().subscribe({
      next: (tasks) => this.tasks.set(tasks),
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  createTask(): void {
    const title = this.newTaskTitle().trim();
    if (!title) {
      this.errorMessage.set('Task title is required');
      return;
    }

    this.errorMessage.set('');
    this.taskService.createTask({ title, completed: false }).subscribe({
      next: (task) => {
        this.tasks.update((tasks) => [...tasks, task]);
        this.newTaskTitle.set('');
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  startEditTask(task: Task): void {
    this.editingTaskId.set(task.id);
    this.editingTaskTitle.set(task.title);
  }

  cancelEditTask(): void {
    this.editingTaskId.set(null);
    this.editingTaskTitle.set('');
  }

  saveEditTask(task: Task): void {
    const title = this.editingTaskTitle().trim();
    if (!title) {
      this.errorMessage.set('Task title is required');
      return;
    }

    this.errorMessage.set('');
    this.taskService.updateTask(task.id, { title }).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) =>
          tasks.map((t) => (t.id === updated.id ? updated : t))
        );
        this.editingTaskId.set(null);
        this.editingTaskTitle.set('');
      },
      error: (err: HttpErrorResponse) => this.handleTaskError(err, task.id),
    });
  }

  toggleTaskCompleted(task: Task): void {
    const completed = !task.completed;
    this.taskService.updateTask(task.id, { completed }).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) =>
          tasks.map((t) => (t.id === updated.id ? updated : t))
        );
      },
      error: (err: HttpErrorResponse) => this.handleTaskError(err, task.id),
    });
  }

  deleteTask(taskId: string): void {
    this.taskService.deleteTask(taskId).subscribe({
      next: () => {
        this.tasks.update((tasks) => tasks.filter((t) => t.id !== taskId));
        this.subtasksByTaskId.update((map) => {
          const newMap = new Map(map);
          newMap.delete(taskId);
          return newMap;
        });
        this.expandedTaskIds.update((set) => {
          const newSet = new Set(set);
          newSet.delete(taskId);
          return newSet;
        });
      },
      error: (err: HttpErrorResponse) => this.handleTaskError(err, taskId),
    });
  }

  // --- Subtask expand/collapse ---

  toggleExpand(taskId: string): void {
    const expanded = this.expandedTaskIds();
    if (expanded.has(taskId)) {
      this.expandedTaskIds.update((set) => {
        const newSet = new Set(set);
        newSet.delete(taskId);
        return newSet;
      });
    } else {
      this.expandedTaskIds.update((set) => {
        const newSet = new Set(set);
        newSet.add(taskId);
        return newSet;
      });
      this.loadSubtasks(taskId);
    }
  }

  isExpanded(taskId: string): boolean {
    return this.expandedTaskIds().has(taskId);
  }

  getSubtasks(taskId: string): Subtask[] {
    return this.subtasksByTaskId().get(taskId) || [];
  }

  private loadSubtasks(taskId: string): void {
    this.subtaskService.getSubtasks(taskId).subscribe({
      next: (subtasks) => {
        this.subtasksByTaskId.update((map) => {
          const newMap = new Map(map);
          newMap.set(taskId, subtasks);
          return newMap;
        });
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.errorMessage.set('Task not found. Refreshing list...');
          this.loadTasks();
        } else {
          this.handleError(err);
        }
      },
    });
  }

  // --- Subtask CRUD ---

  getNewSubtaskTitle(taskId: string): string {
    return this.newSubtaskTitles().get(taskId) || '';
  }

  setNewSubtaskTitle(taskId: string, value: string): void {
    this.newSubtaskTitles.update((map) => {
      const newMap = new Map(map);
      newMap.set(taskId, value);
      return newMap;
    });
  }

  createSubtask(taskId: string): void {
    const title = this.getNewSubtaskTitle(taskId).trim();
    if (!title) {
      this.errorMessage.set('Subtask title is required');
      return;
    }

    this.errorMessage.set('');
    this.subtaskService.createSubtask(taskId, { title, completed: false }).subscribe({
      next: (subtask) => {
        this.subtasksByTaskId.update((map) => {
          const newMap = new Map(map);
          const existing = newMap.get(taskId) || [];
          newMap.set(taskId, [...existing, subtask]);
          return newMap;
        });
        this.newSubtaskTitles.update((map) => {
          const newMap = new Map(map);
          newMap.set(taskId, '');
          return newMap;
        });
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  startEditSubtask(subtask: Subtask): void {
    this.editingSubtaskId.set(subtask.id);
    this.editingSubtaskTitle.set(subtask.title);
  }

  cancelEditSubtask(): void {
    this.editingSubtaskId.set(null);
    this.editingSubtaskTitle.set('');
  }

  saveEditSubtask(taskId: string, subtask: Subtask): void {
    const title = this.editingSubtaskTitle().trim();
    if (!title) {
      this.errorMessage.set('Subtask title is required');
      return;
    }

    this.errorMessage.set('');
    this.subtaskService.updateSubtask(taskId, subtask.id, { title }).subscribe({
      next: (updated) => {
        this.subtasksByTaskId.update((map) => {
          const newMap = new Map(map);
          const subtasks = newMap.get(taskId) || [];
          newMap.set(
            taskId,
            subtasks.map((s) => (s.id === updated.id ? updated : s))
          );
          return newMap;
        });
        this.editingSubtaskId.set(null);
        this.editingSubtaskTitle.set('');
      },
      error: (err: HttpErrorResponse) =>
        this.handleSubtaskError(err, taskId, subtask.id),
    });
  }

  toggleSubtaskCompleted(taskId: string, subtask: Subtask): void {
    const completed = !subtask.completed;
    this.subtaskService.updateSubtask(taskId, subtask.id, { completed }).subscribe({
      next: (updated) => {
        this.subtasksByTaskId.update((map) => {
          const newMap = new Map(map);
          const subtasks = newMap.get(taskId) || [];
          newMap.set(
            taskId,
            subtasks.map((s) => (s.id === updated.id ? updated : s))
          );
          return newMap;
        });
      },
      error: (err: HttpErrorResponse) =>
        this.handleSubtaskError(err, taskId, subtask.id),
    });
  }

  deleteSubtask(taskId: string, subtaskId: string): void {
    this.subtaskService.deleteSubtask(taskId, subtaskId).subscribe({
      next: () => {
        this.removeSubtaskFromState(taskId, subtaskId);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.removeSubtaskFromState(taskId, subtaskId);
        } else {
          this.handleError(err);
        }
      },
    });
  }

  // --- Logout ---

  logout(): void {
    this.authService.logout();
  }

  // --- Error handling helpers ---

  private handleError(err: HttpErrorResponse): void {
    if (err.status === 401) {
      // Interceptor handles 401 redirect; clear local state
      this.tasks.set([]);
      return;
    }
    const message =
      err.error?.message || err.message || 'Something went wrong';
    this.errorMessage.set(message);
  }

  private handleTaskError(err: HttpErrorResponse, taskId: string): void {
    if (err.status === 404) {
      this.tasks.update((tasks) => tasks.filter((t) => t.id !== taskId));
      this.errorMessage.set('Task not found and has been removed from the list.');
    } else if (err.status === 403) {
      this.errorMessage.set('Access denied. You do not own this task.');
    } else {
      this.handleError(err);
    }
  }

  private handleSubtaskError(
    err: HttpErrorResponse,
    taskId: string,
    subtaskId: string
  ): void {
    if (err.status === 404) {
      this.removeSubtaskFromState(taskId, subtaskId);
      this.errorMessage.set('Subtask not found and has been removed.');
    } else {
      this.handleError(err);
    }
  }

  private removeSubtaskFromState(taskId: string, subtaskId: string): void {
    this.subtasksByTaskId.update((map) => {
      const newMap = new Map(map);
      const subtasks = newMap.get(taskId) || [];
      newMap.set(
        taskId,
        subtasks.filter((s) => s.id !== subtaskId)
      );
      return newMap;
    });
  }
}
