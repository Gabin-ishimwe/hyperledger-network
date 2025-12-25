/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(
        name = "kyc",
        info = @Info(
                title = "KYC Contract",
                description = "Chaincode for managing KYC records",
                version = "1.0.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "kyc@example.com",
                        name = "KYC Team",
                        url = "https://hyperledger.example.com")))
public final class KYCContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum KYCErrors {
        KYC_NOT_FOUND,
        KYC_ALREADY_EXISTS,
        INVALID_DECISION,
        UNAUTHORIZED_OPERATION
    }

    /**
     * Creates a new KYC record on the ledger.
     *
     * @param ctx the transaction context
     * @param id unique identifier for the KYC record
     * @param fullName legal name of the individual
     * @param dob date of birth in ISO 8601 format
     * @param nationality country of citizenship
     * @param documentType type of document (Passport, Driver's License, National ID)
     * @param documentHash IPFS CID or SHA-256 hash of the uploaded document
     * @return the created KYC record
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public KYCRecord CreateKYC(final Context ctx, final String id, final String fullName,
                               final String dob, final String nationality,
                               final String documentType, final String documentHash) {

        if (KYCExists(ctx, id)) {
            String errorMessage = String.format("KYC record %s already exists", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.KYC_ALREADY_EXISTS.toString());
        }

        String mspId = ctx.getClientIdentity().getMSPID();

        long timestamp = ctx.getStub().getTxTimestamp().getEpochSecond();

        KYCRecord record = new KYCRecord(
                id,
                fullName,
                dob,
                nationality,
                documentType,
                documentHash,
                KYCStatus.PENDING,
                mspId,
                timestamp
        );

        String sortedJson = genson.serialize(record);
        ctx.getStub().putStringState(id, sortedJson);

        ctx.getStub().setEvent("KYCCreated", sortedJson.getBytes());

        return record;
    }

    /**
     * Verifies or rejects a KYC record.
     *
     * @param ctx the transaction context
     * @param id the ID of the KYC record
     * @param decision the verification decision (APPROVE or REJECT)
     * @return the updated KYC record
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public KYCRecord VerifyKYC(final Context ctx, final String id, final String decision) {
        String kycJSON = ctx.getStub().getStringState(id);

        if (kycJSON == null || kycJSON.isEmpty()) {
            String errorMessage = String.format("KYC record %s does not exist", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.KYC_NOT_FOUND.toString());
        }

        KYCRecord existingRecord = genson.deserialize(kycJSON, KYCRecord.class);

        KYCStatus newStatus;
        if ("APPROVE".equalsIgnoreCase(decision)) {
            newStatus = KYCStatus.VERIFIED;
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            newStatus = KYCStatus.REJECTED;
        } else {
            String errorMessage = String.format("Invalid decision: %s. Must be APPROVE or REJECT", decision);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.INVALID_DECISION.toString());
        }

        String mspId = ctx.getClientIdentity().getMSPID();
        long timestamp = ctx.getStub().getTxTimestamp().getEpochSecond();

        KYCRecord updatedRecord = new KYCRecord(
                existingRecord.getId(),
                existingRecord.getFullName(),
                existingRecord.getDob(),
                existingRecord.getNationality(),
                existingRecord.getDocumentType(),
                existingRecord.getDocumentHash(),
                newStatus,
                mspId,
                timestamp
        );

        String sortedJson = genson.serialize(updatedRecord);
        ctx.getStub().putStringState(id, sortedJson);

        ctx.getStub().setEvent("KYCVerified", sortedJson.getBytes());

        return updatedRecord;
    }

    /**
     * Retrieves a KYC record by ID.
     *
     * @param ctx the transaction context
     * @param id the ID of the KYC record
     * @return the KYC record
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public KYCRecord GetKYC(final Context ctx, final String id) {
        String kycJSON = ctx.getStub().getStringState(id);

        if (kycJSON == null || kycJSON.isEmpty()) {
            String errorMessage = String.format("KYC record %s does not exist", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.KYC_NOT_FOUND.toString());
        }

        return genson.deserialize(kycJSON, KYCRecord.class);
    }

    /**
     * Retrieves the history of a KYC record for audit purposes.
     *
     * @param ctx the transaction context
     * @param id the ID of the KYC record
     * @return JSON array of historical states
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetKYCHistory(final Context ctx, final String id) {
        ChaincodeStub stub = ctx.getStub();

        if (!KYCExists(ctx, id)) {
            String errorMessage = String.format("KYC record %s does not exist", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.KYC_NOT_FOUND.toString());
        }

        List<KYCHistoryEntry> historyList = new ArrayList<>();
        QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(id);

        for (KeyModification modification : history) {
            KYCRecord record = null;
            if (!modification.isDeleted()) {
                record = genson.deserialize(modification.getStringValue(), KYCRecord.class);
            }

            KYCHistoryEntry entry = new KYCHistoryEntry(
                    modification.getTxId(),
                    modification.getTimestamp().getEpochSecond(),
                    modification.isDeleted(),
                    record
            );
            historyList.add(entry);
        }

        return genson.serialize(historyList);
    }

    /**
     * Updates the status of a KYC record to EXPIRED.
     *
     * @param ctx the transaction context
     * @param id the ID of the KYC record
     * @return the updated KYC record
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public KYCRecord ExpireKYC(final Context ctx, final String id) {
        String kycJSON = ctx.getStub().getStringState(id);

        if (kycJSON == null || kycJSON.isEmpty()) {
            String errorMessage = String.format("KYC record %s does not exist", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, KYCErrors.KYC_NOT_FOUND.toString());
        }

        KYCRecord existingRecord = genson.deserialize(kycJSON, KYCRecord.class);

        String mspId = ctx.getClientIdentity().getMSPID();
        long timestamp = ctx.getStub().getTxTimestamp().getEpochSecond();

        KYCRecord updatedRecord = new KYCRecord(
                existingRecord.getId(),
                existingRecord.getFullName(),
                existingRecord.getDob(),
                existingRecord.getNationality(),
                existingRecord.getDocumentType(),
                existingRecord.getDocumentHash(),
                KYCStatus.EXPIRED,
                mspId,
                timestamp
        );

        String sortedJson = genson.serialize(updatedRecord);
        ctx.getStub().putStringState(id, sortedJson);

        ctx.getStub().setEvent("KYCExpired", sortedJson.getBytes());

        return updatedRecord;
    }

    /**
     * Checks if a KYC record exists.
     *
     * @param ctx the transaction context
     * @param id the ID of the KYC record
     * @return true if the record exists, false otherwise
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean KYCExists(final Context ctx, final String id) {
        String kycJSON = ctx.getStub().getStringState(id);
        return (kycJSON != null && !kycJSON.isEmpty());
    }

    /**
     * Inner class to represent a history entry.
     */
    @org.hyperledger.fabric.contract.annotation.DataType()
    public static final class KYCHistoryEntry {

        @org.hyperledger.fabric.contract.annotation.Property()
        private final String txId;

        @org.hyperledger.fabric.contract.annotation.Property()
        private final long timestamp;

        @org.hyperledger.fabric.contract.annotation.Property()
        private final boolean isDeleted;

        @org.hyperledger.fabric.contract.annotation.Property()
        private final KYCRecord record;

        public KYCHistoryEntry(
                @com.owlike.genson.annotation.JsonProperty("txId") final String txId,
                @com.owlike.genson.annotation.JsonProperty("timestamp") final long timestamp,
                @com.owlike.genson.annotation.JsonProperty("isDeleted") final boolean isDeleted,
                @com.owlike.genson.annotation.JsonProperty("record") final KYCRecord record) {
            this.txId = txId;
            this.timestamp = timestamp;
            this.isDeleted = isDeleted;
            this.record = record;
        }

        public String getTxId() {
            return txId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public KYCRecord getRecord() {
            return record;
        }
    }
}
