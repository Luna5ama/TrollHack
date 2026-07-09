package dev.luna5ama.trollhack.mixins.render;

import dev.luna5ama.trollhack.event.impl.render.ParticleEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
    @Inject(at = @At("HEAD"), method = "add(Lnet/minecraft/client/particle/Particle;)V", cancellable = true)
    public void onAddParticle(Particle particle, CallbackInfo ci) {
        ParticleEvent.AddParticle event = new ParticleEvent.AddParticle(particle);
        event.post();
        if (event.getCancelled()) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V", cancellable = true)
    public void onAddEmmiter(Entity entity, ParticleOptions data, CallbackInfo ci) {
        ParticleEvent.AddEmmiter event = new ParticleEvent.AddEmmiter(data);
        event.post();
        if (event.getCancelled()) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V", cancellable = true)
    public void onAddEmmiterAged(Entity entity, ParticleOptions data, int lifetime, CallbackInfo ci) {
        ParticleEvent.AddEmmiter event = new ParticleEvent.AddEmmiter(data);
        event.post();
        if (event.getCancelled()) {
            ci.cancel();
        }
    }
}