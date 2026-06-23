# Topic 07: Security & JWT Authentication

## Core Concepts

JSON Web Tokens (JWT) are an open, industry-standard method for representing claims securely between two parties. In Spring Boot applications, they are heavily used for stateless authentication and authorization.

## Implementation Context

### JWT Configuration and Handling

Before implementing JWT logic, the necessary libraries for generating and parsing tokens must be imported:

[build.gradle.kts](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/build.gradle.kts#L80-L82)
```kotlin
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
```

We also configure the secret key used for signing the tokens within the application properties:

[application.properties](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/resources/application.properties#L22-L25)
```properties
# JWT Configuration
# Note: Spring Boot prioritizes OS environment variables. 
# Set the environment variable JWT_SECRET to override this fallback in production.
jwt.secret=this-works-for-dev-use-environemnt-in-prod
```

To generate, sign, and parse JWTs, we utilize the `jjwt` library. The `JwtUtility` class encapsulates this logic, pulling a cryptographic secret from the application properties.

- **Claim Key:** An internal label (`username`) used to store the user's username inside the token payload.
- **Property Injection:** Instead of hardcoding the secret, we use `@Value` to pull it from `application.properties` or environment variables, ensuring the secret remains secure and environment-specific.
- **Token Generation:** The `generateToken` method uses the secret key to encrypt the token, setting the subject (the user's ID), custom claims (the username), issuance date, and a 24-hour expiration date.

[JwtUtility.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/utility/JwtUtility.java#L21-L47)
```java
@Component
public class JwtUtility {
    private static final String USERNAME_CLAIM = "username";

    private final SecretKey key;

    public JwtUtility(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(USERNAME_CLAIM, user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }
```

When validating or extracting data from a token, the same cryptographic key must be used to decrypt the claims. The parser attempts to read the JWT using the configured key. If the JWT is valid, no exceptions are thrown and it returns `true`. An exception is thrown if the token is tampered with or expired.

[JwtUtility.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/utility/JwtUtility.java#L48-L56)
```java
    public boolean validateToken(String token){
        try{
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
```

### Route Protection via Interceptors

With the API now serving a decoupled frontend application (such as the `angular3-demo`), it's critical to ensure routes are protected. We implement a custom `AuthInterceptor` which validates the JWT sent by the frontend in the `Authorization` header.
This interceptor is registered in our `WebConfig` to protect all endpoints by default, explicitly excluding the public `/login` and `/register` endpoints.

[WebConfig.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/utility/WebConfig.java#L35-L48)
```java
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/register",
                        "/login"
                );
    }
```

The `AuthInterceptor` itself implements `HandlerInterceptor`. It checks the `Authorization` header, validates the token, and attaches the parsed claims (`userId` and `username`) directly to the `HttpServletRequest` as attributes. This allows controllers to access the user's identity without having to re-parse the JWT.
It also allows CORS preflight (`OPTIONS`) requests through without authentication, strips the `Bearer ` prefix, and validates the token via `jwtUtility`.

[AuthInterceptor.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/utility/AuthInterceptor.java)
```java
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or malformed Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtUtility.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return false;
        }

        String userId = jwtUtility.extractSubject(token);
        String username = jwtUtility.extractUsername(token);

        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        return true;
    }
```

Because the `AuthInterceptor` attaches the `userId` to the request, the Controllers can simply cast and read it. This creates a secure, stateless architecture where the backend knows exactly who is making the request without maintaining a session.

[BookController.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/controller/BookController.java)
```java
    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@RequestBody Book book, HttpServletRequest request) {
        UUID userId = UUID.fromString((String) request.getAttribute("userId"));
        
        Book createdBook = bookService.createBook(book, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }
```

> [!NOTE]
> Verified against standard RFC 7519 specifications for JSON Web Tokens.
> Verified `jjwt` 0.13.0 Builder and Parser APIs (`verifyWith`, `parseSignedClaims`): https://github.com/jwtk/jjwt#reading-parsing-a-jws
> Verified Spring Web `HandlerInterceptor` execution flow: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html
