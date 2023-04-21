package dev.luna5ama.trollhack.mixins.core.render;

import dev.luna5ama.trollhack.module.modules.render.AntiAlias;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {
    @Shadow
    public int framebufferWidth;
    @Shadow
    public int framebufferHeight;
    @Shadow
    public int framebufferTextureHeight;
    @Shadow
    public int framebufferTextureWidth;
    @Shadow
    public int framebufferObject;
    @Shadow
    public int framebufferTexture;
    @Shadow
    public boolean useDepth;
    @Shadow
    public int depthBuffer;
    @Shadow(remap = false)
    private boolean stencilEnabled;

    @Shadow
    public abstract void framebufferClear();

    @Shadow
    public abstract void setFramebufferFilter(int framebufferFilterIn);

    @Inject(method = "createFramebuffer", at = @At("HEAD"), cancellable = true)
    public void createBindFramebuffer$inject$HEAD(int width, int height, CallbackInfo ci) {
        if (AntiAlias.INSTANCE.isDisabled()) {
            return;
        }

        ci.cancel();

        float level = AntiAlias.INSTANCE.getSampleLevel();

        create((int) (width * level), (int) (height * level));
    }

    private void create(int width, int height) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.framebufferTextureWidth = width;
        this.framebufferTextureHeight = height;

        this.framebufferObject = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, this.framebufferObject);

        this.framebufferTexture = TextureUtil.glGenTextures();

        this.setFramebufferFilter(GL_LINEAR);
        GlStateManager.bindTexture(this.framebufferTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, 6408, 5121, (ByteBuffer) null);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.framebufferTexture, 0);

        if (this.useDepth) {
            this.depthBuffer = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, this.depthBuffer);

            if (!this.stencilEnabled) {
                glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, this.depthBuffer);
            } else {
                glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, this.depthBuffer);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, this.depthBuffer);
            }
        }

        GlStateManager.bindTexture(0);
        if (this.useDepth) glBindRenderbuffer(GL_RENDERBUFFER, 0);

        this.framebufferClear();
    }
}
