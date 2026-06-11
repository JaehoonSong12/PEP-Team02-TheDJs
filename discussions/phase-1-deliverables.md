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
        Response Payload 200:
        {
            "todoId": Integer,
            "accountId": Integer,
            "title": "String",
            "description": "String",
            "completed": boolean,
        }
        
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
    DELETE /api/todos/{id}/{subtask}
        The response body should include the number of rows that were updated, along with a response status of 200.
