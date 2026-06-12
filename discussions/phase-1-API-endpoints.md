1. Account Creation
    
    - REGISTER NEW ACCOUNT
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

    - USER LOGIN
    POST /api/auth/login
    Request body includes a JSON representation of the account. Returns a JSON of the account if successful, returns error message if not

3. Task Management

    - CREATE NEW TASK
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

    - GET LIST OF ALL TASKS 
    GET /api/todos
    Response Payload 200:
    {
        "todoId": Integer,
        "accountId": Integer,
        "title": "String",
        "description": "String",
        "completed": boolean,
    }
    
    - GET SPECIFIC TASK
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

    - UPDATE SPECIFIC TASK
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

    - DELETE SPECIFIC TASK
    DELETE /api/todos/{id}
    Response Status: 204

4. Subtask Organization

    - GET ALL SUBTASKS
    GET /api/todos/{id}
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
    
    - CREATE NEW SUBTASK    
    POST /api/todos/{id}/{subtask}
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

    - UPDATE NEW SUBTASK
    PUT /api/todos/{id}/{subtask}
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

    - DELETE SUBTASK
    DELETE /api/todos/{id}/{subtask}
    Response Status: 204
