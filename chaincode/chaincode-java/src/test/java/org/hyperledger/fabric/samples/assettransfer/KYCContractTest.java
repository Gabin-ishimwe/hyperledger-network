/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.contract.ClientIdentity;

import com.google.protobuf.Timestamp;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public final class KYCContractTest {

    private static final String SAMPLE_KYC_JSON = "{ \"id\": \"kyc1\", \"fullName\": \"John Doe\", \"dob\": \"1990-01-15\", " +
            "\"nationality\": \"USA\", \"documentType\": \"Passport\", \"documentHash\": \"QmHash123\", " +
            "\"status\": \"PENDING\", \"issuerMSP\": \"Org1MSP\", \"updatedAt\": 1700000000 }";

    @Nested
    class InvokeGetKYCTransaction {

        @Test
        public void whenKYCExists() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);

            KYCRecord record = contract.GetKYC(ctx, "kyc1");

            assertThat(record.getId()).isEqualTo("kyc1");
            assertThat(record.getFullName()).isEqualTo("John Doe");
            assertThat(record.getStatus()).isEqualTo(KYCStatus.PENDING);
        }

        @Test
        public void whenKYCDoesNotExist() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn("");

            Throwable thrown = catchThrowable(() -> {
                contract.GetKYC(ctx, "kyc1");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("KYC record kyc1 does not exist");
            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("KYC_NOT_FOUND".getBytes());
        }
    }

    @Nested
    class InvokeCreateKYCTransaction {

        @Test
        public void whenKYCExists() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);

            Throwable thrown = catchThrowable(() -> {
                contract.CreateKYC(ctx, "kyc1", "John Doe", "1990-01-15", "USA", "Passport", "QmHash123");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("KYC record kyc1 already exists");
            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("KYC_ALREADY_EXISTS".getBytes());
        }

        @Test
        public void whenKYCDoesNotExist() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity clientIdentity = mock(ClientIdentity.class);
            Timestamp timestamp = Timestamp.newBuilder().setSeconds(1700000000L).build();

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("Org1MSP");
            when(stub.getStringState("kyc1")).thenReturn("");
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochSecond(1700000000L));

            KYCRecord record = contract.CreateKYC(ctx, "kyc1", "John Doe", "1990-01-15", "USA", "Passport", "QmHash123");

            assertThat(record.getId()).isEqualTo("kyc1");
            assertThat(record.getFullName()).isEqualTo("John Doe");
            assertThat(record.getStatus()).isEqualTo(KYCStatus.PENDING);
            assertThat(record.getIssuerMSP()).isEqualTo("Org1MSP");
        }
    }

    @Nested
    class InvokeVerifyKYCTransaction {

        @Test
        public void whenKYCDoesNotExist() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn("");

            Throwable thrown = catchThrowable(() -> {
                contract.VerifyKYC(ctx, "kyc1", "APPROVE");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("KYC record kyc1 does not exist");
            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("KYC_NOT_FOUND".getBytes());
        }

        @Test
        public void whenDecisionIsApprove() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity clientIdentity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("AuditorMSP");
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochSecond(1700000100L));

            KYCRecord record = contract.VerifyKYC(ctx, "kyc1", "APPROVE");

            assertThat(record.getStatus()).isEqualTo(KYCStatus.VERIFIED);
            assertThat(record.getIssuerMSP()).isEqualTo("AuditorMSP");
        }

        @Test
        public void whenDecisionIsReject() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity clientIdentity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("AuditorMSP");
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochSecond(1700000100L));

            KYCRecord record = contract.VerifyKYC(ctx, "kyc1", "REJECT");

            assertThat(record.getStatus()).isEqualTo(KYCStatus.REJECTED);
        }

        @Test
        public void whenDecisionIsInvalid() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);

            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);

            Throwable thrown = catchThrowable(() -> {
                contract.VerifyKYC(ctx, "kyc1", "INVALID");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Invalid decision: INVALID. Must be APPROVE or REJECT");
            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("INVALID_DECISION".getBytes());
        }
    }

    @Nested
    class InvokeExpireKYCTransaction {

        @Test
        public void whenKYCDoesNotExist() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn("");

            Throwable thrown = catchThrowable(() -> {
                contract.ExpireKYC(ctx, "kyc1");
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("KYC record kyc1 does not exist");
            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("KYC_NOT_FOUND".getBytes());
        }

        @Test
        public void whenKYCExists() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            ClientIdentity clientIdentity = mock(ClientIdentity.class);

            when(ctx.getStub()).thenReturn(stub);
            when(ctx.getClientIdentity()).thenReturn(clientIdentity);
            when(clientIdentity.getMSPID()).thenReturn("AdminMSP");
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);
            when(stub.getTxTimestamp()).thenReturn(Instant.ofEpochSecond(1700000200L));

            KYCRecord record = contract.ExpireKYC(ctx, "kyc1");

            assertThat(record.getStatus()).isEqualTo(KYCStatus.EXPIRED);
            assertThat(record.getIssuerMSP()).isEqualTo("AdminMSP");
        }
    }

    @Nested
    class InvokeKYCExistsTransaction {

        @Test
        public void whenKYCExists() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn(SAMPLE_KYC_JSON);

            boolean exists = contract.KYCExists(ctx, "kyc1");

            assertThat(exists).isTrue();
        }

        @Test
        public void whenKYCDoesNotExist() {
            KYCContract contract = new KYCContract();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("kyc1")).thenReturn("");

            boolean exists = contract.KYCExists(ctx, "kyc1");

            assertThat(exists).isFalse();
        }
    }
}
