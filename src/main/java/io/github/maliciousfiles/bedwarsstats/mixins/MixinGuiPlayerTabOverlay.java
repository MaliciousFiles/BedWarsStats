package io.github.maliciousfiles.bedwarsstats.mixins;

import io.github.maliciousfiles.bedwarsstats.BedWarsStats;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    // ik this isn't how you're supposed to do it, but the obfuscation
    // mappings are screwed up for GuiPlayerTabOverlay, so any local capture
    // breaks the mixin
    @Unique
    private UUID bedWarsStats$playerUUID = null;
    @Unique
    private int bedWarsStats$nameX = -1, bedWarsStats$nameY = -1, bedWarsStats$columnWidth = -1;

    @Unique
    private int bedWarsStats$maxWidth = 0;

    @ModifyVariable(method = "renderPlayerlist", index=24, at = @At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    public NetworkPlayerInfo getPlayerName(NetworkPlayerInfo networkplayerinfo1) {
        bedWarsStats$playerUUID = networkplayerinfo1.getGameProfile().getId();
        return networkplayerinfo1;
    }

    @ModifyVariable(method = "renderPlayerlist", index=23, at = @At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    public int getNameY(int k2) {
        this.bedWarsStats$nameY = k2;
        return k2;
    }

    @ModifyVariable(method = "renderPlayerlist", index = 13, at = @At(value = "INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V"))
    public int getColumnWidth(int i1) {
        if (BedWarsStats.notInBWs()) return i1;

        return bedWarsStats$columnWidth = i1;
    }

    @ModifyVariable(method="renderPlayerlist", index=16, at=@At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal=0))
    public int increaseWidth(int l1) {
        if (BedWarsStats.notInBWs()) return l1;

        return bedWarsStats$columnWidth + bedWarsStats$maxWidth + 4;
    }
    @ModifyVariable(method="renderPlayerlist", index=14, at=@At(value="INVOKE", shift= At.Shift.BEFORE, target="Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal=0))
    public int setStartX(int j1) {
        if (BedWarsStats.notInBWs()) return j1;

        return bedWarsStats$nameX = j1 - 2 - bedWarsStats$maxWidth / 2;
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
            bedWarsStats$maxWidth = Math.max(bedWarsStats$maxWidth, renderer.getStringWidth(stats.error));
            return;
        }

        int width = 0;
        for (PlayerStat stat : PlayerStat.values()) {
            String label = stat+":";
            float value = stats.stats.get(stat);
            Color color = stat.getColor(value);

            renderer.drawString(label, startX+width, bedWarsStats$nameY, Color.WHITE.getRGB());
            width += renderer.getStringWidth(label) + 2;

            String valueStr = new DecimalFormat("0.##").format(value);
            renderer.drawString(valueStr, startX+width, bedWarsStats$nameY, color.getRGB());
            width += renderer.getStringWidth(valueStr) + 5;
        }

        bedWarsStats$maxWidth = Math.max(bedWarsStats$maxWidth, width - 5);
    }
}