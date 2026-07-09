package dev.luna5ama.trollhack.mixins.screen;

import dev.luna5ama.trollhack.mixins.accessor.IAdvancementWidgetAccessor;
import dev.luna5ama.trollhack.interfaces.IAdvancementsTab;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Map;

@Mixin(AdvancementTab.class)
public abstract class MixinAdvancementTab implements IAdvancementsTab {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow @Final private Map<AdvancementHolder, AdvancementWidget> widgets;

    @Override
    public void languagereload_recreateWidgets() {
        widgets.replaceAll((advancement, widget) -> {
            var newWidget = new AdvancementWidget(
                    ((IAdvancementWidgetAccessor) widget).languagereload_getTab(),
                    minecraft,
                    ((IAdvancementWidgetAccessor) widget).languagereload_getAdvancement(),
                    ((IAdvancementWidgetAccessor) widget).languagereload_getDisplay()
            );
            newWidget.setProgress(((IAdvancementWidgetAccessor) widget).languagereload_getProgress());
            ((IAdvancementWidgetAccessor) newWidget).languagereload_setParent(((IAdvancementWidgetAccessor) widget).languagereload_getParent());
            ((IAdvancementWidgetAccessor) newWidget).languagereload_setChildren(((IAdvancementWidgetAccessor) widget).languagereload_getChildren());
            return newWidget;
        });
    }
}
