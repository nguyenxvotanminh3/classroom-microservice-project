-- Tạo bảng classrooms
CREATE TABLE IF NOT EXISTS classrooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    teacher_id BIGINT NOT NULL,
    capacity INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (code)
);

-- Tạo bảng student_classroom
CREATE TABLE IF NOT EXISTS student_classroom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    grade DOUBLE,
    feedback TEXT,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, classroom_id)
);

-- Tạo các khóa ngoại
ALTER TABLE student_classroom
ADD CONSTRAINT fk_student_classroom_classroom
FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
ON DELETE CASCADE;

-- Thêm dữ liệu mẫu cho lớp học
INSERT INTO classrooms (name, code, description, teacher_id, capacity)
VALUES ('Mathematics 101', 'MATH101', 'Introduction to Mathematics', 1, 30);

INSERT INTO classrooms (name, code, description, teacher_id, capacity)
VALUES ('Computer Science Basics', 'CS101', 'Introduction to Computer Science', 2, 25);

INSERT INTO classrooms (name, code, description, teacher_id, capacity)
VALUES ('Physics for Beginners', 'PHYS101', 'Basic concepts of Physics', 1, 20); 