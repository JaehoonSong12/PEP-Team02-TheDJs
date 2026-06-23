# Spring Data Reference Guide

## Spring Data Overview

Spring Data is a collection of projects designed to simplify data access and persistence in Java applications. It provides a consistent, high-level abstraction for working with various data stores, relational databases, NoSQL databases, and more, while reducing boilerplate code and enabling rapid development.

Key benefits include:
- **Unified programming model:** Work with different data sources using a common set of interfaces and patterns.
- **Repository abstraction:** Define interfaces for data access, and let Spring generate the implementation at runtime.
- **Integration:** Seamlessly integrates with Spring’s core features like dependency injection and transaction management.
- **Productivity:** Focus on business logic instead of repetitive CRUD operations.

Spring Data streamlines data access, making it easier to build robust, maintainable, and scalable applications.

## Spring Orm

Spring ORM (Object-Relational Mapping) provides integration layers for popular ORM frameworks such as Hibernate, JPA, and MyBatis. It manages the complexities of resource management, transaction handling, and exception translation, allowing you to focus on your domain model.

Highlights:
- **Simplified configuration:** Easily wire up ORM frameworks with Spring’s dependency injection.
- **Transaction management:** Consistent, declarative transaction support across different ORM providers.
- **Exception translation:** Converts low-level persistence exceptions into Spring’s unified `DataAccessException` hierarchy for easier error handling.

Spring ORM bridges the gap between object-oriented code and relational databases, promoting clean separation of concerns and reducing boilerplate.

## Spring Data JPA & JPA Hibernate

Spring Data JPA builds on top of JPA (Java Persistence API) and Hibernate, offering a powerful, repository-driven approach to data access. It allows you to define repository interfaces for your entities, and Spring automatically provides the implementation.

Key features:
- **Repository interfaces:** Define CRUD and custom query methods using simple Java interfaces.
- **Query derivation:** Spring can generate queries based on method names (e.g., `findByLastName`).
- **@Query annotation:** Write custom JPQL or native SQL queries directly in your repository interfaces.
- **Pagination and sorting:** Built-in support for paginated and sorted queries.
- **Integration with Hibernate:** Hibernate is the most common JPA provider, offering advanced ORM features and performance optimizations.

Spring Data JPA accelerates development by handling the repetitive aspects of data access, letting you focus on your domain logic.

### Example: Database Configuration

Before we can configure the application properties, we must ensure the correct dependencies (like the database driver and JPA starter) are included in the build configuration:

[build.gradle.kts](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/build.gradle.kts#L22-L32)
```kotlin
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
// ...
	implementation("org.xerial:sqlite-jdbc:3.53.2.0")
	implementation("org.hibernate.orm:hibernate-community-dialects:8.0.0.Alpha1")
```

The JPA Dialect and Database URL are provided in the properties file. The `ddl-auto=create-drop` setting explicitly tells Hibernate to destroy and recreate the schema on application startup, which is useful for testing.

[application.properties](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/resources/application.properties#L4-L14)
```properties
# This one tells Spring where the DB is
spring.datasource.url=jdbc:sqlite:todo.db

# This one tells Spring what flavor of SQL to use
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

# This one tells Spring to destroy all tables and recreate them when the app starts
spring.jpa.hibernate.ddl-auto=create-drop
```

## JPA Repository vs CRUD Repository

Spring Data provides several repository interfaces, each offering different levels of functionality:

- **CrudRepository:** The basic interface for generic CRUD operations (create, read, update, delete). It provides methods like `save`, `findById`, `findAll`, and `delete`.
- **JpaRepository:** Extends `CrudRepository` with additional JPA-specific features such as batch operations, flushing, and support for pagination and sorting.

In practice, `JpaRepository` is preferred for most JPA-based applications, as it offers a richer set of features out of the box. Use `CrudRepository` for simple scenarios or when you want to keep dependencies minimal.

### Example: Defining a JPA Repository

To create a repository, we extend `JpaRepository` and pass two generic types: the Entity class (`User`) and the type of its primary key (`UUID`). Custom queries like `findByUsername` are implemented automatically by Spring, returning an `Optional` to avoid `NullPointerException`s if no user is found.

[UserRepo.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/repo/UserRepo.java#L20-L24)
```java
@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
    // We don't have to tell Spring how to write/implement this query: it just knows how
    Optional<User> findByUsername(String username);
```

## Annotations

Spring Data and JPA use a variety of annotations to map entities and configure repositories:

- **@Entity:** Marks a class as a JPA entity mapped to a database table.
- **@Id:** Specifies the primary key of an entity.
- **@GeneratedValue:** Configures how the primary key is generated (e.g., auto-increment).
- **@Table:** Customizes the table name and schema mapping.
- **@Column:** Customizes column mapping and constraints.
- **@Repository:** Marks a class or interface as a Spring Data repository.
- **@Query:** Defines custom queries for repository methods.
- **@Transactional:** Manages transaction boundaries for data access operations.

These annotations help bridge the gap between your Java domain model and the underlying database, while keeping configuration concise and expressive.

### Example: Mapping an Entity

We use `@Entity` to map the `User` class to a database table, optionally customizing the table name with `@Table(name = "users")`. The `@Id` marks the primary key, and `@GeneratedValue` auto-generates a `UUID` to prevent username changes from breaking relationships.

[User.java](file:///c:/Users/manua/source/repo/_references/__study_notes/spring01-example/src/main/java/com/example/demo/entity/User.java#L20-L28)
```java
@Data // tells Lombok to provide setters, getters, required args constructor, and more
@NoArgsConstructor // needed for Entity decorator
@Entity // indicates the class should be mapped to a table in our database
@Table(name = "users") // if you want to change the name of your table specify it in the decorator
public class User {
    @Column // indicates the field represents a column
    @Id // indicates the field is the primary key (column is technically not needed when we use this)
    @GeneratedValue(strategy = GenerationType.UUID) // indicates the value should be generated by the db
    private UUID id;
```

## Transaction Propagation Strategies

Transaction management is crucial for data integrity and consistency. Spring supports various transaction propagation strategies, which determine how transactions behave when calling methods that are themselves transactional:

- **REQUIRED (default):** Joins the current transaction or creates a new one if none exists.
- **REQUIRES_NEW:** Suspends the current transaction and starts a new one.
- **SUPPORTS:** Runs within a transaction if one exists; otherwise, runs non-transactionally.
- **MANDATORY:** Must run within an existing transaction; throws an exception if none exists.
- **NOT_SUPPORTED:** Runs non-transactionally, suspending any existing transaction.
- **NEVER:** Must not run within a transaction; throws an exception if one exists.
- **NESTED:** Runs within a nested transaction if supported by the underlying database.

These propagation strategies are configured using the `@Transactional` annotation, typically at the service or repository layer. You can specify the desired behavior with the `propagation` attribute. Choose the appropriate propagation strategy based on your business requirements and data consistency needs.

## ACID

ACID is a set of properties that guarantee reliable processing of database transactions:

- **Atomicity:** Transactions are all-or-nothing; either all operations succeed, or none do.
- **Consistency:** Transactions bring the database from one valid state to another, maintaining data integrity.
- **Isolation:** Concurrent transactions do not interfere with each other; intermediate states are not visible.
Spring’s transaction management, combined with ACID-compliant databases, helps ensure your data remains safe, consistent, and reliable.

> [!NOTE]
> Verified against official Spring Data documentation: [Core Concepts](https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html).