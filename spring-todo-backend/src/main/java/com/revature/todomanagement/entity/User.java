package com.revature.todomanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a user in the system.
 * Contains account credentials and identity information.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for account identification.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Unique email address associated with the account.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password for secure authentication.
     */
    @Column(name = "password_hash", nullable = false)
    private String password;

    /**
     * Timestamp indicating when the account was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the account was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * List of todos owned by this user.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;
}
