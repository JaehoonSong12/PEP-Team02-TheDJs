# Tech Stack

## Backend
- **Language:** Java 21
- **Framework:** Spring Boot 4.1.0
- **Modules:** Spring Web MVC, Spring Data JPA, Spring Modulith
- **Security:** JJWT 0.13.0 (jjwt-api, jjwt-impl, jjwt-jackson)
- **Database:** SQLite (via `org.xerial:sqlite-jdbc:3.53.2.0` + `hibernate-community-dialects` for SQLite dialect)
- **ORM:** Hibernate (via Spring Data JPA)
- **Utilities:** Lombok (boilerplate reduction via annotations)
- **Build Tool:** Gradle (Kotlin DSL — `build.gradle.kts`)
- **Base Package:** `com.revature.todomanagement`

## Frontend
- **Framework:** Angular
- **HTTP:** Angular `HttpClient` with an interceptor that attaches `Authorization: Bearer <JWT>` on protected routes

## Testing
- **Unit/Integration:** JUnit 5 (via `spring-boot-starter-test`)
- **In-memory DB for tests:** H2 2.4.240 (replaces SQLite in test context)
- **JPA test slice:** `spring-boot-starter-data-jpa-test`
- **Web MVC test slice:** `spring-boot-starter-webmvc-test`

## Database Configuration (application.properties)
```properties
spring.datasource.url=jdbc:sqlite:todo.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```
> Note: `ddl-auto=create-drop` means the schema is rebuilt on every startup. Change to `update` or `validate` when persistence across restarts is needed.

## Common Commands

All backend commands run from `spring-todo-backend/`:

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests only
./gradlew test

# Clean build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test
```

On Windows use `gradlew.bat` instead of `./gradlew`.
