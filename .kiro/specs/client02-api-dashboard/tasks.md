# Implementation Plan: client02-api-dashboard

## Overview

This plan delivers the authenticated Dashboard feature for the Angular frontend. Tasks are
ordered bottom-up: shared models and infrastructure (interfaces, services, interceptor, guard)
are created first, then the dashboard component and routing are wired together. Each step
builds on the previous ones.

All code lives under `angular-todo-frontend/src/app/` in the Angular project.

---

## Tasks

- [x] 1. Create TypeScript model interfaces
  - [x] 1.1 Create `models/task.model.ts`
    - Export `Task` interface with fields: `id: string`, `userId: string`, `title: string`, `completed: boolean`
    - _Requirements: 11.1_
  - [x] 1.2 Create `models/subtask.model.ts`
    - Export `Subtask` interface with fields: `id: string`, `taskId: string`, `title: string`, `completed: boolean`
    - _Requirements: 11.2_

- [ ] 2. Create HTTP services
  - [ ] 2.1 Create `services/task.service.ts`
    - Annotate with `@Injectable({ providedIn: 'root' })`
    - Inject `HttpClient` via `inject(HttpClient)`
    - Define `private readonly apiUrl = '/api/todos'`
    - Implement `getTasks(): Observable<Task[]>` — `GET /api/todos`
    - Implement `createTask(task: Partial<Task>): Observable<Task>` — `POST /api/todos`
    - Implement `updateTask(id: string, updates: Partial<Task>): Observable<Task>` — `PUT /api/todos/{id}`
    - Implement `deleteTask(id: string): Observable<void>` — `DELETE /api/todos/{id}`
    - Use typed `HttpClient` generics for all calls
    - _Requirements: 2.1, 3.2, 4.3, 5.2, 11.3_
  - [ ] 2.2 Create `services/subtask.service.ts`
    - Annotate with `@Injectable({ providedIn: 'root' })`
    - Inject `HttpClient` via `inject(HttpClient)`
    - Define `private readonly apiUrl = '/api/todos'`
    - Implement `getSubtasks(taskId: string): Observable<Subtask[]>` — `GET /api/todos/{taskId}/subtasks`
    - Implement `createSubtask(taskId: string, subtask: Partial<Subtask>): Observable<Subtask>` — `POST /api/todos/{taskId}/subtasks`
    - Implement `updateSubtask(taskId: string, subtaskId: string, updates: Partial<Subtask>): Observable<Subtask>` — `PUT /api/todos/{taskId}/subtasks/{subtaskId}`
    - Implement `deleteSubtask(taskId: string, subtaskId: string): Observable<void>` — `DELETE /api/todos/{taskId}/subtasks/{subtaskId}`
    - _Requirements: 6.2, 7.2, 8.2, 9.2, 11.3_

- [ ] 3. Create AuthInterceptor
  - [ ] 3.1 Create `interceptors/auth.interceptor.ts`
    - Export a functional interceptor `authInterceptor: HttpInterceptorFn`
    - Read token from `localStorage.getItem('token')`
    - If token exists, clone request with `Authorization: Bearer <token>` header
    - Pass request to `next()` handler
    - In response pipe: on HTTP 401, remove `token` from localStorage and navigate to `/login` via `Router`
    - If no token, pass request unchanged
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 4. Create AuthGuard
  - [ ] 4.1 Create `guards/auth.guard.ts`
    - Export a functional guard `authGuard: CanActivateFn`
    - Check `localStorage.getItem('token')`
    - If token is present, return `true`
    - If token is absent, inject `Router`, navigate to `/login`, and return `false`
    - _Requirements: 1.2, 1.3_

- [ ] 5. Update application configuration
  - [ ] 5.1 Update `app.config.ts`
    - Add `provideHttpClient(withInterceptors([authInterceptor]))` to providers array
    - Import `provideHttpClient` and `withInterceptors` from `@angular/common/http`
    - Import `authInterceptor` from `./interceptors/auth.interceptor`
    - _Requirements: 10.1_
  - [ ] 5.2 Update `app.routes.ts`
    - Add route `{ path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] }`
    - Add default redirect: `{ path: '', redirectTo: '/login', pathMatch: 'full' }`
    - Add wildcard redirect: `{ path: '**', redirectTo: '/login' }`
    - Import `authGuard` from `./guards/auth.guard`
    - _Requirements: 1.1, 1.2_

