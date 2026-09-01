package com.warehouse.wms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    void generateQRCode_returnsNonEmptyPngBytesForValidInput() {
        byte[] result = qrCodeService.generateQRCode("Product SKU: TEST-001");

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Standard PNG signature header: 0x89 'P' 'N' 'G'
        assertEquals((byte) 0x89, result[0]);
        assertEquals('P', result[1]);
        assertEquals('N', result[2]);
        assertEquals('G', result[3]);
    }

    @Test
    void generateQRCode_producesDifferentOutputForDifferentInput() {
        byte[] first = qrCodeService.generateQRCode("LOC:A-01");
        byte[] second = qrCodeService.generateQRCode("LOC:B-02");

        assertFalse(java.util.Arrays.equals(first, second));
    }

    @Test
    void generateQRCode_handlesEmptyStringGracefully() {
        // Should not throw unhandled exception — gracefully caught internally
        byte[] result = qrCodeService.generateQRCode("");

        // ZXing can generate a valid matrix for an empty string or return null on internal error
        assertDoesNotThrow(() -> qrCodeService.generateQRCode(""));
    }

}
