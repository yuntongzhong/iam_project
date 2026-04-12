package com.example.iam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
@RequiredArgsConstructor
public class AuthorizationServerSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("oauth2_authorization")) {
            executeScript("sql/oauth2-authorization-schema.sql");
        }
        if (!tableExists("oauth2_authorization_consent")) {
            executeScript("sql/oauth2-authorization-consent-schema.sql");
        }
    }

    private void executeScript(String path) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(path));
        try {
            populator.execute(dataSource);
        } catch (ScriptException ex) {
            throw new IllegalStateException("Failed to initialize authorization server schema from " + path, ex);
        }
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return exists(metadata, tableName)
                    || exists(metadata, tableName.toUpperCase())
                    || exists(metadata, tableName.toLowerCase());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to inspect database metadata", ex);
        }
    }

    private boolean exists(DatabaseMetaData metadata, String tableName) throws Exception {
        try (ResultSet resultSet = metadata.getTables(null, null, tableName, null)) {
            return resultSet.next();
        }
    }
}
