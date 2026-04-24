package com.kob.backend.consumer.utils;

public class GameCreateOptions {
    private String matchType = "ranked";
    private Integer mapId = null;
    private String mapName = "随机地图";
    private Integer roundSeconds = 15;
    private Boolean allowSpectator = true;
    private String sourceType = "";
    private Integer sourceId = null;

    public String getMatchType() {
        return matchType == null || matchType.isBlank() ? "ranked" : matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public Integer getMapId() {
        return mapId;
    }

    public void setMapId(Integer mapId) {
        this.mapId = mapId;
    }

    public String getMapName() {
        return mapName == null || mapName.isBlank() ? "随机地图" : mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public Integer getRoundSeconds() {
        return roundSeconds == null || roundSeconds <= 0 ? 15 : roundSeconds;
    }

    public void setRoundSeconds(Integer roundSeconds) {
        this.roundSeconds = roundSeconds;
    }

    public Boolean getAllowSpectator() {
        return allowSpectator == null || allowSpectator;
    }

    public void setAllowSpectator(Boolean allowSpectator) {
        this.allowSpectator = allowSpectator;
    }

    public String getSourceType() {
        return sourceType == null ? "" : sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public static GameCreateOptions ranked() {
        return new GameCreateOptions();
    }
}
