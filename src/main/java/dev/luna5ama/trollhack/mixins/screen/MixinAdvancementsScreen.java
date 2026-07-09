package dev.luna5ama.trollhack.mixins.screen;

import dev.luna5ama.trollhack.interfaces.IAdvancementsScreen;
import dev.luna5ama.trollhack.interfaces.IAdvancementsTab;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(AdvancementsScreen.class)
public abstract class MixinAdvancementsScreen extends Screen implements ClientAdvancements.Listener, IAdvancementsScreen {
    @Shadow @Final private Map<AdvancementHolder, AdvancementTab> tabs;

    protected MixinAdvancementsScreen(Component title) {
        super(title);
    }

    @Override
    public void languagereload_recreateWidgets() {
        for (var advancementTab : tabs.values()) {
            ((IAdvancementsTab) advancementTab).languagereload_recreateWidgets();
        }
    }
}
