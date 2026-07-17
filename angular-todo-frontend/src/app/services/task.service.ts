import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task } from '../models/task.model';

/**
 * CRUD operations for top-level tasks.
 *
 * Uses relative path `/api/todos`. The `authInterceptor` prepends the
 * runtime `apiUrl` from `/config.json` when deployed to S3 (production).
 * Locally, `apiUrl` is empty and Nginx proxies relative paths to the backend.
 */
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);

  /** Relative API base — infrastructure handles routing to the backend. */
  private readonly apiUrl = '/api/todos';

  getTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.apiUrl);
  }

  createTask(task: Partial<Task>): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, task);
  }

  updateTask(id: string, updates: Partial<Task>): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, updates);
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
