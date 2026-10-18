package com.playerpicked.treasure.storage;

import com.playerpicked.treasure.config.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class Database implements AutoCloseable {
    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS treasure_cooldowns (
                treasure_id VARCHAR(64) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                opened_at TIMESTAMP NOT NULL,
                PRIMARY KEY (treasure_id, player_uuid)
            )
            """;

    private final HikariDataSource dataSource;
    private final SqlClient sql;

    private Database(@NotNull HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.sql = new SqlClient(dataSource);
    }

    public static @NotNull Database connect(@NotNull DatabaseSettings settings, @NotNull Path dataFolder) {
        return switch (settings.type()) {
            case MYSQL -> open(createMysqlPoolConfig(settings), SCHEMA);
            case H2 -> open(createH2PoolConfig(dataFolder), SCHEMA);
        };
    }

    public @NotNull SqlClient sql() {
        return sql;
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) dataSource.close();
    }

    static @NotNull Database open(@NotNull HikariConfig poolConfig, @NotNull String schema) {
        Database database = new Database(new HikariDataSource(poolConfig));
        try {
            database.sql.update(schema);
        } catch (RuntimeException exception) {
            database.close();
            throw exception;
        }
        return database;
    }

    static @NotNull HikariConfig createH2PoolConfig(@NotNull Path dataFolder) {
        HikariConfig config = basePoolConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:file:" + dataFolder.resolve("treasure").toAbsolutePath() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        config.setMaximumPoolSize(4);
        return config;
    }

    private static @NotNull HikariConfig createMysqlPoolConfig(@NotNull DatabaseSettings settings) {
        HikariConfig config = basePoolConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(settings.mysqlJdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.poolSize());

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        return config;
    }

    private static @NotNull HikariConfig basePoolConfig() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("Treasure");
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        return config;
    }
}
