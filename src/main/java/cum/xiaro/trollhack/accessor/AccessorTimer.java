package cum.xiaro.trollhack.accessor;

import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface AccessorTimer {
    @Accessor("tickLength")
    float trollGetTickLength();

    @Accessor("tickLength")
    void trollSetTickLength(float value);
}
