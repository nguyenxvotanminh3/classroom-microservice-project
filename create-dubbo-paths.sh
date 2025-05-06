#!/bin/bash

# Đợi Zookeeper khởi động
echo "Đợi Zookeeper khởi động..."
sleep 10

# Tạo đường dẫn cần thiết cho Dubbo trong Zookeeper
echo "Tạo các đường dẫn Dubbo trong Zookeeper..."

# Đường dẫn đến zkCli.sh
ZK_CLI="/usr/bin/zookeeper-shell"

# Tạo path /dubbo
echo "create /dubbo ''" | $ZK_CLI zookeeper:2181

# Tạo các service path
echo "create /dubbo/com.kienlongbank.api.UserService ''" | $ZK_CLI zookeeper:2181
echo "create /dubbo/com.kienlongbank.api.UserService/providers ''" | $ZK_CLI zookeeper:2181
echo "create /dubbo/com.kienlongbank.api.UserService/consumers ''" | $ZK_CLI zookeeper:2181
echo "create /dubbo/com.kienlongbank.api.UserService/configurators ''" | $ZK_CLI zookeeper:2181
echo "create /dubbo/com.kienlongbank.api.UserService/routers ''" | $ZK_CLI zookeeper:2181

# Thêm các service khác tại đây nếu cần

echo "Đã tạo xong các đường dẫn Dubbo trong Zookeeper" 