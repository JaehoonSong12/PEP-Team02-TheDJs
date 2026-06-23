# Spring Boot Web Reference Guide

## Spring MVC Overview

Spring MVC (Model-View-Controller) is a powerful web framework built on the core principles of Spring. It provides a clean separation of concerns by dividing web applications into three interconnected components:

- **Model:** Represents the application’s data and business logic.
- **View:** Handles the presentation layer, rendering the model data (e.g., as HTML, JSON).
- **Controller:** Processes incoming HTTP requests, interacts with the model, and selects the appropriate view for response.

Spring MVC streamlines web development by handling request routing, data binding, validation, and response rendering. It integrates seamlessly with other Spring modules and supports both traditional server-side rendering and RESTful APIs.

Key benefits include:
- **Flexible architecture:** Easily adapt to different web application styles (monolithic, REST, microservices).
- **Annotation-driven configuration:** Reduces boilerplate and keeps configuration close to the code.
- **Extensibility:** Customize request handling, data binding, and view resolution as needed.

Spring MVC is the foundation for building robust, maintainable web applications in the Spring ecosystem.

## Controllers

Controllers are the core components in Spring MVC responsible for handling HTTP requests and returning responses. Spring simplifies controller development through a set of powerful annotations:

- **@Controller:** Marks a class as a web controller, allowing Spring to detect and register it for request handling.
- **@RestController:** A specialized version of `@Controller` that combines `@Controller` and `@ResponseBody`, making it ideal for RESTful APIs by automatically serializing return values to JSON or XML.

Within controllers, you define handler methods that process specific requests. These methods are annotated to map URLs, extract parameters, and define response behavior. Controllers promote clean separation between request processing and business logic, making your codebase easier to test and maintain.

Best practices:
- Keep controllers focused on request handling and delegate business logic to service classes.
- Use clear, descriptive method names and annotations to improve readability.

## Exposing Endpoints

Spring MVC makes it straightforward to expose HTTP endpoints and handle various aspects of web requests and responses:

- **@RequestMapping:** The foundational annotation for mapping HTTP requests to handler methods. You can specify the URL path, HTTP method (GET, POST, etc.), and other attributes.
- **@GetMapping, @PostMapping, etc.:** Shortcut annotations for common HTTP methods, improving clarity and reducing boilerplate.
- **@ResponseBody:** Indicates that the return value of a method should be written directly to the HTTP response body (commonly used for REST APIs).
- **@RequestParam:** Binds HTTP request parameters (e.g., query parameters) to method arguments.
- **@PathVariable:** Extracts values from URI path segments and binds them to method parameters.
- **ResponseEntity:** A flexible way to build HTTP responses, allowing you to set status codes, headers, and body content explicitly.
- **Status Codes:** You can control HTTP status codes using `ResponseEntity`, the `@ResponseStatus` annotation, or by returning appropriate values from controller methods.

These features enable you to build expressive, well-structured APIs and web endpoints with minimal configuration.

### Example: Defining an Endpoint

By using `@PostMapping` and `@RequestBody`, we instruct Spring to bind incoming HTTP POST payloads directly to our domain objects (in this case, `User`). We leverage `ResponseEntity` to explicitly return the `201 Created` status code. 

[UserController.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/controller/UserController.java#L42-L46)
```java
    @PostMapping("/register")
    public ResponseEntity<Void> registerNewUser(@RequestBody User user){
        userService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }
```

## Exception Handling

Robust exception handling is essential for building reliable web applications. Spring MVC provides several mechanisms to manage errors gracefully:

- **@ExceptionHandler:** Annotate methods in your controller (or a dedicated `@ControllerAdvice` class) to handle specific exceptions and return custom responses.
- **@ControllerAdvice / @RestControllerAdvice:** A global exception handler that applies to multiple controllers. `@RestControllerAdvice` is specifically designed for REST APIs, combining `@ControllerAdvice` and `@ResponseBody` to automatically serialize errors to JSON.
- **ResponseEntityExceptionHandler:** Extend this base class to customize the handling of standard Spring MVC exceptions.
- **Custom error responses:** Return meaningful error messages and appropriate HTTP status codes to clients, improving API usability.

By leveraging these tools, you can ensure consistent error handling across your application, provide helpful feedback to users, and maintain clean separation between error management and business logic.

### Example: Controller-Specific Exception Handling

When a custom Java exception (like `RegistrationFailure`) is thrown by the service layer, a controller can intercept it and translate it into an HTTP response using `@ExceptionHandler`. Note that this specific handler is not global; it will only catch exceptions originating from `UserController`.

[UserController.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/controller/UserController.java#L55-L62)
```java
    /*
        NOTE: this exception handler is NOT universal: it will only trigger for any RegistrationFailiure
        exceptions triggered by UserController code
    */
    @ExceptionHandler(RegistrationFailure.class)
    public ResponseEntity<String> handleRegistrationFailure(RegistrationFailure exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
```

### Global CORS Configuration

To allow frontend applications (such as our Angular application in `angular3-demo`) running on different domains to interact with our API (which is blocked by default browser security policies), we map global Cross-Origin Resource Sharing (CORS) rules using `WebMvcConfigurer`. The allowed origins are injected from the application properties.

[application.properties](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/resources/application.properties#L27-L29)
```properties
# CORS Configuration
# Note: Set the environment variable CORS_ALLOWED_ORIGINS to override this fallback in production.
cors.allowed-origins=http://localhost:4200
```

[WebConfig.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/utility/WebConfig.java#L36-L43)
```java
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply CORS rules to all endpoints in the API
                .allowedOrigins(allowedOrigins) // Only allow requests from our frontend
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // HTTP methods the frontend can use
                .allowedHeaders("*") // Allow any headers (e.g. Authorization, Content-Type)
                .allowCredentials(true); // Allow cookies/auth headers to be sent with requests
    }
```

> [!NOTE]
> Verified against official Spring MVC documentation: [@RestControllerAdvice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html) and [Global CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html#mvc-cors-global).
