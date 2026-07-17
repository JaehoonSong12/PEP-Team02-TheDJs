import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subtask } from '../models/subtask.model';

/**
 * CRUD operations for subtasks nested under a parent task.
 *
 * Uses relative path `/api/todos/{taskId}/subtasks`. The `authInterceptor`
 * prepends the runtime `apiUrl` from `/config.json` when deployed to S3
 * (production). Locally, `apiUrl` is empty and Nginx proxies relative
 * paths to the backend.
 */
@Injectable({ providedIn: 'root' })
export class SubtaskService {
  private readonly http = inject(HttpClient);

  /** Relative API base — infrastructure handles routing to the backend. */
  private readonly apiUrl = '/api/todos';

  getSubtasks(taskId: string): Observable<Subtask[]> {
    return this.http.get<Subtask[]>(`${this.apiUrl}/${taskId}/subtasks`);
  }

  createSubtask(taskId: string, subtask: Partial<Subtask>): Observable<Subtask> {
    return this.http.post<Subtask>(`${this.apiUrl}/${taskId}/subtasks`, subtask);
  }

  updateSubtask(taskId: string, subtaskId: string, updates: Partial<Subtask>): Observable<Subtask> {
    return this.http.put<Subtask>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}`, updates);
  }

  deleteSubtask(taskId: string, subtaskId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}`);
  }
}
