package com.tzl.llongagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
public class ConversationEntity {

    @Id
    @Column(length = 6)
    private String id;

    @Column(name = "user_id", nullable = false, length = 6)
    private String userId;

    @Column(length = 200)
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
