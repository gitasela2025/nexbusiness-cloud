package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private String docId;

    private int pageNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
