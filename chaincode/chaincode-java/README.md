## Basic Asset Transfer

This sample implements the basic asset transfer scenario, illustrating the use of the Java Contract SDKs to provide a
smart contract as a service.

### Building with Maven

```bash
# Compile the chaincode
mvn clean compile

# Run tests
mvn test

# Package as a shaded JAR
mvn clean package

# The packaged chaincode JAR will be at: target/chaincode.jar
```

### Docker Build

```bash
docker build -t asset-transfer-basic-java .
```

### Running as Chaincode as a Service

To run this chaincode contract locally on a development network, see:

- [Debugging chaincode as a service](../../test-network-k8s/docs/CHAINCODE_AS_A_SERVICE.md) (Kube test network)
- [End-to-end with the test-network](../../test-network/CHAINCODE_AS_A_SERVICE_TUTORIAL.md#end-to-end-with-the-the-test-network) (Docker compose)
