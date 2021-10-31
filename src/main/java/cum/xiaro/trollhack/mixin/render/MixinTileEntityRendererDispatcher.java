package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.module.modules.render.NoRender;
import cum.xiaro.trollhack.module.modules.render.Xray;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcher {
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true)
    public void render$Inject$HEAD(TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (Xray.shouldReplace(tileEntity.getBlockType().getDefaultState())) {
            ci.cancel();
        } else if (NoRender.INSTANCE.isEnabled()) {
            NoRender.handleTileEntity(tileEntity, ci);
        }
    }
}
