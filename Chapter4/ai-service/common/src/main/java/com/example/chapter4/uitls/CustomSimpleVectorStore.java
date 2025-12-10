package com.example.chapter4.uitls;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStoreContent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class CustomSimpleVectorStore extends SimpleVectorStore {

    private final double similarityThreshold;

    public CustomSimpleVectorStore(EmbeddingModel embeddingModel, double similarityThreshold) {
        super(SimpleVectorStore.builder(embeddingModel));
        this.similarityThreshold = similarityThreshold;
    }

    //回调接口
    public interface MemoryListener {
        void onMemoryRetrieved(List<Document> docs);
    }

    private MemoryListener listener;

    public void setMemoryListener(MemoryListener listener) {
        this.listener = listener;
    }

    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        Predicate<SimpleVectorStoreContent> filterPredicate = content -> true;

        if (request.hasFilterExpression()) {
            filterPredicate = this::matchFilter;
        }

        float[] queryEmbedding = this.embeddingModel.embed(request.getQuery());

        List<Document> result = this.store.values()
                .stream()
                .filter(filterPredicate)
                .map(content -> content.toDocument(
                        EmbeddingMath.cosineSimilarity(queryEmbedding, content.getEmbedding())))
                //加入 similarityThreshold 过滤
                .peek(doc -> System.out.println(
                        "DocumentId=" + doc.getId() +
                                "  Content=" + doc.getText() +
                                "  Score=" + doc.getScore()))
                .filter(doc -> doc.getScore() >= similarityThreshold)
                .sorted(Comparator.comparing(Document::getScore).reversed())
                .limit(request.getTopK())
                .toList();

        //触发回调
        if (listener != null) {
            listener.onMemoryRetrieved(result);
        }

        return result;
    }

    private boolean matchFilter(SimpleVectorStoreContent content) {
        return true;
    }
}