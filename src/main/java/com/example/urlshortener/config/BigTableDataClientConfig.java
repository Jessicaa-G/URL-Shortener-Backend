package com.example.urlshortener.config;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class BigTableDataClientConfig {
    @Value("${PROJECT_ID}")
    private String projectId;
    @Value("${INSTANCE_ID}")
    private String instanceId;

    @Bean({"dataClient"})
    BigtableDataClient create() throws IOException {
        BigtableDataSettings settings = BigtableDataSettings.newBuilder().setProjectId(projectId)
                .setInstanceId(instanceId).build();
        return BigtableDataClient.create(settings);
    }
}

