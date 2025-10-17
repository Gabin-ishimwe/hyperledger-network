# Crypto Service

A minimal Express.js service that provides **complete crypto materials** needed for Hyperledger Fabric Gateway setup in a single API call.

## What It Provides

This service returns **everything** your backend needs to create a Fabric Gateway client:
- ✅ User certificate & private key (for transaction signing)
- ✅ TLS CA certificate (for secure peer communication)
- ✅ MSP ID (for organization identity)
- ✅ Peer endpoint (for connection)

## Setup

1. **Install dependencies:**
   ```bash
   cd crypto-service
   npm install
   ```

2. **Make sure your Hyperledger network is running**

3. **Start the service:**
   ```bash
   npm start
   ```

## API Endpoint

### Gateway Crypto Materials
- **GET** `/api/crypto/gateway/:orgName/:userName/:peerName`

**Example:**
```bash
curl http://localhost:8080/api/crypto/gateway/org1.example.com/User1/peer0
```

**Response:**
```json
{
  "identity": {
    "certificate": "-----BEGIN CERTIFICATE-----...",
    "privateKey": "-----BEGIN PRIVATE KEY-----...",
    "mspId": "Org1MSP"
  },
  "tls": {
    "caCert": "-----BEGIN CERTIFICATE-----...",
    "peerEndpoint": "peer0.org1.example.com:7051"
  },
  "metadata": {
    "userName": "User1",
    "orgName": "org1.example.com",
    "peerName": "peer0",
    "generatedAt": "2025-10-17T12:00:00.000Z"
  }
}
```

## Usage in Your Backend

```javascript
// 1. Get all crypto materials in one call
const response = await fetch('http://localhost:8080/api/crypto/gateway/org1.example.com/User1/peer0');
const { identity, tls } = await response.json();

// 2. Create Fabric Gateway directly
const tlsCredentials = grpc.credentials.createSsl(Buffer.from(tls.caCert));
const client = new grpc.Client(tls.peerEndpoint, tlsCredentials);

const fabricIdentity = { 
  mspId: identity.mspId, 
  credentials: Buffer.from(identity.certificate) 
};
const signer = signers.newPrivateKeySigner(crypto.createPrivateKey(identity.privateKey));

const gateway = connect({ client, identity: fabricIdentity, signer });
```

## Docker Deployment

```bash
# Build
docker build -t openledger-crypto-service .

# Run
docker run -d \
  --name crypto-service \
  -p 8080:8080 \
  -v $(pwd)/../organizations:/app/organizations:ro \
  openledger-crypto-service
```

## Why This Design?

- **🎯 Single Purpose**: One endpoint for everything you need
- **🚀 Simple Integration**: One API call, ready-to-use materials  
- **🔒 Complete Security**: Both identity and TLS materials included
- **⚡ Minimal Overhead**: No unnecessary endpoints or complexity

Perfect for backends that just want to connect to Fabric without dealing with crypto complexity!
