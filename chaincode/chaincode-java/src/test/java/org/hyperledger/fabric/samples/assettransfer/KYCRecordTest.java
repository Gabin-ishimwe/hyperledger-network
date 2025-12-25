/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public final class KYCRecordTest {

    @Nested
    class Equality {

        @Test
        public void isReflexive() {
            KYCRecord record = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);

            assertThat(record).isEqualTo(record);
        }

        @Test
        public void isSymmetric() {
            KYCRecord recordA = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);
            KYCRecord recordB = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);

            assertThat(recordA).isEqualTo(recordB);
            assertThat(recordB).isEqualTo(recordA);
        }

        @Test
        public void isTransitive() {
            KYCRecord recordA = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);
            KYCRecord recordB = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);
            KYCRecord recordC = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);

            assertThat(recordA).isEqualTo(recordB);
            assertThat(recordB).isEqualTo(recordC);
            assertThat(recordA).isEqualTo(recordC);
        }

        @Test
        public void handlesInequality() {
            KYCRecord recordA = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);
            KYCRecord recordB = new KYCRecord("kyc2", "Jane Smith", "1985-05-20",
                    "UK", "Driver's License", "QmHash456", KYCStatus.VERIFIED, "Org2MSP", 1700000100L);

            assertThat(recordA).isNotEqualTo(recordB);
        }

        @Test
        public void handlesOtherObjects() {
            KYCRecord recordA = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);
            String recordB = "not a KYCRecord";

            assertThat(recordA).isNotEqualTo(recordB);
        }

        @Test
        public void handlesNull() {
            KYCRecord record = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                    "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);

            assertThat(record).isNotEqualTo(null);
        }
    }

    @Test
    public void toStringIdentifiesKYCRecord() {
        KYCRecord record = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                "USA", "Passport", "QmHash123", KYCStatus.PENDING, "Org1MSP", 1700000000L);

        assertThat(record.toString()).contains("id=kyc1");
        assertThat(record.toString()).contains("fullName=John Doe");
        assertThat(record.toString()).contains("status=PENDING");
    }

    @Test
    public void gettersReturnCorrectValues() {
        KYCRecord record = new KYCRecord("kyc1", "John Doe", "1990-01-15",
                "USA", "Passport", "QmHash123", KYCStatus.VERIFIED, "Org1MSP", 1700000000L);

        assertThat(record.getId()).isEqualTo("kyc1");
        assertThat(record.getFullName()).isEqualTo("John Doe");
        assertThat(record.getDob()).isEqualTo("1990-01-15");
        assertThat(record.getNationality()).isEqualTo("USA");
        assertThat(record.getDocumentType()).isEqualTo("Passport");
        assertThat(record.getDocumentHash()).isEqualTo("QmHash123");
        assertThat(record.getStatus()).isEqualTo(KYCStatus.VERIFIED);
        assertThat(record.getIssuerMSP()).isEqualTo("Org1MSP");
        assertThat(record.getUpdatedAt()).isEqualTo(1700000000L);
    }
}
