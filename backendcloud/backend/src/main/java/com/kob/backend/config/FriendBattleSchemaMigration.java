package com.kob.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FriendBattleSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public FriendBattleSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("friend_invite", "map_id", "INT NULL");
        addColumnIfMissing("friend_invite", "room_name", "VARCHAR(50) NOT NULL DEFAULT ''");
        addColumnIfMissing("friend_invite", "round_seconds", "INT NOT NULL DEFAULT 15");
        addColumnIfMissing("friend_invite", "allow_spectator", "TINYINT(1) NOT NULL DEFAULT 1");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM " + tableName + " LIKE ?",
                columnName
        );
        if (columns.isEmpty()) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
