CREATE DATABASE IF NOT EXISTS erpdb;
USE erpdb;

CREATE TABLE students (
  user_id INT PRIMARY KEY,
  roll_no VARCHAR(50),
  program VARCHAR(100),
  year INT
);

CREATE TABLE instructors (
  user_id INT PRIMARY KEY,
  department VARCHAR(100)
);

CREATE TABLE courses (
  course_id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  credits INT NOT NULL
);

CREATE TABLE sections (
  section_id INT AUTO_INCREMENT PRIMARY KEY,
  course_id INT NOT NULL,
  instructor_id INT,
  day_time VARCHAR(100),
  room VARCHAR(50),
  capacity INT NOT NULL,
  semester VARCHAR(20),
  year INT,
  FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
  FOREIGN KEY (instructor_id) REFERENCES instructors(user_id) ON DELETE SET NULL
);

CREATE TABLE enrollments (
  enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  section_id INT NOT NULL,
  status ENUM('ENROLLED','DROPPED') DEFAULT 'ENROLLED',
  enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES students(user_id) ON DELETE CASCADE,
  FOREIGN KEY (section_id) REFERENCES sections(section_id) ON DELETE CASCADE,
  UNIQUE KEY unique_enrollment (student_id, section_id)
);

CREATE TABLE grades (
  grade_id INT AUTO_INCREMENT PRIMARY KEY,
  enrollment_id INT NOT NULL,
  component VARCHAR(100),
  score DOUBLE,
  final_grade VARCHAR(10),
  FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
);

CREATE TABLE notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  message VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_read TINYINT(1) DEFAULT 0,
  INDEX idx_notifications_student (student_id),
  INDEX idx_notifications_is_read (is_read)
);

CREATE TABLE settings (
  `key` VARCHAR(100) PRIMARY KEY,
  `value` VARCHAR(255)
);

CREATE TABLE system_settings (
  setting_key VARCHAR(50) PRIMARY KEY,
  setting_value VARCHAR(255)
);
