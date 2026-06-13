package com.aidlc.spring.core.rag.ingest;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Describes where ingestion content comes from and how to read it.
 *
 * <p>Each variant exposes a stable {@link #identifier()} used to derive deterministic
 * document ids, and a {@link #read()} that produces the raw (pre-split) documents.
 */
public sealed interface DocumentSource {

    String identifier();

    List<Document> read();

    static DocumentSource classpath(String locationPattern) {
        return new ClasspathSource(locationPattern);
    }

    static DocumentSource path(String filePath) {
        return new PathSource(filePath);
    }

    static DocumentSource url(String url) {
        return new UrlSource(url);
    }

    static DocumentSource text(String content) {
        return text(content, "inline-text");
    }

    static DocumentSource text(String content, String sourceName) {
        return new TextSource(content, sourceName);
    }

    /**
     * Resolves a classpath ant-style glob (e.g. {@code rag-corpus/**}/*.md}) and reads
     * every matching resource with {@link TextReader}.
     */
    record ClasspathSource(String locationPattern) implements DocumentSource {

        @Override
        public String identifier() {
            return "classpath:" + locationPattern;
        }

        @Override
        public List<Document> read() {
            var resolver = new PathMatchingResourcePatternResolver();
            try {
                Resource[] resources = resolver.getResources("classpath*:" + locationPattern);
                List<Document> documents = new ArrayList<>();
                for (Resource resource : resources) {
                    documents.addAll(new TextReader(resource).read());
                }
                return documents;
            }
            catch (IOException e) {
                throw new DocumentSourceException("Failed to resolve classpath resources for " + locationPattern, e);
            }
        }
    }

    /** Reads a single file from the filesystem with {@link TextReader}. */
    record PathSource(String filePath) implements DocumentSource {

        @Override
        public String identifier() {
            return "path:" + filePath;
        }

        @Override
        public List<Document> read() {
            return new TextReader(new FileSystemResource(filePath)).read();
        }
    }

    /** Reads a remote resource with {@link TextReader}. */
    record UrlSource(String url) implements DocumentSource {

        @Override
        public String identifier() {
            return "url:" + url;
        }

        @Override
        public List<Document> read() {
            try {
                return new TextReader(new UrlResource(URI.create(url))).read();
            }
            catch (Exception e) {
                throw new DocumentSourceException("Failed to read URL resource " + url, e);
            }
        }
    }

    /** Wraps an in-memory string as a single document. */
    record TextSource(String content, String sourceName) implements DocumentSource {

        @Override
        public String identifier() {
            return "text:" + sourceName;
        }

        @Override
        public List<Document> read() {
            return List.of(Document.builder()
                .text(content)
                .metadata(TextReader.SOURCE_METADATA, sourceName)
                .build());
        }
    }

    class DocumentSourceException extends RuntimeException {
        public DocumentSourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
