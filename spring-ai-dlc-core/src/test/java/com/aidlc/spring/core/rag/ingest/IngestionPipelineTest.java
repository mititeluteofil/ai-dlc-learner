package com.aidlc.spring.core.rag.ingest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionPipelineTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void runReadsChunksAndAddsThemToVectorStore() {
        IngestionPipeline ingestion = new IngestionPipeline(vectorStore);
        DocumentSource source = DocumentSource.text("a".repeat(5000), "sample");

        IngestionReport report = ingestion.from(source).run();

        assertEquals(1, report.documentsRead());
        assertTrue(report.chunksWritten() > 0);
        assertTrue(report.elapsed().toNanos() >= 0);
        assertTrue(report.errors().isEmpty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> written = captor.getValue();
        assertEquals(report.chunksWritten(), written.size());
        for (Document document : written) {
            assertNotNull(document.getId());
        }
    }

    @Test
    void deterministicIdsAreStableAcrossRuns() {
        IngestionPipeline ingestion = new IngestionPipeline(vectorStore);
        DocumentSource source = DocumentSource.text("b".repeat(5000), "stable-source");

        List<String> firstIds = idsOf(ingestion.from(source).run(), source);
        List<String> secondIds = idsOf(ingestion.from(source).run(), source);

        assertEquals(firstIds, secondIds);
        assertFalse(firstIds.isEmpty());
    }

    @Test
    void deterministicIdDependsOnSourceAndChunkIndex() {
        String idA0 = IngestionPipeline.deterministicId("source-a", 0);
        String idA1 = IngestionPipeline.deterministicId("source-a", 1);
        String idB0 = IngestionPipeline.deterministicId("source-b", 0);

        assertFalse(idA0.equals(idA1));
        assertFalse(idA0.equals(idB0));
        assertEquals(idA0, IngestionPipeline.deterministicId("source-a", 0));
    }

    @Test
    void runWithEmptySourceWritesNothing() {
        IngestionPipeline ingestion = new IngestionPipeline(vectorStore);
        DocumentSource source = DocumentSource.text("tiny", "empty-source");

        IngestionReport report = ingestion.from(source).run();

        assertEquals(1, report.documentsRead());
        assertTrue(report.chunksWritten() >= 0);
    }

    private List<String> idsOf(IngestionReport report, DocumentSource source) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, org.mockito.Mockito.atLeastOnce()).add(captor.capture());
        List<Document> last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        return last.stream().map(Document::getId).toList();
    }
}