- [ ] 6. Create DashboardComponent
  - [ ] 6.1 Create `dashboard/dashboard.component.ts`
    - Standalone component with `selector: 'app-dashboard'`
    - Import `CommonModule` and `FormsModule`
    - Inject `TaskService`, `SubtaskService`, and `Router`
    - Define signals: `tasks`, `errorMessage`, `newTaskTitle`, `expandedTaskIds`, `subtasksByTaskId`
    - Implement `ngOnInit()` to call `TaskService.getTasks()` and populate `tasks` signal
    - _Requirements: 2.1_
  - [ ] 6.2 Implement task creation logic
    - Validate `newTaskTitle` is not blank before sending request
    - Call `TaskService.createTask({title, completed: false})`
    - On success, append the returned task to `tasks` signal
    - On validation failure, set `errorMessage` signal
    - Clear input field on success
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [ ] 6.3 Implement task update logic
    - Provide inline edit mode for task title (toggle via a button or double-click)
    - Provide a checkbox for toggling `completed` status
    - Validate title is not blank before sending
    - Call `TaskService.updateTask(id, updates)` on change
    - On success, update the task in the `tasks` signal
    - Handle 404 by removing task from list
    - Handle 403 by showing error message
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_
  - [ ] 6.4 Implement task deletion logic
    - Provide a delete button per task
    - Call `TaskService.deleteTask(id)` on click
    - On 204 success, remove task from `tasks` signal and clear from `subtasksByTaskId`
    - Handle 404 by removing task from list
    - Handle 403 by showing error message
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  - [ ] 6.5 Implement subtask expand/collapse and listing
    - Provide an expand/collapse toggle per task
    - On expand, call `SubtaskService.getSubtasks(taskId)` and store results in `subtasksByTaskId`
    - Display subtasks beneath the parent task when expanded
    - Show "No subtasks" when array is empty
    - Handle 404 by showing error and refreshing task list
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [ ] 6.6 Implement subtask creation logic
    - When a task is expanded, provide an input field for new subtask title
    - Validate title is not blank before sending
    - Call `SubtaskService.createSubtask(taskId, {title, completed: false})`
    - On success, append subtask to the corresponding entry in `subtasksByTaskId`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - [ ] 6.7 Implement subtask update logic
    - Provide inline edit and completed toggle for each subtask
    - Validate title is not blank before sending
    - Call `SubtaskService.updateSubtask(taskId, subtaskId, updates)`
    - On success, update the subtask in `subtasksByTaskId`
    - Handle 404 by removing subtask from list
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_
  - [ ] 6.8 Implement subtask deletion logic
    - Provide a delete button per subtask
    - Call `SubtaskService.deleteSubtask(taskId, subtaskId)` on click
    - On 204 or 404 success, remove subtask from `subtasksByTaskId`
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  - [ ] 6.9 Implement logout
    - Provide a logout button in the dashboard
    - On click, remove `token` from localStorage
    - Navigate to `/login`
    - _Requirements: 1.5_
  - [ ] 6.10 Handle 401 globally
    - If any service call returns 401 (handled by interceptor), verify the redirect occurs
    - Display no stale data after redirect
    - _Requirements: 2.5, 10.4_

- [ ] 7. Create dashboard template and styles
  - [ ] 7.1 Create `dashboard/dashboard.component.html`
    - Header area with app title and logout button
    - "Create task" form: input field + submit button
    - Task list: iterate over `tasks` signal, render title, completed checkbox, edit/delete buttons
    - Per-task expand toggle and subtask area (conditionally rendered)
    - Subtask list: iterate over subtasks, render title, completed checkbox, edit/delete buttons
    - "Create subtask" input within the expanded section
    - Error message display area
    - Empty state message when no tasks
    - _Requirements: 2.2, 2.3, 3.1, 4.1, 4.2, 5.1, 6.1, 6.3, 7.1, 8.1, 9.1_
  - [ ] 7.2 Create `dashboard/dashboard.component.css`
    - Style task list items with visual separation
    - Style subtask items with indentation to show hierarchy
    - Style completed tasks/subtasks differently (e.g., strikethrough or muted)
    - Style form inputs and buttons
    - Style error messages
    - Ensure basic accessibility (focus states, contrast)

- [ ] 8. Checkpoint — compile and verify
  - Run `npm start` (or `ng serve`) and verify:
    - App redirects to `/login` when no token is present
    - Manually setting a token in localStorage allows access to `/dashboard`
    - Dashboard renders without runtime errors
  - Fix any compilation or runtime errors before proceeding

- [ ] 9. Integration with login page
  - [ ] 9.1 Ensure LoginComponent stores token and navigates to dashboard
    - After successful `POST /api/auth/login`, extract token from `Authorization` response header
    - Store token in `localStorage.setItem('token', token)`
    - Call `router.navigate(['/dashboard'])`
    - _Requirements: 1.4_
  - [ ] 9.2 Verify end-to-end flow
    - Login with valid credentials → token stored → redirected to dashboard → tasks load
    - Invalid credentials → error shown, no redirect
    - Logout → token cleared → redirected to login → cannot access dashboard

---

## Notes

- The LoginComponent implementation is assumed to exist or be built as part of a separate spec (client01). Task 9 ensures it integrates correctly with this dashboard spec.
- All components use Angular 22 standalone component pattern — no NgModules.
- Signals are used for component state; RxJS Observables are used for HTTP calls.
- The `apiUrl` in services uses a relative path (`/api/todos`), which requires an Angular proxy config or deployment behind the same origin as the backend. For local dev, configure `proxy.conf.json` to forward `/api` to `http://localhost:8080`.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "3.1", "4.1"] },
    { "id": 2, "tasks": ["5.1", "5.2"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["6.2", "6.3", "6.4", "6.5", "6.6", "6.7", "6.8", "6.9", "6.10"] },
    { "id": 5, "tasks": ["7.1", "7.2"] },
    { "id": 6, "tasks": ["8"] },
    { "id": 7, "tasks": ["9.1", "9.2"] }
  ]
}
```
