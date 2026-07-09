package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface IClientboundSectionBlocksUpdatePacketAccessor {

    @Accessor
    SectionPos getSectionPos();

    @Mutable
    @Accessor
    void setSectionPos(SectionPos sectionPos);

    @Mutable
    @Accessor
    void setPositions(short[] positions);

    @Mutable
    @Accessor
    void setStates(BlockState[] blockStates);

//    @Mutable
//    @Accessor
//    void setNoLightingUpdates(boolean noLightingUpdates);

}