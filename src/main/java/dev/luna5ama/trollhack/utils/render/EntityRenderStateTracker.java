package dev.luna5ama.trollhack.utils.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class EntityRenderStateTracker {
    private static final Map<EntityRenderState, Entity> ENTITIES = Collections.synchronizedMap(new WeakHashMap<>());

    private EntityRenderStateTracker() {
    }

    public static void bind(EntityRenderState state, Entity entity) {
        ENTITIES.put(state, entity);
    }

    public static Entity remove(EntityRenderState state) {
        return ENTITIES.remove(state);
    }
}
