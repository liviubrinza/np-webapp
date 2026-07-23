-- Default credentials: admin / admin. Change this password before any real deployment.
INSERT INTO admin_users (id, username, password_hash, role) VALUES
    (1, 'admin', '$2a$10$WZDm86bttGQUB1lYgCQ/teHorlrcidzPV1FnoJwrtEqVPpbBWVwDq', 'ADMIN');

ALTER TABLE admin_users ALTER COLUMN id RESTART WITH 2;
