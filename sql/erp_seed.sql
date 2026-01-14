USE erpdb;

INSERT INTO students (user_id, roll_no, program, year) VALUES
(3,'2024299','B.Tech CS',2025),
(4,'2024345','B.Tech IT',2025);

INSERT INTO instructors (user_id, department) VALUES
(2,'Computer Science');

INSERT INTO courses (code, title, credits) VALUES
('CSE101','Introduction to Programming',4),
('CSE102','Data Structures',4);

INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES
(1,2,'Mon/Wed 10:00-11:30','C101',30,'Monsoon',2025),
(2,2,'Tue/Thu 14:00-15:30','C102',25,'Winter',2025);

-- insert maintenance OFF
INSERT INTO settings (`key`,`value`) VALUES ('maintenance_on','false');
