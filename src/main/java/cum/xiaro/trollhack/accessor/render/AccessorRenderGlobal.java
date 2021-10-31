package cum.xiaro.trollhack.accessor.render;

import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.shader.ShaderGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderGlobal.class)
public interface AccessorRenderGlobal {
    @Accessor("entityOutlineShader")
    ShaderGroup getEntityOutlineShader();

    @Accessor("damagedBlocks")
    Map<Integer, DestroyBlockProgress> trollGetDamagedBlocks();

    @Accessor("renderEntitiesStartupCounter")
    int trollGetRenderEntitiesStartupCounter();

    @Accessor("renderEntitiesStartupCounter")
    void trollSetRenderEntitiesStartupCounter(int value);

    @Accessor("countEntitiesTotal")
    int trollGetCountEntitiesTotal();

    @Accessor("countEntitiesTotal")
    void trollSetCountEntitiesTotal(int value);

    @Accessor("countEntitiesRendered")
    int trollGetCountEntitiesRendered();

    @Accessor("countEntitiesRendered")
    void trollSetCountEntitiesRendered(int value);

    @Accessor("countEntitiesHidden")
    int trollGetCountEntitiesHidden();

    @Accessor("countEntitiesHidden")
    void trollSetCountEntitiesHidden(int value);
}
