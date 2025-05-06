#!/bin/bash
set -e

VAULT_CONFIG_DIR="/vault/config"
VAULT_DATA_DIR="/vault/data"
VAULT_INIT_FILE="${VAULT_DATA_DIR}/.init_complete"

# Make sure vault directories have proper permissions
echo "Setting up directory permissions..."
mkdir -p "${VAULT_DATA_DIR}"
mkdir -p "${VAULT_CONFIG_DIR}"
chown -R vault:vault "${VAULT_DATA_DIR}"
chown -R vault:vault "${VAULT_CONFIG_DIR}"
chmod -R 755 "${VAULT_DATA_DIR}"
chmod -R 755 "${VAULT_CONFIG_DIR}"

# Tạo file cấu hình cho Vault nếu chưa tồn tại
if [ ! -f "$VAULT_CONFIG_DIR/vault.json" ]; then
  echo "Creating Vault config file..."
  cat > "$VAULT_CONFIG_DIR/vault.json" << EOF
{
  "storage": {
    "file": {
      "path": "${VAULT_DATA_DIR}"
    }
  },
  "listener": {
    "tcp": {
      "address": "0.0.0.0:8200",
      "tls_disable": 1
    }
  },
  "ui": true
}
EOF
fi

# Khởi động Vault với config
echo "Starting Vault server..."
vault server -config="$VAULT_CONFIG_DIR/vault.json" &
VAULT_PID=$!

# Đặt địa chỉ Vault API
VAULT_API="http://127.0.0.1:8200"
export VAULT_ADDR="http://127.0.0.1:8200"

# Đợi Vault khởi động
echo "Waiting for Vault to start..."
while ! curl -s --head --fail ${VAULT_API}/v1/sys/health > /dev/null; do
  printf '.'
  sleep 1
done
echo "Vault is up and running!"

# Kiểm tra xem Vault đã được initialize chưa
INIT_STATUS=$(curl -s ${VAULT_API}/v1/sys/init | jq -r .initialized)

if [ "$INIT_STATUS" = "false" ]; then
  echo "Initializing Vault..."
  
  # Initialize Vault và lưu unseal keys + root token
  INIT_RESPONSE=$(curl -s \
    --request POST \
    --data '{"secret_shares": 1, "secret_threshold": 1}' \
    ${VAULT_API}/v1/sys/init)
  
  UNSEAL_KEY=$(echo $INIT_RESPONSE | jq -r .keys[0])
  ROOT_TOKEN=$(echo $INIT_RESPONSE | jq -r .root_token)
  
  # Lưu keys và token vào file (lưu ý: đây là cho môi trường dev, không nên làm vậy trong production)
  echo $INIT_RESPONSE > "${VAULT_DATA_DIR}/init_keys.json"
  echo "Vault initialized successfully!"
  
  # Unseal Vault
  echo "Unsealing Vault..."
  curl -s \
    --request POST \
    --data "{\"key\": \"$UNSEAL_KEY\"}" \
    ${VAULT_API}/v1/sys/unseal
  
  echo "Vault unsealed!"
  
  # Đăng nhập và cấu hình Vault
  export VAULT_TOKEN=$ROOT_TOKEN
  
  # Enable KV secrets engine v2
  echo "Enabling KV secrets engine..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"type":"kv-v2"}' \
    ${VAULT_API}/v1/sys/mounts/secret
  
  # Add secrets for security-service
  echo "Adding secrets for security-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"jwt.secret":"MySecretJwtKey123","jwt.expiration":"86400000","auth.whitelist":"/api/auth/login,/api/auth/register,/actuator/**,/swagger-ui/**,/v3/api-docs/**"}}' \
    ${VAULT_API}/v1/secret/data/security-service
  
  # Add secrets for user-service
  echo "Adding secrets for user-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"db.url":"jdbc:mysql://mysql:3306/user_write_db","db.username":"root","db.password":"Mink281104@"}}' \
    ${VAULT_API}/v1/secret/data/user-service
  
  # Add secrets for classroom-service
  echo "Adding secrets for classroom-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"db.url":"jdbc:mysql://mysql:3306/classroom_write_db","db.username":"root","db.password":"Mink281104@","redis.host":"redis","redis.port":"6379"}}' \
    ${VAULT_API}/v1/secret/data/classroom-service
  
  # Add secrets for email-service
  echo "Adding secrets for email-service..."
  curl -s \
    --header "X-Vault-Token: $VAULT_TOKEN" \
    --request POST \
    --data '{"data":{"mail.username":"worknguyenvotanminh@gmail.com","mail.password":"gtejknutlxjzzrjr"}}' \
    ${VAULT_API}/v1/secret/data/email-service
  
  # Đánh dấu đã initialize xong
  touch $VAULT_INIT_FILE
  
  echo "Vault setup completed successfully!"
else
  echo "Vault has already been initialized."
  # Kiểm tra trạng thái sealed
  SEALED_STATUS=$(curl -s ${VAULT_API}/v1/sys/seal-status | jq -r .sealed)
  
  if [ "$SEALED_STATUS" = "true" ]; then
    echo "Vault is sealed. Unsealing..."
    # Lấy unseal key từ file đã lưu
    if [ -f "${VAULT_DATA_DIR}/init_keys.json" ]; then
      UNSEAL_KEY=$(cat "${VAULT_DATA_DIR}/init_keys.json" | jq -r .keys[0])
      
      # Unseal Vault
      curl -s \
        --request POST \
        --data "{\"key\": \"$UNSEAL_KEY\"}" \
        ${VAULT_API}/v1/sys/unseal
      
      echo "Vault unsealed!"
    else
      echo "ERROR: Cannot find unseal keys at ${VAULT_DATA_DIR}/init_keys.json"
      echo "You may need to initialize Vault again."
    fi
  else
    echo "Vault is already unsealed."
  fi
fi

# Giữ container chạy và theo dõi Vault process
echo "Vault is running in background..."
wait $VAULT_PID 