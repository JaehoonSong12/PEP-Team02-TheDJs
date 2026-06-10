1. Account Creation
    POST /api/auth/register
    Request Payload:
    {
        "username": "String",
        "password": "String"
    }
    Response Payload 200:
    {
        "accountId": Integer,
        "username": "String"
    }

    Response Payload 409:
    {
        "status": 409
    }
    Response Payload 400:
    {
        "status": 400
    }

2. Authentication
    POST /api/auth/login
        Request body includes a JSON representation of the account. Returns a JSON of the account if successful, returns error message if not

3. Task Management
    POST /api/todos
    Request Payload:
    {
        "title": "String",
        "description": "String",
        "completed": boolean;
    }

    Response Payload 200:
    {
        "todoId": Integer,
        "accountId": Integer,
        "title": "String",
        "description": "String",
        "completed": boolean,
    }

    Response Payload 400:
    {
        "status": 400
    }

    GET /api/todos
    Response Payload 200:
    [
        {
            "todoId": Integer,
            "accountId": Integer,
            "title": "String",
            "description": "String",
            "completed": boolean,
        }
    ]
    
    GET /api/todos/{id}
    Response Payload 200:
    {
        "todoId": Integer,
        "accountId": Integer,
        "title": "String",
        "description": "String",
        "completed": boolean,
    }

    Response Payload 404:
    {
        "status": 404
    }

    PUT /api/todos/{id}
    Request Payload:
    {
        "title": "String",
        "description": "String",
        "completed": boolean;     
    }

    Response Payload 200:
    {
        "todoId": Integer,
        "accountId": Integer,
        "title": "String",
        "description": "String",
        "completed": boolean,
    }

    Response Payload 404:
    {
        "status": 404
    }

    DELETE /api/todos/{id}

4. Subtask Organization
    GET /api/todos/{id}
        Returns a JSON representation of a list containing all the subtasks of that specific id with a response status of 200. If there are no subtasks, the list should be empty
    POST /api/todos/{id}/{subtask}
        The request body should include a text value of the new subtask. Returns a JSON of the subtask that was created with a response status of 200. If the creation was not successful, return response status of 400
    PATCH /api/todos/{id}/{subtask}
        The request body should include a text value with which to update the subtask. Returns a JSON of the number of rows that were updated with a response status of 200. If the creation was not successful, return response status of 400
    DELETE /api/todos/{id}/{subtask}
        The response body should include the number of rows that were updated, along with a response status of 200.
