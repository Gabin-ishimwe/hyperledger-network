// crypto-service/server.js
const express = require('express');
const fs = require('fs');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8080;

// Middleware
app.use(cors());
app.use(express.json());

// Base path to crypto materials (will be mounted via Docker volume)
const CRYPTO_BASE_PATH = process.env.CRYPTO_BASE_PATH || '../local-network/organizations';

/**
 * Utility function to convert org name to MSP ID
 * Example: "org1.example.com" -> "Org1MSP"
 */
function orgNameToMspId(orgName) {
  const orgBaseName = orgName.split('.')[0];
  return orgBaseName.charAt(0).toUpperCase() + orgBaseName.slice(1) + 'MSP';
}

/**
 * Health check endpoint
 * GET /health
 */
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    service: 'openledger-crypto-service',
    timestamp: new Date().toISOString(),
    cryptoPath: CRYPTO_BASE_PATH
  });
});

/**
 * Get complete crypto materials needed for Fabric Gateway setup
 * Returns user identity + TLS certificates for secure peer communication
 * GET /api/crypto/gateway/:orgName/:userName/:peerName
 */
app.get('/api/crypto/gateway/:orgName/:userName/:peerName', (req, res) => {
  try {
    const { orgName, userName, peerName } = req.params;
    
    // Validate parameters
    if (!orgName || !userName || !peerName) {
      return res.status(400).json({ 
        error: 'Organization name, user name, and peer name are required' 
      });
    }

    console.log(`📋 Requesting gateway crypto materials for ${userName}@${orgName} via ${peerName}.${orgName}`);

    // === USER IDENTITY MATERIALS ===
    const userPath = path.join(
      CRYPTO_BASE_PATH,
      'peerOrganizations',
      orgName,
      'users',
      `${userName}@${orgName}`
    );

    // Check if user exists
    if (!fs.existsSync(userPath)) {
      console.log(`❌ User path not found: ${userPath}`);
      return res.status(404).json({ 
        error: `User ${userName}@${orgName} not found` 
      });
    }

    // Get user certificate
    const certDir = path.join(userPath, 'msp/signcerts');
    if (!fs.existsSync(certDir)) {
      return res.status(404).json({ 
        error: 'User certificate directory not found' 
      });
    }

    const certFiles = fs.readdirSync(certDir);
    if (certFiles.length === 0) {
      return res.status(404).json({ 
        error: 'No user certificate files found' 
      });
    }

    const userCertificate = fs.readFileSync(
      path.join(certDir, certFiles[0]), 
      'utf8'
    );

    // Get user private key
    const keyDir = path.join(userPath, 'msp/keystore');
    if (!fs.existsSync(keyDir)) {
      return res.status(404).json({ 
        error: 'User private key directory not found' 
      });
    }

    const keyFiles = fs.readdirSync(keyDir);
    if (keyFiles.length === 0) {
      return res.status(404).json({ 
        error: 'No user private key files found' 
      });
    }

    const userPrivateKey = fs.readFileSync(
      path.join(keyDir, keyFiles[0]), 
      'utf8'
    );

    // === TLS MATERIALS FOR SECURE COMMUNICATION ===
    const tlsCertPath = path.join(
      CRYPTO_BASE_PATH,
      'peerOrganizations',
      orgName,
      'peers',
      `${peerName}.${orgName}`,
      'tls/ca.crt'
    );

    // Check if TLS cert exists
    if (!fs.existsSync(tlsCertPath)) {
      console.log(`❌ TLS cert not found: ${tlsCertPath}`);
      return res.status(404).json({ 
        error: `TLS certificate for ${peerName}.${orgName} not found` 
      });
    }

    const tlsCaCert = fs.readFileSync(tlsCertPath, 'utf8');

    // Get MSP ID
    const mspId = orgNameToMspId(orgName);

    console.log(`✅ Successfully retrieved gateway crypto materials for ${userName}@${orgName}`);

    // Return everything needed for Fabric Gateway setup
    res.json({
      // User identity for transaction signing
      identity: {
        certificate: userCertificate,
        privateKey: userPrivateKey,
        mspId: mspId
      },
      // TLS materials for secure peer communication
      tls: {
        caCert: tlsCaCert,
        peerEndpoint: `${peerName}.${orgName}:7051`
      },
      // Metadata
      metadata: {
        userName,
        orgName,
        peerName,
        generatedAt: new Date().toISOString()
      }
    });

  } catch (error) {
    console.error(`❌ Error retrieving gateway crypto materials:`, error);
    res.status(500).json({ 
      error: 'Internal server error',
      message: error.message
    });
  }
});

// Startup validation
function validateSetup() {
  console.log('🔍 Validating crypto service setup...');
  console.log(`📁 Crypto base path: ${CRYPTO_BASE_PATH}`);
  
  if (!fs.existsSync(CRYPTO_BASE_PATH)) {
    console.error(`❌ Crypto base path not found: ${CRYPTO_BASE_PATH}`);
    console.error('   Make sure the Hyperledger network has been started and crypto materials generated');
    process.exit(1);
  }
  
  const peerOrgsPath = path.join(CRYPTO_BASE_PATH, 'peerOrganizations');
  if (!fs.existsSync(peerOrgsPath)) {
    console.error(`❌ Peer organizations directory not found: ${peerOrgsPath}`);
    console.error('   Make sure the network has been properly initialized');
    process.exit(1);
  }
  
  console.log('✅ Crypto service setup validation passed');
}

// Start server
validateSetup();

app.listen(PORT, () => {
  console.log('🚀 OpenLedger Crypto Service started');
  console.log(`📡 Server running on port ${PORT}`);
  console.log(`🏥 Health check: http://localhost:${PORT}/health`);
  console.log(`📖 Gateway crypto endpoint:`);
  console.log(`   GET /api/crypto/gateway/:orgName/:userName/:peerName`);
  console.log('');
  console.log(`💡 Example usage:`);
  console.log(`   curl http://localhost:${PORT}/api/crypto/gateway/org1.example.com/User1/peer0`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('🛑 Received SIGTERM signal, shutting down gracefully...');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('🛑 Received SIGINT signal, shutting down gracefully...');
  process.exit(0);
});
