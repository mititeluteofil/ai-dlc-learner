package com.aidlc.spring.demo.rag;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aidlc.spring.core.rag.ingest.IngestionPipeline;
import com.aidlc.spring.core.rag.ingest.IngestionReport;
import com.aidlc.spring.core.rag.query.Citation;
import com.aidlc.spring.core.rag.query.RagAnswer;
import com.aidlc.spring.core.rag.query.RagPipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagPipeline ragPipeline;

    @MockitoBean
    private IngestionPipeline ingestionPipeline;

    @Test
    void askReturnsAnswerAndCitationsFromTheRagPipeline() throws Exception {
        RagAnswer answer = new RagAnswer(
                "AI-DLC is an AI-assisted development lifecycle.",
                List.of(new Citation("rag-corpus/implementation-plan.md", "snippet", 0.9)));
        when(ragPipeline.ask("What is AI-DLC?")).thenReturn(answer);

        MvcResult result = mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is AI-DLC?\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("AI-DLC is an AI-assisted development lifecycle.")
                .contains("rag-corpus/implementation-plan.md");
    }

    @Test
    void ingestTriggersTheIngestionPipelineAndReturnsTheReport() throws Exception {
        IngestionPipeline.Run run = org.mockito.Mockito.mock(IngestionPipeline.Run.class);
        when(ingestionPipeline.from(any())).thenReturn(run);
        when(run.withChunking(any())).thenReturn(run);
        when(run.run()).thenReturn(new IngestionReport(2, 10, Duration.ofMillis(50), List.of()));

        mockMvc.perform(post("/api/rag/ingest"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"documentsRead\":2,\"chunksWritten\":10,\"errors\":[]}"));
    }
}
