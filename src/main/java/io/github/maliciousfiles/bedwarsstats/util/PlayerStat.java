package io.github.maliciousfiles.bedwarsstats.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public enum PlayerStat {
    FKDR(stats -> getStat(stats, "stats.Bedwars.final_kills_bedwars") /
            getStat(stats, "stats.Bedwars.final_deaths_bedwars"), new float[]{0.7f, 1, 8}, 0.4f, false),
    KILLS(stats -> getStat(stats, "stats.Bedwars.kills_bedwars"), new float[]{3000, 8000, 20000}, 0.1f),
    LEVEL(stats -> getStat(stats, "achievements.bedwars_level"), new float[]{100, 300, 500}, 0.1f),
    WINS(stats -> getStat(stats, "stats.Bedwars.wins_bedwars"), new float[]{500, 1500, 5000}, 0.1f),
    BEDS(stats -> getStat(stats, "stats.Bedwars.beds_broken_bedwars"), new float[]{500, 4000, 10000}, 0.1f),
    WLR(stats -> getStat(stats, "stats.Bedwars.wins_bedwars") /
            getStat(stats, "stats.Bedwars.losses_bedwars"), new float[]{0.5f, 1, 6}, 0.2f, false);

    private static final float[] THRESHOLD_HUES = new float[] {135, 96, 55, 0};
    private static final float[] THRESHOLD_VALUES = new float[] {100, 86, 100, 65};

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
            if (element == null) return 0.0f;
        }

        return element.getAsFloat();
    }

    public float parse(JsonObject stats) {
        return Math.round(parser.apply(stats) * 100) / 100f;
    }

    private float interpolate(float value, float[] values) {
        int endIdx;
        for (endIdx = 0; endIdx < thresholds.length-1; endIdx++) {
            if (value < thresholds[endIdx]) break;
        }

        float start = endIdx == 0 ? 0 : thresholds[endIdx - 1];
        float end = thresholds[endIdx];

        float percent = (value - start) / (end - start);

        float startHue = values[endIdx];
        float endHue = values[endIdx+1];

        return percent * (endHue-startHue) + startHue;
    }

    private static Color getColorFromHV(float h, float v) {
        float excess = Math.max(-h / 360, 0);
        return Color.getHSBColor(Math.max(h / 360, 0), 1, Math.max(v / 100 - excess, 0.37f));
    }

    public Color getColor(float value) {
        return getColorFromHV(interpolate(value, THRESHOLD_HUES), interpolate(value, THRESHOLD_VALUES));
    }

    public static Color averageColor(Map<PlayerStat, Float> stats) {
        if (stats == null) return getColorFromHV(0, 0);

        float hue = 0;
        float value = 0;

        for (Map.Entry<PlayerStat, Float> entry : stats.entrySet()) {
            hue += entry.getKey().interpolate(entry.getValue(), THRESHOLD_HUES) * entry.getKey().weight;
            value += entry.getKey().interpolate(entry.getValue(), THRESHOLD_VALUES) * entry.getKey().weight;
        }

        return getColorFromHV(hue, value);
    }
}
