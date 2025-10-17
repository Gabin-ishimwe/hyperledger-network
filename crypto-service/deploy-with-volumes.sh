#!/bin/bash
# crypto-service/deploy-with-volumes.sh

echo "🐳 Deploying Crypto Service with Named Volumes"

# Create named volume for crypto materials
docker volume create openledger-crypto-materials

# Copy crypto materials to named volume (run this after network is up)
echo "📋 Copying crypto materials to Docker volume..."
docker run --rm \
  -v /Users/gabinishimwe/Desktop/projects/openledger-network/local-network/organizations:/source:ro \
  -v openledger-crypto-materials:/target \
  alpine:latest \
  cp -r /source/. /target/

# Deploy crypto service
echo "🚀 Starting Crypto Service..."
docker run -d \
  --name openledger-crypto-service \
  -p 8080:8080 \
  -v openledger-crypto-materials:/app/organizations:ro \
  -e CRYPTO_BASE_PATH=/app/organizations \
  -e DOCKER_ENV=true \
  --network fabric-network \
  openledger-crypto-service

echo "✅ Crypto Service deployed successfully"
echo "🏥 Health check: http://localhost:8080/health"
