package com.revature.todomanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a sub-item of a primary Task.
 */
@Entity
@Table(name = "subtasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtask {
    /**
     * Unique identifier for the subtask.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent task this subtask belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Task task;

    /**
     * The title or name of the subtask.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Completion status of the subtask.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;

    /**
     * Timestamp indicating when the subtask was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the subtask was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
