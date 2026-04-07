-- Employees table
CREATE TABLE employee
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table (1:N — each task has one reporter and optionally one assignee)
CREATE TABLE task
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_key     VARCHAR(20)  NOT NULL UNIQUE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    story_points INT,
    reporter_id  BIGINT       NOT NULL,
    assignee_id  BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_reporter FOREIGN KEY (reporter_id) REFERENCES employee (id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES employee (id)
);

-- Tags table
CREATE TABLE tag
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Task-Tag join table (N:M)
CREATE TABLE task_tag
(
    task_id  BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_tl_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE,
    CONSTRAINT fk_tl_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_task_status ON task (status);
CREATE INDEX idx_task_assignee ON task (assignee_id);
CREATE INDEX idx_task_reporter ON task (reporter_id);
CREATE INDEX idx_task_priority ON task (priority);
