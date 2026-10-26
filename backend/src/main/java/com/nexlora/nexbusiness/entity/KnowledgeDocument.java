package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    @Id
    @Column(name = "doc_id", length = 100)
    private String docId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "project_namespace", nullable = false, length = 100)
    private String projectNamespace;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role", nullable = false)
    private AccessRole accessRole = AccessRole.PUBLIC;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum AccessRole {
        PUBLIC, ADMIN_ONLY
    }
}
