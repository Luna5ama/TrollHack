package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.event.events.render.RenderEntityEvent;
import cum.xiaro.trollhack.module.modules.combat.CrystalChams;
import cum.xiaro.trollhack.util.graphics.GlStateUtils;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderDragon;
import net.minecraft.client.renderer.entity.RenderEnderCrystal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(RenderEnderCrystal.class)
public abstract class MixinRenderEnderCrystal extends Render<EntityEnderCrystal> {
    @Shadow @Final private ModelBase modelEnderCrystal;
    @Shadow @Final private ModelBase modelEnderCrystalNoBase;

    protected MixinRenderEnderCrystal(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    public void doRender$Inject$HEAD(EntityEnderCrystal entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (CrystalChams.INSTANCE.isEnabled() && !RenderEntityEvent.getRenderingEntities()) {
            float spinTicks = (float) entity.innerRotation + partialTicks;
            float floatTicks = MathHelper.sin(spinTicks * 0.2f * CrystalChams.INSTANCE.getFloatSpeed()) / 2.0f + 0.5f;
            float scale = CrystalChams.INSTANCE.getScale();
            float spinSpeed = CrystalChams.INSTANCE.getSpinSpeed();

            floatTicks = floatTicks * floatTicks + floatTicks;
            ModelBase model = entity.shouldShowBottom() ? this.modelEnderCrystal : this.modelEnderCrystalNoBase;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);

            if (CrystalChams.INSTANCE.getFilled()) {
                CrystalChams.setFilledColor();
                GlStateUtils.INSTANCE.depth(CrystalChams.INSTANCE.getFilledDepth());
                model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f * scale);
            }

            if (CrystalChams.INSTANCE.getOutline()) {
                CrystalChams.setOutlineColor();
                GlStateUtils.INSTANCE.depth(CrystalChams.INSTANCE.getOutlineDepth());
                GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f * scale);
                GlStateManager.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }

            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
            BlockPos blockpos = entity.getBeamTarget();

            if (blockpos != null) {
                this.bindTexture(RenderDragon.ENDERCRYSTAL_BEAM_TEXTURES);
                double posX = blockpos.getX() + 0.5f - entity.posX;
                double posY = blockpos.getY() + 0.5f - entity.posY;
                double posZ = blockpos.getZ() + 0.5f - entity.posZ;
                RenderDragon.renderCrystalBeams(x + posX, y - 0.3D + (double) (floatTicks * 0.4f) + posY, z + posZ, partialTicks, (float) blockpos.getX() + 0.5f, (float) blockpos.getY() + 0.5f, (float) blockpos.getZ() + 0.5f, entity.innerRotation, entity.posX, entity.posY, entity.posZ);
            }

            GlStateManager.disableTexture2D();
            ci.cancel();
        }
    }
}
