package com.warehouse.wms.controller;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.warehouse.wms.model.Product;
import com.warehouse.wms.repository.ProductRepository;
import com.warehouse.wms.service.QrCodeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping(value = "/qr/{sku}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getProductQr(@PathVariable String sku) {
        return qrCodeService.generateQRCode("Produs SKU: " + sku);
    }

    @GetMapping("/export-pdf")
    public void exportToPDF(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=etichete_qr.pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Lista Etichete QR - Depozit").setBold().setFontSize(18));

        List<Product> products = productRepository.findAll();
        float[] columnWidths = {200f, 200f};
        Table table = new Table(columnWidths);

        for (Product p : products) {
            table.addCell(new Paragraph(p.getName() + "\nSKU: " + p.getSku()));

            // Generăm imaginea QR pentru PDF
            byte[] qrBytes = qrCodeService.generateQRCode("Produs SKU: " + p.getSku());
            Image qrImage = new Image(ImageDataFactory.create(qrBytes)).setWidth(100);
            table.addCell(qrImage);
        }

        document.add(table);
        document.close();
    }

    @GetMapping(value = "/qr-location/{code}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getLocationQRCode(@PathVariable String code) {
        return qrCodeService.generateQRCode("LOC:" + code);
    }
}