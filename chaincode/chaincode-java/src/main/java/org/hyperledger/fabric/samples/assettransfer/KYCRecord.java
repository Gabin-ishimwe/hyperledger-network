/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class KYCRecord {

    @Property()
    private final String id;

    @Property()
    private final String fullName;

    @Property()
    private final String dob;

    @Property()
    private final String nationality;

    @Property()
    private final String documentType;

    @Property()
    private final String documentHash;

    @Property()
    private final KYCStatus status;

    @Property()
    private final String issuerMSP;

    @Property()
    private final long updatedAt;

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDob() {
        return dob;
    }

    public String getNationality() {
        return nationality;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public KYCStatus getStatus() {
        return status;
    }

    public String getIssuerMSP() {
        return issuerMSP;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public KYCRecord(@JsonProperty("id") final String id,
                     @JsonProperty("fullName") final String fullName,
                     @JsonProperty("dob") final String dob,
                     @JsonProperty("nationality") final String nationality,
                     @JsonProperty("documentType") final String documentType,
                     @JsonProperty("documentHash") final String documentHash,
                     @JsonProperty("status") final KYCStatus status,
                     @JsonProperty("issuerMSP") final String issuerMSP,
                     @JsonProperty("updatedAt") final long updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.dob = dob;
        this.nationality = nationality;
        this.documentType = documentType;
        this.documentHash = documentHash;
        this.status = status;
        this.issuerMSP = issuerMSP;
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        KYCRecord other = (KYCRecord) obj;

        return Objects.deepEquals(
                new String[] {getId(), getFullName(), getDob(), getNationality(),
                        getDocumentType(), getDocumentHash(), getIssuerMSP()},
                new String[] {other.getId(), other.getFullName(), other.getDob(), other.getNationality(),
                        other.getDocumentType(), other.getDocumentHash(), other.getIssuerMSP()})
                && Objects.equals(getStatus(), other.getStatus())
                && getUpdatedAt() == other.getUpdatedAt();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFullName(), getDob(), getNationality(),
                getDocumentType(), getDocumentHash(), getStatus(), getIssuerMSP(), getUpdatedAt());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + " [id=" + id
                + ", fullName=" + fullName
                + ", dob=" + dob
                + ", nationality=" + nationality
                + ", documentType=" + documentType
                + ", documentHash=" + documentHash
                + ", status=" + status
                + ", issuerMSP=" + issuerMSP
                + ", updatedAt=" + updatedAt + "]";
    }
}
