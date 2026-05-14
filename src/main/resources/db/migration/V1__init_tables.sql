CREATE TABLE users (
    id          VARCHAR(6) PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE conversations (
    id          VARCHAR(6) PRIMARY KEY,
    user_id     VARCHAR(6) NOT NULL REFERENCES users(id),
    title       VARCHAR(200),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(6) NOT NULL,
    message_type    VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_conversations_user ON conversations(user_id);
CREATE INDEX idx_messages_conv_time ON messages(conversation_id, created_at);
