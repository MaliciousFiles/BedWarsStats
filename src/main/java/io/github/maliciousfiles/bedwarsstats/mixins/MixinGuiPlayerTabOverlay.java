package io.github.maliciousfiles.bedwarsstats.mixins;

import io.github.maliciousfiles.bedwarsstats.BedWarsStats;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    @Unique
    private UUID bedWarsStats$playerUUID = null;
    @Unique
    private int bedWarsStats$nameX = -1, bedWarsStats$nameY = -1, bedWarsStats$columnWidth = -1;

    @Unique
    private int[] bedWarsStats$maxXs = new int[PlayerStat.values().length];
    @Unique
    private int[] bedWarsStats$curMaxXs = new int[PlayerStat.values().length];
    @Unique
    private int bedWarsStats$maxWidth = 0;
    @Unique
    private int bedWarsStats$curMaxWidth = 0;

    @Inject(method="renderPlayerlist", at=@At("HEAD"))
    public void resetMaxWidth(CallbackInfo ci) {
        bedWarsStats$curMaxWidth = 0;
        Arrays.fill(bedWarsStats$curMaxXs, 0);
    }
    @Inject(method="renderPlayerlist", at=@At("RETURN"))
    public void moveMaxWidth(CallbackInfo ci) {
        bedWarsStats$maxWidth = bedWarsStats$curMaxWidth;
        bedWarsStats$maxXs = Arrays.copyOf(bedWarsStats$curMaxXs, bedWarsStats$curMaxXs.length);
    }

    @ModifyVariable(method = "renderPlayerlist", index=24, at = @At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    public NetworkPlayerInfo getPlayerName(NetworkPlayerInfo networkplayerinfo1) {
        bedWarsStats$playerUUID = networkplayerinfo1.getGameProfile().getId();
        return networkplayerinfo1;
    }

    @ModifyVariable(method = "renderPlayerlist", index=23, at = @At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    public int getNameY(int k2) {
        return bedWarsStats$nameY = k2;
    }

    @ModifyVariable(method = "renderPlayerlist", index = 13, at=@At(value = "INVOKE", target="Lnet/minecraft/client/gui/FontRenderer;listFormattedStringToWidth(Ljava/lang/String;I)Ljava/util/List;", ordinal=0))
    public int getColumnWidth(int i1) {
        return bedWarsStats$columnWidth = i1;
    }

    @ModifyVariable(method="renderPlayerlist", index=16, at=@At(value = "INVOKE", target="Lnet/minecraft/client/gui/FontRenderer;listFormattedStringToWidth(Ljava/lang/String;I)Ljava/util/List;", ordinal=1))
    public int increaseWidth(int l1) {
        if (BedWarsStats.notInBWs()) return l1;

        return bedWarsStats$columnWidth + bedWarsStats$maxWidth + 4;
    }
    @ModifyVariable(method="renderPlayerlist", index=14, at=@At(value = "INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal=0))
    public int setStartX(int j1) {
        if (BedWarsStats.notInBWs()) return j1;

        return bedWarsStats$nameX = j1 - 1 - bedWarsStats$maxWidth / 2;
    }


    @Inject(method="renderPlayerlist", at = @At(value = "INVOKE", shift= At.Shift.AFTER, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    public void renderStats(CallbackInfo ci) {
        if (BedWarsStats.notInBWs()) return;

        PlayerStatHolder stats = BedWarsStats.PLAYER_STATS.get(bedWarsStats$playerUUID);
        if (stats == null) return;

        FontRenderer renderer = Minecraft.getMinecraft().fontRendererObj;
        int startX = bedWarsStats$nameX + bedWarsStats$columnWidth + 2;

        if (stats.error != null) {
            renderer.drawString(stats.error, startX, bedWarsStats$nameY, Color.RED.getRGB());
            bedWarsStats$curMaxWidth = Math.max(bedWarsStats$curMaxWidth, renderer.getStringWidth(stats.error));
            return;
        }

        int width = 0;
        for (PlayerStat stat : PlayerStat.values()) {
            String label = stat+":";
            float value = stats.stats.get(stat);
            Color color = stat.getColor(value);

            bedWarsStats$curMaxXs[stat.ordinal()] = Math.max(bedWarsStats$curMaxXs[stat.ordinal()], width);
            width = Math.max(width, bedWarsStats$maxXs[stat.ordinal()]);

            width = 2 + renderer.drawString(label,
                    startX+width, bedWarsStats$nameY, Color.LIGHT_GRAY.getRGB()) - startX;

            width = 5 + renderer.drawString(new DecimalFormat("0.##").format(value),
                    startX+width, bedWarsStats$nameY, color.getRGB()) - startX;
        }

        bedWarsStats$curMaxWidth = Math.max(bedWarsStats$curMaxWidth, width - 5);
    }
}