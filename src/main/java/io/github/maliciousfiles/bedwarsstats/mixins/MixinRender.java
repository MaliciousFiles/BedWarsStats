package io.github.maliciousfiles.bedwarsstats.mixins;

import io.github.maliciousfiles.bedwarsstats.BedWarsStats;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Render.class)
public abstract class MixinRender {
    @Shadow @Final protected RenderManager renderManager;

    // TODO: remove unnecessary bloat
    // TODO: players have two nametags
    @Inject(method = "renderLivingLabel", at = @At(value="INVOKE", shift=At.Shift.AFTER, target="Lnet/minecraft/client/renderer/GlStateManager;popMatrix()V"), locals= LocalCapture.CAPTURE_FAILSOFT)
    private <T extends Entity> void renderLabel(T entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (BedWarsStats.notInBWs() || !(entityIn instanceof EntityPlayer)) return;

        PlayerStatHolder stats = BedWarsStats.PLAYER_STATS.get(entityIn.getUniqueID());
        if (stats == null || stats.error != null) return;

        FontRenderer fontrenderer = this.renderManager.getFontRenderer();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + 0.0F, (float)y + entityIn.height + 0.5F, (float)z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.0266666688, -0.0266666688, 0.0266666688);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        if (str.equals("deadmau5"))
        {
            i = -10;
        }

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        float[] color = PlayerStat.averageColor(stats.stats).getRGBComponents(null);
        worldrenderer.pos(j+4.5, 1 + i, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
        worldrenderer.pos(j+4.5, 6 + i, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
        worldrenderer.pos(j+9.5, 6 + i, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
        worldrenderer.pos(j+9.5, 1 + i, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
