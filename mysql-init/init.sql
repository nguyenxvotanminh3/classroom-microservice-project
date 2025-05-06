-- Create databases
CREATE DATABASE IF NOT EXISTS user_write_db;
CREATE DATABASE IF NOT EXISTS user_read_db1;
CREATE DATABASE IF NOT EXISTS user_read_db2;
CREATE DATABASE IF NOT EXISTS classroom_read_db1;
CREATE DATABASE IF NOT EXISTS nguyenminh_classroom;

-- Grant privileges
GRANT ALL PRIVILEGES ON user_write_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON user_read_db1.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON user_read_db2.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON classroom_read_db1.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON nguyenminh_classroom.* TO 'root'@'%';
FLUSH PRIVILEGES; 