-- Add initial users to the write database
USE user_write_db;

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('admin', 'Admin User', 'admin@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('user1', 'Regular User', 'user1@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Add initial users to the read database 1
USE user_read_db1;

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('admin', 'Admin User', 'admin@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('user1', 'Regular User', 'user1@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Add initial users to the read database 2
USE user_read_db2;

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('admin', 'Admin User', 'admin@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, full_name, email, password, active, created_at, updated_at) 
VALUES ('user1', 'Regular User', 'user1@example.com', 'password123', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 