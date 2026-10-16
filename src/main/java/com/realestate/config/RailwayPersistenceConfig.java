package com.realestate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RailwayPersistenceConfig {
    @Bean
    ApplicationRunner requirePersistentStorageOnRailway(
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${RAILWAY_VOLUME_MOUNT_PATH:}") String volumeMountPath,
            @Value("${RAILWAY_ENVIRONMENT_NAME:}") String railwayEnvironment) {
        return args -> {
            boolean usesFileDatabase = databaseUrl.startsWith("jdbc:h2:file:");
            boolean runningOnRailway = !railwayEnvironment.isBlank()
                    || System.getenv().keySet().stream().anyMatch(name -> name.startsWith("RAILWAY_"));

            if (runningOnRailway && usesFileDatabase && volumeMountPath.isBlank()) {
                throw new IllegalStateException(
                        "Railway H2 database has no persistent volume. Attach a Railway volume, "
                                + "then set REAL_ESTATE_DATA_DIR to its mount path, or configure "
                                + "REAL_ESTATE_DB_URL to use a persistent external database.");
            }
        };
    }
}
