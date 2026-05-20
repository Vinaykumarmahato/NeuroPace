CREATE DATABASE IF NOT EXISTS neuropace;
USE neuropace;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    enrollment_date TIMESTAMP NOT NULL,
    avg_response_time DOUBLE
);

CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    difficulty_level INT NOT NULL CHECK (difficulty_level BETWEEN 1 AND 10),
    topic_area VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS prerequisites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    requires_subject_id BIGINT NOT NULL,
    weight DOUBLE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (requires_subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_prereq UNIQUE (subject_id, requires_subject_id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    enrolled_date TIMESTAMP NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_enrollment UNIQUE (student_id, subject_id)
);

CREATE TABLE IF NOT EXISTS learning_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    response_time_ms BIGINT NOT NULL,
    performance_score INT NOT NULL CHECK (performance_score BETWEEN 0 AND 5),
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS load_windows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    event_count INT NOT NULL,
    avg_response_time DOUBLE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reroute_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    from_subject_id BIGINT NOT NULL,
    to_subject_id BIGINT NOT NULL,
    reason VARCHAR(1000),
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (from_subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (to_subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);
