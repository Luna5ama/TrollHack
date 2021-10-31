package cum.xiaro.trollhack.mixin.render;

import cum.xiaro.trollhack.module.modules.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static cum.xiaro.trollhack.util.accessor.GuiKt.getTileSign;

@Mixin(TileEntitySignRenderer.class)
public class MixinTileEntitySignRenderer {

    private final Minecraft mc = Minecraft.getMinecraft();

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/tileentity/TileEntitySign;signText:[Lnet/minecraft/util/text/ITextComponent;", opcode = Opcodes.GETFIELD))
    public ITextComponent[] getRenderViewEntity(TileEntitySign sign) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getSignText()) {
            GuiScreen screen = mc.currentScreen;
            if (screen instanceof GuiEditSign && getTileSign((GuiEditSign) screen).equals(sign)) {
                return sign.signText;
            }
            return new ITextComponent[]{};
        }
        return sign.signText;
    }

}
