#!/bin/sh
# Script khởi tạo đơn giản cho Vault với file storage

# Cấu hình biến môi trường
export VAULT_ADDR=http://127.0.0.1:8200

# Đợi Vault khởi động
echo "Đang đợi Vault khởi động..."
sleep 5

# Kiểm tra trạng thái khởi tạo
INIT_STATUS=$(curl -s ${VAULT_ADDR}/v1/sys/init | jq -r .initialized)

if [ "$INIT_STATUS" = "false" ]; then
  echo "Vault chưa được khởi tạo. Đang khởi tạo..."
  
  # Khởi tạo Vault và lưu keys + token
  INIT_RESPONSE=$(curl -s \
    --request POST \
    --data '{"secret_shares": 1, "secret_threshold": 1}' \
    ${VAULT_ADDR}/v1/sys/init)
  
  UNSEAL_KEY=$(echo $INIT_RESPONSE | jq -r .keys[0])
  ROOT_TOKEN=$(echo $INIT_RESPONSE | jq -r .root_token)
  
  # Lưu thông tin vào file để sử dụng lại sau này
  echo $INIT_RESPONSE > /vault/file/init-keys.json
  
  echo "Vault đã được khởi tạo!"
  echo "Root token: $ROOT_TOKEN"
  echo "Unseal key: $UNSEAL_KEY"
  
  # Unseal Vault
  echo "Đang mở khóa Vault..."
  curl -s \
    --request POST \
    --data "{\"key\": \"$UNSEAL_KEY\"}" \
    ${VAULT_ADDR}/v1/sys/unseal > /dev/null
  
  # Đặt token cho các lệnh tiếp theo
  export VAULT_TOKEN=$ROOT_TOKEN
  
  # Kích hoạt KV secrets engine v2
  echo "Đang xóa KV engine mặc định và kích hoạt KV v2..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request DELETE \
    ${VAULT_ADDR}/v1/sys/mounts/secret > /dev/null
  
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"type":"kv","options":{"version":"2"}}' \
    ${VAULT_ADDR}/v1/sys/mounts/secret > /dev/null
  
  # Thêm secrets ban đầu
  echo "Thêm secrets cho security-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"jwt.secret":"MySecretJwtKey123","jwt.expiration":"86400000","auth.whitelist":"/api/auth/login,/api/auth/register,/actuator/**,/swagger-ui/**,/v3/api-docs/**"}}' \
    ${VAULT_ADDR}/v1/secret/data/security-service > /dev/null
  
  echo "Thêm secrets cho user-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"db.url":"jdbc:mysql://mysql:3306/user_write_db","db.username":"root","db.password":"Mink281104@"}}' \
    ${VAULT_ADDR}/v1/secret/data/user-service > /dev/null
  
  echo "Thêm secrets cho classroom-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"db.url":"jdbc:mysql://mysql:3306/classroom_write_db","db.username":"root","db.password":"Mink281104@","redis.host":"redis","redis.port":"6379"}}' \
    ${VAULT_ADDR}/v1/secret/data/classroom-service > /dev/null
  
  echo "Thêm secrets cho email-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"mail.username":"worknguyenvotanminh@gmail.com","mail.password":"gtejknutlxjzzrjr"}}' \
    ${VAULT_ADDR}/v1/secret/data/email-service > /dev/null
  
  echo "Thiết lập Vault hoàn tất! Dữ liệu đã được lưu trữ vĩnh viễn."
else
  echo "Vault đã được khởi tạo từ trước."
  
  # Kiểm tra trạng thái sealed
  SEALED_STATUS=$(curl -s ${VAULT_ADDR}/v1/sys/seal-status | jq -r .sealed)
  
  if [ "$SEALED_STATUS" = "true" ]; then
    echo "Vault đang ở trạng thái sealed. Đang mở khóa..."
    
    # Đọc unseal key từ file đã lưu
    if [ -f "/vault/file/init-keys.json" ]; then
      UNSEAL_KEY=$(cat "/vault/file/init-keys.json" | jq -r .keys[0])
      ROOT_TOKEN=$(cat "/vault/file/init-keys.json" | jq -r .root_token)
      
      # Unseal Vault
      curl -s \
        --request POST \
        --data "{\"key\": \"$UNSEAL_KEY\"}" \
        ${VAULT_ADDR}/v1/sys/unseal > /dev/null
      
      echo "Vault đã được mở khóa!"
      echo "Sử dụng root token sau để đăng nhập: $ROOT_TOKEN"
    else
      echo "CẢNH BÁO: Không tìm thấy file keys. Không thể tự động mở khóa Vault."
    fi
  else
    echo "Vault đã ở trạng thái mở khóa."
    if [ -f "/vault/file/init-keys.json" ]; then
      ROOT_TOKEN=$(cat "/vault/file/init-keys.json" | jq -r .root_token)
      echo "Sử dụng root token sau để đăng nhập: $ROOT_TOKEN"
    fi
  fi
fi

echo "Vault đã sẵn sàng tại http://localhost:8200" 