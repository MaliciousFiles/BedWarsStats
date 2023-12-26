package io.github.maliciousfiles.bedwarsstats.mixins;

import io.github.maliciousfiles.bedwarsstats.BedWarsStats;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStat;
import io.github.maliciousfiles.bedwarsstats.util.PlayerStatHolder;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Redirect(method = "renderLivingLabel", at = @At(value="INVOKE", target="Lnet/minecraft/client/renderer/WorldRenderer;pos(DDD)Lnet/minecraft/client/renderer/WorldRenderer;", ordinal = 0))
    private WorldRenderer renderLabel(WorldRenderer instance, double x, double y, double z) {
        PlayerStatHolder stats;

        if (bedWarsStats$entity != null && (stats = BedWarsStats.PLAYER_STATS.get(bedWarsStats$entity.getUniqueID())) != null && stats.error == null) {
            float[] color = PlayerStat.averageColor(stats.stats).getRGBComponents(null);

            instance.pos(-x+3.5, 2 + y, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
            instance.pos(-x+3.5, 7 + y, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
            instance.pos(-x+8.5, 7 + y, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
            instance.pos(-x+8.5, 2 + y, 0.0D).color(color[0], color[1], color[2], color[3]).endVertex();
        }

        return instance.pos(x, y, z);
    }
}
