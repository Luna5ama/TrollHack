package dev.luna5ama.trollhack.mixins.world;

import dev.luna5ama.trollhack.event.impl.world.TickEntityEvent;
import dev.luna5ama.trollhack.event.impl.world.WorldEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Shadow
    public abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/TransientEntitySectionManager;addEntity(Lnet/minecraft/world/level/entity/EntityAccess;)V", shift = At.Shift.BEFORE))
    public void onAddEntityPrivate(Entity entity, CallbackInfo ci) {
        WorldEvent.Entity.Add event = new WorldEvent.Entity.Add(entity);
        event.post();
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    public void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = getEntities().get(entityId);
        if (entity != null) {
            WorldEvent.Entity.Remove event = new WorldEvent.Entity.Remove(entity);
            event.post();
        }
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "HEAD"), cancellable = true)
    public void onTickEntity$Pre(Entity entity, CallbackInfo ci) {
        TickEntityEvent.Pre event = new TickEntityEvent.Pre(entity, (ClientLevel) (Object) this);
        event.post();
        if (event.getCancelled()) ci.cancel();
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "RETURN"))
    public void onTickEntity$Post(Entity entity, CallbackInfo ci) {
        TickEntityEvent.Post event = new TickEntityEvent.Post(entity, (ClientLevel) (Object) this);
        event.post();
    }
}
