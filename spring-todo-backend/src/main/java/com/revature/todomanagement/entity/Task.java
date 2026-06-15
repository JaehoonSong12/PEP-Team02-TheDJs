package com.revature.todomanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a Todo item (Task) in the system.
 * A Task is owned by a User and can contain multiple Subtasks.
 */
@Entity
@Table(name = "todos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    /**
     * Unique identifier for the task.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this task.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The title or name of the task.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Detailed description of what the task involves.
     */
    private String description;

    /**
     * Completion status of the task.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;

    /**
     * Timestamp indicating when the task was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the task was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * List of subtasks belonging to this task.
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subtask> subtasks;
}
