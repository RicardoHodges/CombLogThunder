package com.comblogthunder.combat;

import java.util.UUID;

public final class CombatTag {
    private final UUID playerId;
    private UUID opponentId;
    private long endTimestampMs;

    public CombatTag(UUID playerId, UUID opponentId, long endTimestampMs) {
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.endTimestampMs = endTimestampMs;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(UUID opponentId) {
        this.opponentId = opponentId;
    }

    public long getEndTimestampMs() {
        return endTimestampMs;
    }

    public void setEndTimestampMs(long endTimestampMs) {
        this.endTimestampMs = endTimestampMs;
    }

    public long getRemainingSeconds() {
        long remainingMs = endTimestampMs - System.currentTimeMillis();
        return Math.max(0, (long) Math.ceil(remainingMs / 1000.0));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTimestampMs;
    }
}
