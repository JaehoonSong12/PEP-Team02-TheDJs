Use these stories as the foundation for your task breakdown and API design:

    1. Account Creation: As a new user, I can register an account to start tracking my todo tasks.
       1. Define User entity fields (id, username, email, password) (SQLite, creating a table called User)
       2. Encrypt passwords 
       3. Define validation rules
       4. Register endpoints e.g. POST /api/auth/register
       5. Document error responses for duplicate usernames (HTTP 409)
       6. Return general error responses on any issues (HTTP 400)
       7. Return OK (HTTP 200) if valid
    2. Authentication: As a new user, I can log in and out to securely access my todo items.
        1. Check the database for username/password combination
        2. Register endpoints e.g. POST /api/auth/login
        3. Return OK (HTTP 200) if request body with JSON matches with the database 
        4. 
    3. Task Management: As a user, I can create, edit, and delete todo items to keep track of my work.
        1. Specify POST /api/todos, 
        2. Specify GET /api/todos         - list todos for authenticated user.
        3. Specify GET /api/todos/{id}    - retrieve single todo.
        4. Specify PUT /api/todos/{id}        - update todo fields.
        5. Specify DELETE /api/todos/{id} - delete todo (cascade subtasks).
        6. Users may only access their own todos (ownership)
    4. Subtask Organization: As a user, I can create, edit, and delete subtask items to better organize my primary tasks.
        1. Define Subtask entity (id, title, completed, todo_id, timestamps)
        2. GET /api/todos/{id}
        3. POST /api/todos/{id}/{subtask}
        4. PATCH /api/todos/{id}/{subtask}
        5. DELETE /api/todos/{id}/{subtask}
        6. Document cascade delete when parent todo is removed.
