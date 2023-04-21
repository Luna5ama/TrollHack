package dev.luna5ama.trollhack.util.accessor

import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.Shader
import net.minecraft.client.shader.ShaderGroup

val DestroyBlockProgress.entityID: Int get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorDestroyBlockProgress).trollGetEntityID()

val RenderGlobal.entityOutlineShader: ShaderGroup get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).entityOutlineShader
val RenderGlobal.damagedBlocks: MutableMap<Int, DestroyBlockProgress> get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollGetDamagedBlocks()
var RenderGlobal.renderEntitiesStartupCounter: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollGetRenderEntitiesStartupCounter()
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollSetRenderEntitiesStartupCounter(
            value
        )
    }
var RenderGlobal.countEntitiesTotal: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollGetCountEntitiesTotal()
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollSetCountEntitiesTotal(value)
    }
var RenderGlobal.countEntitiesRendered: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollGetCountEntitiesRendered()
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollSetCountEntitiesRendered(value)
    }
var RenderGlobal.countEntitiesHidden: Int
    get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollGetCountEntitiesHidden()
    set(value) {
        (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderGlobal).trollSetCountEntitiesHidden(value)
    }

val RenderManager.renderPosX: Double get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderManager).renderPosX
val RenderManager.renderPosY: Double get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderManager).renderPosY
val RenderManager.renderPosZ: Double get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderManager).renderPosZ
val RenderManager.renderOutlines: Boolean get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorRenderManager).renderOutlines

val ShaderGroup.listShaders: List<Shader> get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorShaderGroup).listShaders
val ShaderGroup.listFrameBuffers: List<Framebuffer> get() = (this as dev.luna5ama.trollhack.mixins.accessor.render.AccessorShaderGroup).listFramebuffers
