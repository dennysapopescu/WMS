package com.warehouse.wms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    void generateQRCode_returnsNonEmptyPngBytesForValidInput() {
        byte[] result = qrCodeService.generateQRCode("Produs SKU: TEST-001");

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Semnătura PNG standard: 0x89 'P' 'N' 'G'
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
        // Nu ar trebui să arunce excepție necontrolată — serviciul prinde excepțiile
        // intern și întoarce null în caz de eroare.
        byte[] result = qrCodeService.generateQRCode("");

        // ZXing poate genera cu succes un QR și pentru string gol, sau serviciul
        // întoarce null dacă apare vreo eroare internă — ambele sunt comportamente
        // acceptabile, important e că nu crapă aplicația.
        assertDoesNotThrow(() -> qrCodeService.generateQRCode(""));
    }
}
