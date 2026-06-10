1. Account Creation
    POST /api/auth/register

2. Authentication
    POST /api/auth/login
        Request body includes a JSON representation of the account. Returns a JSON of the account if successful, returns error message if not

3. Task Management
    POST /api/stodos
    GET /api/todos
    GET /api/todos/{id}
    PUT /api/todos/{id}
    DELETE /api/todos/{id}

4. Subtask Organization
    GET /api/todos/{id}
        Returns a JSON representation of a list containing all the subtasks of that specific id with a response status of 200. If there are no subtasks, the list should be empty
    POST /api/todos/{id}/{subtask}
        The request body should include a text value of the new subtask. Returns a JSON of the subtask that was created with a response status of 200. If the creation was not successful, return response status of 400
    PATCH /api/todos/{id}/{subtask}
        The request body should include a text value with which to update the subtask. Returns a JSON of the subtask that was updated with a response status of 200. If the creation was not successful, return response status of 400
    DELETE /api/todos/{id}/{subtask}
        The response body should include the number of rows that were updated, along with a response status of 200.
