package io.github.maliciousfiles.bedwarsstats;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.properties.Property;
import io.github.maliciousfiles.bedwarsstats.util.AsyncCache;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

// TODO: list ppl on teams
@Mod(modid = "bedwarsstats", version = "1.0", name="BedWars Stats")
public class BedWarsStats {
    private static final String URL = "https://api.hypixel.net/v2/player?uuid=%s&key=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    private static final long CACHE_TIME = 1000 * 60 * 10;
    private static final Gson GSON = new Gson();

    public static final AsyncCache<UUID, PlayerStatHolder> PLAYER_STATS = new AsyncCache<>(BedWarsStats::queryPlayerData);

    @Mod.EventHandler
    public void init(FMLInitializationEvent evt) {
        MinecraftForge.EVENT_BUS.register(this);
    }


    private static long rateLimitReset = -1;
    public static Map.Entry<PlayerStatHolder, Long> queryPlayerData(UUID player) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format(URL, player)).openConnection();

            if (rateLimitReset <= System.currentTimeMillis()) {
                rateLimitReset = System.currentTimeMillis() + conn.getHeaderFieldLong("Retry-After", 300) * 1000 + 250; // 250ms buffer
            }
            if (conn.getResponseCode() == 429) {
                return Maps.immutableEntry(PlayerStatHolder.error(PlayerStatHolder.Error.RATE_LIMIT), rateLimitReset - System.currentTimeMillis());
            }

            JsonObject response = GSON.fromJson(
                    new BufferedReader(new InputStreamReader(conn.getInputStream())
                    ).readLine(), JsonObject.class);

            JsonElement stats = response.get("player");
            if (stats.isJsonNull()) {
                EntityPlayer entity = Minecraft.getMinecraft().theWorld.getPlayerEntityByUUID(player);
                if (entity == null) return Maps.immutableEntry(null, CACHE_TIME);

                Optional<Property> texture = entity
                        .getGameProfile().getProperties().get("textures")
                        .stream().findFirst();
                if (!texture.isPresent()) {
                    System.out.println("UNABLE TO FIND NICK ("+player+"): "+entity.getGameProfile().getProperties().keys());
                    return Maps.immutableEntry(PlayerStatHolder.error(PlayerStatHolder.Error.NICK), CACHE_TIME);
                }

                String newId = GSON.fromJson(new String(Base64.getDecoder().decode(
                        texture.get().getValue())), JsonObject.class)
                        .get("profileId").getAsString();
                UUID newUUID = UUID.fromString(newId.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));

                return queryPlayerData(newUUID);
            }

            HashMap<PlayerStat, Float> playerStats = new HashMap<>();

            for (PlayerStat stat : PlayerStat.values()) {
                playerStats.put(stat, stat.parse(stats.getAsJsonObject()));
            }

            return Maps.immutableEntry(PlayerStatHolder.of(playerStats), CACHE_TIME);
        } catch (IOException|NullPointerException e) {
            LogManager.getLogger().error(e.getClass()+": "+e.getLocalizedMessage());
            for (StackTraceElement trace : e.getStackTrace()) {
                LogManager.getLogger().error("\t"+trace);
            }
            return Maps.immutableEntry(PlayerStatHolder.error(PlayerStatHolder.Error.IO), CACHE_TIME);
        }
    }

    private static boolean inBWs = false;
    public static boolean notInBWs() {
        return !inBWs;
    }

    private boolean ranLocraw;
    private boolean runLocraw;

    @SubscribeEvent
    public void onChangeWorld(EntityJoinWorldEvent evt) {
        if (evt.entity != Minecraft.getMinecraft().thePlayer) return;

        runLocraw = true;
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.player != Minecraft.getMinecraft().thePlayer) return;

        runLocraw = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent evt) {
        if (runLocraw) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/locraw");
            ranLocraw = true;
            runLocraw = false;
        }
    }

    @SubscribeEvent(priority= EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent evt) {
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
