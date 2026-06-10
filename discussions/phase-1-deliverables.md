1. Account Creation
    POST /api/auth/register

2. Authentication
    POST /api/auth/login

3. Task Management
    POST /api/stodos
    GET /api/todos
    GET /api/todos/{id}
    PUT /api/todos/{id}
    DELETE /api/todos/{id}

4. Subtask Organization
    GET /api/todos/{id}
    POST /api/todos/{id}/{subtask}
    PATCH /api/todos/{id}/{subtask}
    DELETE /api/todos/{id}/{subtask}