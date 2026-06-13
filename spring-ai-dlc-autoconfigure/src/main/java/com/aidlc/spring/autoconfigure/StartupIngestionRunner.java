package com.aidlc.spring.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.aidlc.spring.core.rag.ingest.ChunkingSpec;
import com.aidlc.spring.core.rag.ingest.DocumentSource;
import com.aidlc.spring.core.rag.ingest.IngestionPipeline;
import com.aidlc.spring.core.rag.ingest.IngestionReport;

/**
 * Runs {@link IngestionPipeline} against a configured classpath location pattern on startup,
 * when {@code ai.dlc.rag.ingest-on-startup.enabled=true}.
 */
@AutoConfiguration(after = AiDlcRagAutoConfiguration.class)
@ConditionalOnBean(IngestionPipeline.class)
@EnableConfigurationProperties(AiDlcProperties.class)
public class StartupIngestionRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupIngestionRunner.class);

    @Bean
    @ConditionalOnProperty(prefix = "ai.dlc.rag.ingest-on-startup", name = "enabled", havingValue = "true")
    public ApplicationRunner aiDlcStartupIngestionRunner(IngestionPipeline ingestionPipeline, AiDlcProperties properties) {
        return new IngestionApplicationRunner(ingestionPipeline, properties);
    }

    static final class IngestionApplicationRunner implements ApplicationRunner {

        private final IngestionPipeline ingestionPipeline;
        private final AiDlcProperties properties;

        IngestionApplicationRunner(IngestionPipeline ingestionPipeline, AiDlcProperties properties) {
            this.ingestionPipeline = ingestionPipeline;
            this.properties = properties;
        }

        @Override
        public void run(ApplicationArguments args) {
            AiDlcProperties.Rag rag = properties.rag();
            String pattern = rag.ingestOnStartup().locationPattern();
            ChunkingSpec chunkingSpec = new ChunkingSpec(rag.chunkSize(), rag.chunkOverlap());

            IngestionReport report = ingestionPipeline.from(DocumentSource.classpath(pattern))
                    .withChunking(chunkingSpec)
                    .run();

            log.info(
                    "AI-DLC startup ingestion of '{}' complete: {} document(s) read, {} chunk(s) written, "
                            + "elapsed {}, errors {}",
                    pattern, report.documentsRead(), report.chunksWritten(), report.elapsed(), report.errors());
        }
    }
}
