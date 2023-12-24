package io.github.maliciousfiles.bedwarsstats;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.maliciousfiles.bedwarsstats.util.AsyncCache;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO: list ppl on teams
@Mod(modid = "bedwarsstats", version = "1.0", name="BedWars Stats")
public class BedWarsStats {
    public static final String MODID = "bedwarsstats";
    public static final String VERSION = "1.0";

    private static final String URL = "https://api.hypixel.net/v2/player?uuid=%s&key=ead23913-5ca4-4ce4-a192-2e7940940adc";
    private static final long CACHE_TIME = 1000 * 60 * 10;
    private static final Gson GSON = new Gson();

    public static final AsyncCache<UUID, PlayerStatHolder> PLAYER_STATS = new AsyncCache<>(BedWarsStats::queryPlayerData);

    @Mod.EventHandler
    public void init(FMLInitializationEvent evt) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static Map.Entry<PlayerStatHolder, Long> queryPlayerData(UUID player) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format(URL, player)).openConnection();
            JsonObject response = GSON.fromJson(
                    new BufferedReader(new InputStreamReader(conn.getInputStream())
                    ).readLine(), JsonObject.class);

            if (conn.getResponseCode() == 429) {
                return Maps.immutableEntry(PlayerStatHolder.error(PlayerStatHolder.Error.RATE_LIMIT),
                        conn.getHeaderFieldLong("Retry-After", 0));
            }

            JsonElement stats = response.get("player");
            if (stats.isJsonNull()) {
                String newId = GSON.fromJson(new String(Base64.getDecoder().decode(
                        Minecraft.getMinecraft().theWorld.getPlayerEntityByUUID(player)
                                .getGameProfile().getProperties().get("texture")
                                .stream().findFirst().get().getValue())), JsonObject.class)
                        .get("profileId").getAsString();
                UUID newUUID = new UUID(Integer.parseInt(newId.substring(0, 16), 16), Integer.parseInt(newId.substring(16), 16));

                System.out.println("NICK: "+player+" -> "+newUUID);
                if (newUUID.equals(player)) return Maps.immutableEntry(PlayerStatHolder.error(PlayerStatHolder.Error.NICK), CACHE_TIME);

                return queryPlayerData(newUUID);
            }

            HashMap<PlayerStat, Float> playerStats = new HashMap<>();

            for (PlayerStat stat : PlayerStat.values()) {
                playerStats.put(stat, stat.parse(stats.getAsJsonObject()));
            }

            return Maps.immutableEntry(PlayerStatHolder.of(playerStats), CACHE_TIME);
        } catch (IOException|NullPointerException e) {
            LogManager.getLogger().error(e.getClass()+": "+e.getLocalizedMessage());
            return null;
        }
    }

    private static boolean inBWs = false;
    public static boolean notInBWs() {
        return !inBWs;
    }

    private boolean ranLocraw;
    @SubscribeEvent(priority= EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent evt) {
        if (evt.message.getUnformattedTextForChat().startsWith("Sending you to mini")) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/locraw");
            ranLocraw = true;
            return;
        }

        try {
            JsonObject obj = GSON.fromJson(evt.message.getUnformattedTextForChat(), JsonObject.class);
            if (obj == null || !obj.has("server")) return;

            if (ranLocraw) evt.setCanceled(true);
            ranLocraw = false;
            inBWs = obj.get("server").getAsString().startsWith("mini") &&
                    obj.get("gametype").getAsString().equals("BEDWARS");
        } catch (JsonSyntaxException|ClassCastException ignored) {}
    }
}
