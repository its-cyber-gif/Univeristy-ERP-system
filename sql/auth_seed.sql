USE authdb;

INSERT INTO users_auth (username, role, password_hash, status) VALUES
('admin1','ADMIN','$2a$10$EIXChVgFX5kWq3M1Fasv4.8i1r8F9b8e0GZB1Q9JQq5kVwQk0e9yK','ACTIVE'),
('inst1','INSTRUCTOR','$2a$10$EIXChVgFX5kWq3M1Fasv4.8i1r8F9b8e0GZB1Q9JQq5kVwQk0e9yK','ACTIVE'),
('stu1','STUDENT','$2a$10$EIXChVgFX5kWq3M1Fasv4.8i1r8F9b8e0GZB1Q9JQq5kVwQk0e9yK','ACTIVE'),
('stu2','STUDENT','$2a$10$EIXChVgFX5kWq3M1Fasv4.8i1r8F9b8e0GZB1Q9JQq5kVwQk0e9yK','ACTIVE');
