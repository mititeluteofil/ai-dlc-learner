# Java vs. Python for AI development

## Thesis

Python owns AI velocity: LangChain/LlamaIndex give a one-line `RetrievalQA.from_chain_type(...)`
that goes from zero to a working RAG prototype faster than almost anything in Java. That speed
is real, and it's why most AI prototyping happens in Python.

But "fastest to a notebook" and "what your team runs in production" are different questions.
Most backend teams already run Java/Spring in production — their auth, observability,
deployment pipelines, and on-call runbooks are built around it. Rewriting a working AI feature
into Python to ship it, or running a second-language microservice just for the LLM call, is a
tax that compounds over the life of the system. **This template's bet: shrink the Python
prototyping-speed advantage enough that a Java/Spring team can build (and own) AI features in
their existing stack**, using Spring AI 2.0 plus the thin facades in `spring-ai-dlc-core`.

## Side by side: RAG query

**Python (LangChain)** — roughly 15 lines to go from a question to an answer with retrieval:

```python
from langchain_community.vectorstores import PGVector
from langchain_community.embeddings import OllamaEmbeddings
from langchain_community.chat_models import ChatOllama
from langchain.chains import RetrievalQA

embeddings = OllamaEmbeddings(model="nomic-embed-text")
vectorstore = PGVector(
    connection_string="postgresql://aidlc:aidlc@localhost:5432/aidlc",
    embedding_function=embeddings,
    collection_name="vector_store",
)
llm = ChatOllama(model="qwen3:4b")

qa = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=vectorstore.as_retriever(search_kwargs={"k": 4}),
    return_source_documents=True,
)

result = qa.invoke({"query": "How do I switch to OpenAI?"})
print(result["result"])
for doc in result["source_documents"]:
    print(doc.metadata["source"], doc.page_content[:200])
```

**Java (this repo)** — `RagController` from `demo-app/src/main/java/com/aidlc/spring/demo/rag/RagController.java`,
~8 lines including the endpoint:

```java
@RestController
public class RagController {

    private final RagPipeline ragPipeline;

    @PostMapping("/api/rag/ask")
    public RagAnswer ask(@RequestBody AskRequest request) {
        return ragPipeline.ask(request.question());
    }
}
```

`RagPipeline` and `IngestionPipeline` are autoconfigured beans (`AiDlcRagAutoConfiguration`):
the vector store, embeddings model, chat model, retrieval advisor, top-K, similarity threshold,
and citation extraction are all wired from `application.yml` + `ai.dlc.*` properties — nothing
the controller needs to construct. `RagAnswer` is a record with `answer` and
`List<Citation>` (source, snippet, score), so citations are typed, not dict lookups.

## Ecosystem maturity

Python's LLM ecosystem is broader and moves faster: new model providers, retrieval techniques,
and evaluation tooling typically land in LangChain/LlamaIndex first, often as a `pip install`
away. Spring AI 2.0 covers the core surface — chat models, embeddings, vector stores, tool
calling, RAG advisors, structured output, observability — across the major providers
(OpenAI, Anthropic, Ollama, etc.), but the long tail of niche integrations is smaller and
newer. For teams whose needs sit in that core surface (most production RAG/agent apps), the
gap is mostly closed; for cutting-edge research-style integrations, Python still leads.

## Typing and refactorability

This is where Java pulls ahead. `RagAnswer`, `Citation`, `IngestionReport`, `AgentResult`,
`AgentDefinition` — every cross-cutting data shape in `spring-ai-dlc-core` is a `record` with
named, typed fields. Renaming a field is a compiler error at every call site, not a runtime
`KeyError` three services downstream. `Workflow` is a one-method functional interface, so
`ChainWorkflow`, `ParallelWorkflow`, `RoutingWorkflow`, and `EvaluatorOptimizerWorkflow` are all
interchangeable and IDE-navigable. Python's dynamic typing (even with type hints and Pydantic)
catches far less of this at build time — especially across the dict-shaped `context`/`metadata`
objects that pervade LangChain.

## Operations

- **Single executable artifact**: `./gradlew :demo-app:bootJar` produces one fat jar with an
  embedded server — `java -jar demo-app.jar` and you're running. No virtualenv, no `pip` lock
  file drift, no separate ASGI server.
- **Virtual threads**: `ParallelWorkflow` (`spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/workflow/ParallelWorkflow.java`)
  runs each workflow step on `Executors.newVirtualThreadPerTaskExecutor()` — cheap,
  high-concurrency I/O-bound fan-out (e.g. calling multiple LLM endpoints at once) without
  thread-pool tuning or `asyncio` event-loop considerations.
- **GraalVM native image potential**: Spring Boot 4's AOT processing supports
  `native-image` builds for fast-starting, low-memory-footprint deployments — relevant for
  scale-to-zero or CLI-style agent workloads. Not yet exercised by this template, but the
  Boot/Java toolchain makes it a configuration step rather than an architectural change.
- **Observability**: Spring Boot Actuator (`management.endpoints.web.exposure`, already
  configured in `demo-app/src/main/resources/application.yml`) gives health/metrics endpoints
  for free, integrating with the same Micrometer/Prometheus/tracing stack a Java team already
  runs for its non-AI services.

## Performance characteristics

The JVM's JIT and mature GC make Java's steady-state throughput and latency for the
non-model-call parts of a request (request handling, retrieval post-processing, JSON
(de)serialization via Jackson 3) competitive with or better than CPython. The dominant cost in
most RAG/agent requests is the LLM/embedding call itself — both languages are bottlenecked on
the same network round trip to Ollama/OpenAI/Anthropic, so this is rarely the deciding factor.
Where it matters more: cold-start latency (JVM startup vs. Python interpreter startup — both
templates are not "instant", but JVM warm-up under load is well understood and tunable) and
memory footprint per instance when running many replicas.

## Team fit

If your team already writes Spring services — controllers, `@ConfigurationProperties`,
Actuator, Gradle/Maven CI — adding an AI feature with this template means **the same
language, the same build, the same deployment pipeline, the same on-call runbooks**. There's
no context-switch to a Python service with its own dependency management, packaging, and
observability story, and no "the AI part is a black box maintained by a different team in a
different language" split. The cost of that consistency is accepting that some
Python-ecosystem-first integrations will take longer to arrive in Spring AI — a trade-off this
template is betting is worth it for teams whose core stack is already Java.
