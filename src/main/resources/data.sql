
-- Insert default tenant
INSERT IGNORE INTO tenant (id, name, slug, active, created_at, updated_at) VALUES
(1, 'CBD', 'cbd', true, '2025-06-10 17:00:00', '2025-06-10 17:00:00');

-- Insert into users (tenant_id=1 for regular users, NULL for superadmin)
INSERT INTO users (id, created_at, updated_at, username, full_name, password, superadmin, tenant_id) VALUES
(2, '2025-06-10 17:12:06.803000', '2025-06-10 17:12:06.803000', 'janko', 'Janko Ljubic', '$2a$10$32I8LTgojlnoubqi1thPaejOQELVtIujDfkY5VTlB./ddbmiixqqG', false, 1),
(1, '2025-06-10 17:10:14.479000', '2025-06-10 17:10:14.480000', 'milica', 'Milica Ljubic', '$2a$10$y7J.7LHt.M3sAJSfIbSjcOdyeg9LWtEI6a7PyvNPldJPx7bTyW2H.', false, 1),
(3, '2025-06-10 17:00:00', '2025-06-10 17:00:00', 'superadmin', 'Platform Admin', '$2a$10$32I8LTgojlnoubqi1thPaejOQELVtIujDfkY5VTlB./ddbmiixqqG', true, NULL);

-- Insert into role (admin renamed to company_admin)
INSERT INTO role (id, name) VALUES
(1, 'company_admin'),
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
(11, 'payment-add'),
(12, 'attendance-check-in'),
(13, 'attendance-view-all'),
(14, 'attendance-edit'),
(15, 'location-manage');

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
(12, 1),
(13, 1),
(14, 1),
(15, 1),
(4, 2),
(5, 2),
(6, 2),
(7, 2),
(10, 2),
(12, 2),
(12, 3),
(13, 3);
