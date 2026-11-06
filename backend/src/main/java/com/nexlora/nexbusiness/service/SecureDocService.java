package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.entity.DocumentChunk;
import com.nexlora.nexbusiness.entity.KnowledgeDocument;
import com.nexlora.nexbusiness.repository.DocumentChunkRepository;
import com.nexlora.nexbusiness.repository.KnowledgeDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SecureDocService {

    static final int CHUNK_SIZE = 800;
    static final int OVERLAP = 100;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public String processAndStorePdf(MultipartFile file, String namespace, String accessRole) {
        KnowledgeDocument.AccessRole role;
        try {
            role = KnowledgeDocument.AccessRole.valueOf(accessRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid accessRole. Use PUBLIC or ADMIN_ONLY.";
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String docId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf";

            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setDocId(docId);
            doc.setFileName(fileName);
            doc.setProjectNamespace(namespace);
            doc.setAccessRole(role);
            doc.setVersion(1);
            knowledgeDocumentRepository.save(doc);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator(" ");
            int totalPages = document.getNumberOfPages();
            int chunkCount = 0;

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).replaceAll("\\s+", " ").trim();
                for (String piece : split(text)) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocId(docId);
                    chunk.setPageNumber(page);
                    chunk.setContent(piece);
                    chunkRepository.save(chunk);
                    chunkCount++;
                }
            }

            return "Uploaded " + fileName + " as " + docId + ": " + totalPages
                    + " pages, " + chunkCount + " chunks indexed.";

        } catch (IOException e) {
            return "Failed to process PDF: " + e.getMessage();
        }
    }

    public record Source(String fileName, int page, String text) {}
    public record AskResult(String answer, List<Source> sources) {}

    /** isAdmin is decided by the caller. The access filter is applied inside the SQL query. */
    public AskResult ask(String namespace, String question, boolean isAdmin) {
        List<Object[]> rows = chunkRepository.search(namespace, question, isAdmin);
        List<Source> sources = new ArrayList<>();
        for (Object[] r : rows) {
            sources.add(new Source((String) r[2], ((Number) r[1]).intValue(), (String) r[0]));
        }
        if (sources.isEmpty()) {
            return new AskResult("I could not find this in the documents you have access to.", sources);
        }

        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            Source src = sources.get(i);
            ctx.append("[").append(i + 1).append("] (").append(src.fileName())
               .append(", page ").append(src.page()).append(") ").append(src.text()).append("\n");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "Answer the question using ONLY the numbered sources below. "
                + "Cite sources like [1] after each fact. "
                + "If the sources do not contain the answer, say you could not find it. "
                + "Never use outside knowledge.\n\nSOURCES:\n" + ctx));
            messages.add(Map.of("role", "user", "content", question));

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.1);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                groqApiUrl, new HttpEntity<>(body, headers), Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            Object content = msg.get("content");
            return new AskResult(content == null ? "" : content.toString(), sources);
        } catch (Exception e) {
            return new AskResult("Failed to get an answer: " + e.getMessage(), sources);
        }
    }

    /** Splits text into ~800-char pieces with 100-char overlap. */
    static List<String> split(String text) {
        List<String> pieces = new ArrayList<>();
        if (text.isEmpty()) return pieces;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            pieces.add(text.substring(start, end));
            if (end == text.length()) break;
            start = end - OVERLAP;
        }
        return pieces;
    }
}
