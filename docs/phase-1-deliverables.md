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

    Response Payload 400:

2. Authentication
    POST /api/auth/login

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


    DELETE /api/todos/{id}

    Response Payload 200:
    {
        "message": "String"
    }

4. Subtask Organization
    GET /api/todos/{id}
    POST /api/todos/{id}/{subtask}
    PATCH /api/todos/{id}/{subtask}
    DELETE /api/todos/{id}/{subtask}