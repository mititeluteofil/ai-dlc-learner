package com.aidlc.spring.core.rag.query;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * Collapses Spring AI's {@code ChatClient} + retrieval-advisor chain into a single
 * {@code rag.ask("question")} call.
 *
 * <p>By default this wraps the vector store in a {@link QuestionAnswerAdvisor}. When
 * {@link Builder#enableQueryRewrite()} is set, a {@link RetrievalAugmentationAdvisor}
 * with a {@link RewriteQueryTransformer} is used instead so the user's question is
 * rewritten into a better search query before retrieval.
 */
public final class RagPipeline {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;
    private final PromptTemplate promptTemplate;
    private final Filter.Expression filterExpression;
    private final boolean queryRewriteEnabled;

    private RagPipeline(Builder builder) {
        this.chatClientBuilder = builder.chatClientBuilder;
        this.vectorStore = builder.vectorStore;
        this.topK = builder.topK;
        this.similarityThreshold = builder.similarityThreshold;
        this.promptTemplate = builder.promptTemplate;
        this.filterExpression = builder.filterExpression;
        this.queryRewriteEnabled = builder.queryRewriteEnabled;
    }

    public static Builder builder(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        return new Builder(chatClientBuilder, vectorStore);
    }

    public RagAnswer ask(String question) {
        Objects.requireNonNull(question, "question must not be null");

        ChatClient.ChatClientRequestSpec request = chatClientBuilder.build()
            .prompt()
            .user(question)
            .advisors(queryRewriteEnabled ? retrievalAugmentationAdvisor() : questionAnswerAdvisor());

        ChatClientResponse response = request.call().chatClientResponse();
        String answer = response.chatResponse().getResult().getOutput().getText();
        List<Citation> citations = extractCitations(response);
        return new RagAnswer(answer, citations);
    }

    private QuestionAnswerAdvisor questionAnswerAdvisor() {
        QuestionAnswerAdvisor.Builder advisorBuilder = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(searchRequest());
        if (promptTemplate != null) {
            advisorBuilder.promptTemplate(promptTemplate);
        }
        return advisorBuilder.build();
    }

    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
        VectorStoreDocumentRetriever.Builder retrieverBuilder = VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .topK(topK)
            .similarityThreshold(similarityThreshold);
        if (filterExpression != null) {
            retrieverBuilder.filterExpression(filterExpression);
        }

        RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .build();

        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(rewriteQueryTransformer)
            .documentRetriever(retrieverBuilder.build())
            .build();
    }

    private SearchRequest searchRequest() {
        SearchRequest.Builder builder = SearchRequest.builder().topK(topK).similarityThreshold(similarityThreshold);
        if (filterExpression != null) {
            builder.filterExpression(filterExpression);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<Citation> extractCitations(ChatClientResponse response) {
        Object retrieved = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (retrieved == null) {
            retrieved = response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        }
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        return documents.stream()
            .filter(Document.class::isInstance)
            .map(Document.class::cast)
            .map(this::toCitation)
            .toList();
    }

    private Citation toCitation(Document document) {
        Object source = document.getMetadata().get("source");
        String sourceValue = source != null ? source.toString() : document.getId();
        String text = document.getText();
        String snippet = text != null && text.length() > 200 ? text.substring(0, 200) : text;
        return new Citation(sourceValue, snippet, document.getScore());
    }

    /** Fluent builder for {@link RagPipeline}. */
    public static final class Builder {

        private final ChatClient.Builder chatClientBuilder;
        private final VectorStore vectorStore;
        private int topK = SearchRequest.DEFAULT_TOP_K;
        private double similarityThreshold = SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL;
        private PromptTemplate promptTemplate;
        private Filter.Expression filterExpression;
        private boolean queryRewriteEnabled = false;

        private Builder(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
            this.chatClientBuilder = Objects.requireNonNull(chatClientBuilder, "chatClientBuilder must not be null");
            this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder similarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder filterExpression(Filter.Expression filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        public Builder enableQueryRewrite() {
            this.queryRewriteEnabled = true;
            return this;
        }

        public RagPipeline build() {
            return new RagPipeline(this);
        }
    }
}
