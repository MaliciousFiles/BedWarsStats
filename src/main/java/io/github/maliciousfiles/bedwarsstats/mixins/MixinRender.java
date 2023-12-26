package io.github.maliciousfiles.bedwarsstats.mixins;

import io.github.maliciousfiles.bedwarsstats.BedWarsStats;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class MixinRender {
    @Unique
    private EntityPlayer bedWarsStats$entity = null;

    @Inject(method="renderLivingLabel", at=@At("HEAD"))
    public <T extends Entity> void getPlayer(T entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        bedWarsStats$entity = BedWarsStats.notInBWs() || !(entityIn instanceof EntityPlayer) || !str.contains(entityIn.getName()) ? null :
                (EntityPlayer) entityIn;
    }

    @Unique
    private void bedWarsStats$render(WorldRenderer renderer, double x, double y, double z, float[] color) {
        renderer.pos(-x+2, 2 + y, z).color(color[0], color[1], color[2], color[3]).endVertex();
        renderer.pos(-x+2, 7 + y, z).color(color[0], color[1], color[2], color[3]).endVertex();
        renderer.pos(-x+7, 7 + y, z).color(color[0], color[1], color[2], color[3]).endVertex();
        renderer.pos(-x+7, 2 + y, z).color(color[0], color[1], color[2], color[3]).endVertex();

        Tessellator.getInstance().draw();
        GlStateManager.disableTexture2D();
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
    }
    
    @Redirect(method = "renderLivingLabel", at = @At(value="INVOKE", target="Lnet/minecraft/client/renderer/WorldRenderer;pos(DDD)Lnet/minecraft/client/renderer/WorldRenderer;", ordinal = 0))
    private WorldRenderer renderLabel(WorldRenderer instance, double x, double y, double z) {
        PlayerStatHolder stats;

        if (bedWarsStats$entity != null && (stats = BedWarsStats.PLAYER_STATS.get(bedWarsStats$entity.getUniqueID())) != null && stats.error == null) {
            float[] color = PlayerStat.averageColor(stats.stats).getRGBComponents(null);
            float colorMod = (float) 0x4F / 0xFF;

            GlStateManager.disableDepth();
            bedWarsStats$render(instance, x, y, z, new float[] {color[0] * colorMod, color[1] * colorMod, color[2] * colorMod, color[3]});

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            bedWarsStats$render(instance, x, y, z, color);

            GlStateManager.disableDepth();
        }

        return instance.pos(x, y, z);
    }
}
