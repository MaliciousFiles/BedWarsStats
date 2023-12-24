package io.github.maliciousfiles.bedwarsstats.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public enum PlayerStat {
    KILLS(stats -> getStat(stats, "stats.Bedwars.kills_bedwars"), new float[]{5000, 10000, 20000}, 0.15f),
    WINS(stats -> getStat(stats, "stats.Bedwars.wins_bedwars"), new float[]{500, 1500, 5000}, 0.1f),
    BEDS(stats -> getStat(stats, "stats.Bedwars.beds_broken_bedwars"), new float[]{500, 4000, 10000}, 0.1f),
    LEVEL(stats -> getStat(stats, "achievements.bedwars_level"), new float[]{100, 300, 500}, 0.15f),
    FKDR(stats -> getStat(stats, "stats.Bedwars.final_kills_bedwars") /
            getStat(stats, "stats.Bedwars.final_deaths_bedwars"), new float[]{1, 4, 10}, 0.2f, false),
    WLR(stats -> getStat(stats, "stats.Bedwars.wins_bedwars") /
            getStat(stats, "stats.Bedwars.losses_bedwars"), new float[]{1, 4, 10}, 0.3f, false);

    private final String label;
    private final float[] thresholds;
    private final float weight;
    private final Function<JsonObject, Float> parser;

    PlayerStat(Function<JsonObject, Float> parser, float[] thresholds, float weight, boolean capitalize) {
        this.label = capitalize ? WordUtils.capitalizeFully(name()) : name();
        this.thresholds = thresholds;
        this.weight = weight;
        this.parser = parser;
    }

    PlayerStat(Function<JsonObject, Float> parser, float[] thresholds, float weight) {
        this(parser, thresholds, weight, true);
    }

    @Override
    public String toString() {
        return label;
    }

    private static Float getStat(JsonObject obj, String stat) {
        JsonElement element = obj;
        for (String key : stat.split("\\.")) {
            element = element.getAsJsonObject().get(key);
        }
        return element.getAsFloat();
    }

    public float parse(JsonObject stats) {
        return Math.round(parser.apply(stats) * 100) / 100f;
    }

    private float getHue(float value) {
        int startIdx;
        for (startIdx = 0; startIdx < thresholds.length-2; startIdx++) {
            if (value < thresholds[startIdx]) break;
        }

        if (startIdx == thresholds.length) return 0;
        float start = startIdx == 0 ? 0 : thresholds[startIdx];
        float end = thresholds[startIdx + 1];

        float localPercent = (value - start) / (end - start);

        float global = startIdx + localPercent;

        return (1 - global / thresholds.length) * 96;
    }

    private static Color getColorFromHue(float hue) {
        float excess = Math.min(-hue / 360, 0);
        return Color.getHSBColor(Math.max(hue / 360, 0), 1, Math.max(0.86f + excess, 0));
    }

    public Color getColor(float value) {
        return getColorFromHue(getHue(value));
    }

    public static Color averageColor(Map<PlayerStat, Float> stats) {
        if (stats == null) return getColorFromHue(0);

        float hue = 0;

        for (Map.Entry<PlayerStat, Float> entry : stats.entrySet()) {
            hue += entry.getKey().getHue(entry.getValue()) * entry.getKey().weight;
        }

        return getColorFromHue(hue);
    }
}
