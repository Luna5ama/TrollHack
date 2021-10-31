package cum.xiaro.trollhack.util.accessor

import cum.xiaro.trollhack.accessor.render.AccessorDestroyBlockProgress
import cum.xiaro.trollhack.accessor.render.AccessorRenderGlobal
import cum.xiaro.trollhack.accessor.render.AccessorRenderManager
import cum.xiaro.trollhack.accessor.render.AccessorShaderGroup
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.Shader
import net.minecraft.client.shader.ShaderGroup

val DestroyBlockProgress.entityID: Int get() = (this as AccessorDestroyBlockProgress).trollGetEntityID()

val RenderGlobal.entityOutlineShader: ShaderGroup get() = (this as AccessorRenderGlobal).entityOutlineShader
val RenderGlobal.damagedBlocks: MutableMap<Int, DestroyBlockProgress> get() = (this as AccessorRenderGlobal).trollGetDamagedBlocks()
var RenderGlobal.renderEntitiesStartupCounter: Int
    get() = (this as AccessorRenderGlobal).trollGetRenderEntitiesStartupCounter()
    set(value) {
        (this as AccessorRenderGlobal).trollSetRenderEntitiesStartupCounter(value)
    }
var RenderGlobal.countEntitiesTotal: Int
    get() = (this as AccessorRenderGlobal).trollGetCountEntitiesTotal()
    set(value) {
        (this as AccessorRenderGlobal).trollSetCountEntitiesTotal(value)
    }
var RenderGlobal.countEntitiesRendered: Int
    get() = (this as AccessorRenderGlobal).trollGetCountEntitiesRendered()
    set(value) {
        (this as AccessorRenderGlobal).trollSetCountEntitiesRendered(value)
    }
var RenderGlobal.countEntitiesHidden: Int
    get() = (this as AccessorRenderGlobal).trollGetCountEntitiesHidden()
    set(value) {
        (this as AccessorRenderGlobal).trollSetCountEntitiesHidden(value)
    }

val RenderManager.renderPosX: Double get() = (this as AccessorRenderManager).renderPosX
val RenderManager.renderPosY: Double get() = (this as AccessorRenderManager).renderPosY
val RenderManager.renderPosZ: Double get() = (this as AccessorRenderManager).renderPosZ
val RenderManager.renderOutlines: Boolean get() = (this as AccessorRenderManager).renderOutlines

val ShaderGroup.listShaders: List<Shader> get() = (this as AccessorShaderGroup).listShaders
val ShaderGroup.listFrameBuffers: List<Framebuffer> get() = (this as AccessorShaderGroup).listFramebuffers
