package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /** Rows: [content, page_number, file_name]. Access filter lives in SQL, not in the prompt. */
    @Query(value = """
        SELECT c.content, c.page_number, d.file_name
        FROM document_chunks c
        JOIN knowledge_documents d ON d.doc_id = c.doc_id
        WHERE d.project_namespace = :ns
          AND (:isAdmin = TRUE OR d.access_role = 'PUBLIC')
          AND MATCH(c.content) AGAINST (:q IN NATURAL LANGUAGE MODE)
        ORDER BY MATCH(c.content) AGAINST (:q IN NATURAL LANGUAGE MODE) DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> search(@Param("ns") String namespace,
                          @Param("q") String question,
                          @Param("isAdmin") boolean isAdmin);
}
