package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.entity.KnowledgeDocument;
import com.nexlora.nexbusiness.repository.KnowledgeDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SecureDocService {

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    public String processAndStorePdf(MultipartFile file, String namespace, String accessRole) {
        try {
            // 1. Generate unique Document ID
            String docId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String fileName = file.getOriginalFilename();

            // 2. Extract Text using Apache PDFBox 3.0
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                int totalPages = document.getNumberOfPages();
                
                // For demonstration, we extract text (can be chunked page-by-page later)
                String extractedText = stripper.getText(document);
                System.out.println("Extracted " + totalPages + " pages from " + fileName);
            }

            // 3. Save Metadata to MySQL Database
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setDocId(docId);
            doc.setFileName(fileName != null ? fileName : "unknown.pdf");
            doc.setProjectNamespace(namespace);
            doc.setAccessRole(KnowledgeDocument.AccessRole.valueOf(accessRole.toUpperCase()));
            doc.setVersion(1);

            knowledgeDocumentRepository.save(doc);

            return "Successfully uploaded and indexed document with ID: " + docId;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to process PDF: " + e.getMessage();
        }
    }
}
