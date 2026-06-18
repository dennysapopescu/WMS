package com.warehouse.wms.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warehouse.wms.model.Product;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfService {

    public void export(HttpServletResponse response, List<Product> products) throws IOException {
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Obținem numele utilizatorului curent pentru raport
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        // Header raport profesional
        document.add(new Paragraph("RAPORT INVENTAR WMS ENTERPRISE").setBold().setFontSize(18));
        document.add(new Paragraph("Data generarii: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));
        document.add(new Paragraph("Generat de operator: " + currentUser).setItalic().setFontSize(10));
        document.add(new Paragraph("\n"));

        // Tabel cu 6 coloane
        float[] columnWidths = {120, 80, 60, 80, 80, 80};
        Table table = new Table(UnitValue.createPointArray(columnWidths)).useAllAvailableWidth();

        // Header Tabel
        table.addHeaderCell("Produs");
        table.addHeaderCell("SKU");
        table.addHeaderCell("Stoc");
        table.addHeaderCell("Pret");
        table.addHeaderCell("Locatie");
        table.addHeaderCell("Cod QR");

        for (Product p : products) {
            table.addCell(p.getName() != null ? p.getName() : "-");
            table.addCell(p.getSku() != null ? p.getSku() : "-");
            table.addCell(String.valueOf(p.getQuantity()));
            table.addCell(p.getPrice() + " RON");
            table.addCell(p.getLocation() != null ? p.getLocation().getCode() : "-");

            // Generare și adăugare QR Code
            if (p.getSku() != null) {
                try {
                    byte[] qrBytes = generateQRCodeBytes(p.getSku());
                    ImageData imageData = ImageDataFactory.create(qrBytes);
                    Image qrImage = new Image(imageData).setWidth(50);
                    table.addCell(qrImage);
                } catch (Exception e) {
                    table.addCell("Eroare QR");
                }
            } else {
                table.addCell("-");
            }
        }

        document.add(table);

        // Footer pagină
        document.add(new Paragraph("\n\nSemnatura Controlor Stoc: ___________________").setFontSize(10));

        document.close();
    }

    private byte[] generateQRCodeBytes(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 150, 150);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return baos.toByteArray();
        }
    }
}