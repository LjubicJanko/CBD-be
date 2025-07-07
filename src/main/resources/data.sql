
-- Insert into users
INSERT INTO users (id, created_at, updated_at, username, full_name, password) VALUES
(2, '2025-06-10 17:12:06.803000', '2025-06-10 17:12:06.803000', 'janko', 'Janko Ljubic', '$2a$10$32I8LTgojlnoubqi1thPaejOQELVtIujDfkY5VTlB./ddbmiixqqG'),
(1, '2025-06-10 17:10:14.479000', '2025-06-10 17:10:14.480000', 'milica', 'Milica Ljubic', '$2a$10$y7J.7LHt.M3sAJSfIbSjcOdyeg9LWtEI6a7PyvNPldJPx7bTyW2H.');

-- Insert into role
INSERT INTO role (id, name) VALUES
(1, 'admin'),
(2, 'manufacturer'),
(3, 'manager');

-- Insert into privilege
INSERT INTO privilege (id, name) VALUES
(1, 'order-create'),
(2, 'order-info-edit'),
(3, 'move-to-print-ready'),
(4, 'move-to-printing'),
(5, 'move-to-sewing'),
(6, 'move-to-ship-ready'),
(7, 'move-to-shipped'),
(8, 'move-to-done'),
(9, 'order-cancel'),
(10, 'order-pause'),
(11, 'payment-add');

-- Insert into users_roles
INSERT INTO users_roles (user_id, role_id) VALUES
(1, 2),
(2, 1);

-- Insert into roles_privileges
INSERT INTO roles_privileges (privilege_id, role_id) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(5, 1),
(6, 1),
(7, 1),
(8, 1),
(9, 1),
(10, 1),
(11, 1),
(4, 2),
(5, 2),
(6, 2),
(7, 2),
(10, 2);