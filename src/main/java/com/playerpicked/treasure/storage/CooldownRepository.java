package com.playerpicked.treasure.storage;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownRepository {
    private final SqlClient sql;

    public CooldownRepository(@NotNull SqlClient sql) {
        this.sql = sql;
    }

    public @NotNull Map<String, Instant> findOpenTimesOf(@NotNull UUID playerUuid) {
        Map<String, Instant> openTimes = new HashMap<>();
        sql.queryList("SELECT treasure_id, opened_at FROM treasure_cooldowns WHERE player_uuid = ?",
                        row -> Map.entry(row.getString("treasure_id"), SqlClient.readInstant(row, "opened_at")), playerUuid)
                .forEach(entry -> openTimes.put(entry.getKey(), entry.getValue()));
        return openTimes;
    }

    public void recordOpen(@NotNull String treasureId, @NotNull UUID playerUuid, @NotNull Instant openedAt) {
        sql.update("""
                INSERT INTO treasure_cooldowns (treasure_id, player_uuid, opened_at) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE opened_at = VALUES(opened_at)
                """, treasureId, playerUuid, openedAt);
    }

    public void forgetTreasure(@NotNull String treasureId) {
        sql.update("DELETE FROM treasure_cooldowns WHERE treasure_id = ?", treasureId);
    }
}
