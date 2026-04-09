-- Employees
INSERT INTO employee (username, password, email, full_name, role) VALUES
    ('jdoe',    '$2a$10$FNc7lD1TVHmxNxyuZnNDu.IiB.lx2gYcRlsz0y8gQ.K08HXtBs4nG', 'john.doe@example.com',    'John Doe',    'ADMIN'),
    ('asmith',  '$2a$10$FNc7lD1TVHmxNxyuZnNDu.IiB.lx2gYcRlsz0y8gQ.K08HXtBs4nG', 'alice.smith@example.com',  'Alice Smith',  'PROJECT_MANAGER'),
    ('bwilson', '$2a$10$FNc7lD1TVHmxNxyuZnNDu.IiB.lx2gYcRlsz0y8gQ.K08HXtBs4nG', 'bob.wilson@example.com',   'Bob Wilson',   'DEVELOPER'),
    ('cjones',  '$2a$10$FNc7lD1TVHmxNxyuZnNDu.IiB.lx2gYcRlsz0y8gQ.K08HXtBs4nG', 'carol.jones@example.com',  'Carol Jones',  'DEVELOPER');

-- Tags
INSERT INTO tag (name, description) VALUES
    ('bug',         'Something is broken'),
    ('feature',     'New feature request'),
    ('improvement', 'Enhancement to existing functionality'),
    ('backend',     'Backend related work'),
    ('frontend',    'Frontend related work');

-- Tasks
INSERT INTO task (task_key, title, description, status, priority, story_points, reporter_id, assignee_id) VALUES
    ('PROJ-1', 'Set up project infrastructure',  'Initialize the project repository and CI/CD pipeline',  'DONE',        'HIGH',   3, 1, 3),
    ('PROJ-2', 'User authentication module',     'Implement JWT-based authentication',                    'IN_PROGRESS', 'HIGH',   8, 2, 3),
    ('PROJ-3', 'Fix login page crash on mobile', 'App crashes when rotating screen on login page',        'TODO',        'URGENT', 2, 4, 3),
    ('PROJ-4', 'Design dashboard wireframes',    'Create wireframes for the main dashboard view',         'IN_REVIEW',   'MEDIUM', 5, 2, 4),
    ('PROJ-5', 'API rate limiting',              'Implement rate limiting for public API endpoints',      'TODO',        'MEDIUM', 5, 1, NULL);

-- Task-Tag associations
INSERT INTO task_tag (task_id, tag_id) VALUES
    (1, 5),
    (2, 2),
    (2, 5),
    (3, 1),
    (3, 4),
    (5, 3),
    (5, 5);
