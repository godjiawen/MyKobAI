package com.kob.backend.pojo;

import java.util.List;

/**
 * 对局快照 POJO，用于 Redis 存储与前端下发。
 */
public class GameSnapshot {
    private String gameId;
    private String status;   // playing / finished
    private Integer aId;
    private Integer bId;
    private Integer aBotId;
    private Integer bBotId;
    private Integer aSx;
    private Integer aSy;
    private Integer bSx;
    private Integer bSy;
    private int[][] map;
    private String aSteps;
    private String bSteps;
    private boolean paused;
    private String pausedBy;
    private boolean suspended;
    private Integer suspendedBy;
    private String suspendedReason;
    private List<Integer> awayUserIds;
    private String roomId;
    private String loser;
    private long updatedAt;
    private long expireAt;

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAId() { return aId; }
    public void setAId(Integer aId) { this.aId = aId; }
    public Integer getBId() { return bId; }
    public void setBId(Integer bId) { this.bId = bId; }
    public Integer getABotId() { return aBotId; }
    public void setABotId(Integer aBotId) { this.aBotId = aBotId; }
    public Integer getBBotId() { return bBotId; }
    public void setBBotId(Integer bBotId) { this.bBotId = bBotId; }
    public Integer getASx() { return aSx; }
    public void setASx(Integer aSx) { this.aSx = aSx; }
    public Integer getASy() { return aSy; }
    public void setASy(Integer aSy) { this.aSy = aSy; }
    public Integer getBSx() { return bSx; }
    public void setBSx(Integer bSx) { this.bSx = bSx; }
    public Integer getBSy() { return bSy; }
    public void setBSy(Integer bSy) { this.bSy = bSy; }
    public int[][] getMap() { return map; }
    public void setMap(int[][] map) { this.map = map; }
    public String getASteps() { return aSteps; }
    public void setASteps(String aSteps) { this.aSteps = aSteps; }
    public String getBSteps() { return bSteps; }
    public void setBSteps(String bSteps) { this.bSteps = bSteps; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public String getPausedBy() { return pausedBy; }
    public void setPausedBy(String pausedBy) { this.pausedBy = pausedBy; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public Integer getSuspendedBy() { return suspendedBy; }
    public void setSuspendedBy(Integer suspendedBy) { this.suspendedBy = suspendedBy; }
    public String getSuspendedReason() { return suspendedReason; }
    public void setSuspendedReason(String suspendedReason) { this.suspendedReason = suspendedReason; }
    public List<Integer> getAwayUserIds() { return awayUserIds; }
    public void setAwayUserIds(List<Integer> awayUserIds) { this.awayUserIds = awayUserIds; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getLoser() { return loser; }
    public void setLoser(String loser) { this.loser = loser; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getExpireAt() { return expireAt; }
    public void setExpireAt(long expireAt) { this.expireAt = expireAt; }
}
