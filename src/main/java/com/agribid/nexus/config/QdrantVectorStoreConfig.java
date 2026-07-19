package com.agribid.nexus.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI's Qdrant auto-configuration already builds a VectorStore
 * bean from spring.ai.vectorstore.qdrant.* properties and backs off
 * (@ConditionalOnMissingBean) if this application defines its own —
 * which is exactly what we do here, purely to force
 * initializeSchema(true) explicitly and document the dev-vs-prod
 * distinction below, rather than relying on whatever the starter's
 * own default happens to be.
 *
 * initializeSchema(true) is appropriate for local dev against the
 * docker-compose Qdrant instance (auto-creates the collection on
 * boot). Set spring.ai.vectorstore.qdrant.initialize-schema=false in
 * application-prod.properties once the collection has been
 * provisioned once, deliberately, rather than implicitly on every
 * production boot.
 */
@Configuration
public class QdrantVectorStoreConfig {

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.initialize-schema:true}")
    private boolean initializeSchema;

    @Bean
    public VectorStore qdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
            .collectionName(collectionName)
            .initializeSchema(initializeSchema)
            .build();
    }
}
