package io.github.maliciousfiles.bedwarsstats.util;

import java.util.HashMap;

public class PlayerStatHolder {
    public enum Error {
        RATE_LIMIT("Rate limit exceeded: will try again"),
        NICK("Unable to reveal nick"),
        IO("Internal error, report if this issue persists");

        private final String message;

        Error(String message) {
            this.message = message;
        }
    }

    public final HashMap<PlayerStat, Float> stats;
    public final String error;

    private PlayerStatHolder(HashMap<PlayerStat, Float> stats, String error) {
        this.stats = stats;
        this.error = error;
    }

    public static PlayerStatHolder error(Error error) {
        return new PlayerStatHolder(null, error.message);
    }

    public static PlayerStatHolder of(HashMap<PlayerStat, Float> stats) {
        return new PlayerStatHolder(stats, null);
    }
}
