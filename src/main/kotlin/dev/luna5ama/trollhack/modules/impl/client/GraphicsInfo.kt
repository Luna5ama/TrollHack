package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.RenderSystem
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module

object GraphicsInfo : Module("Graphics Info", category = Category.CLIENT) {
    init {
        val compatibility = RenderSystem.compatibility
        label("GPU Vendor", { compatibility.gpuManufacturer }, true)
        label("GPU Name", { compatibility.gpuName }, true)
        label("OpenGL 1.1", { compatibility.openGL11.toString() }, true)
        label("OpenGL 1.2", { compatibility.openGL12.toString() }, true)
        label("OpenGL 1.3", { compatibility.openGL13.toString() }, true)
        label("OpenGL 1.4", { compatibility.openGL14.toString() }, true)
        label("OpenGL 1.5", { compatibility.openGL15.toString() }, true)
        label("OpenGL 2.0", { compatibility.openGL20.toString() }, true)
        label("OpenGL 2.1", { compatibility.openGL21.toString() }, true)
        label("OpenGL 3.0", { compatibility.openGL30.toString() }, true)
        label("OpenGL 3.1", { compatibility.openGL31.toString() }, true)
        label("OpenGL 3.2", { compatibility.openGL32.toString() }, true)
        label("OpenGL 3.3", { compatibility.openGL33.toString() }, true)
        label("OpenGL 4.0", { compatibility.openGL40.toString() }, true)
        label("OpenGL 4.1", { compatibility.openGL41.toString() }, true)
        label("OpenGL 4.2", { compatibility.openGL42.toString() }, true)
        label("OpenGL 4.3", { compatibility.openGL43.toString() }, true)
        label("OpenGL 4.4", { compatibility.openGL44.toString() }, true)
        label("OpenGL 4.5", { compatibility.openGL45.toString() }, true)
        label("OpenGL 4.6", { compatibility.openGL46.toString() }, true)
        label("GL_MAX_ARRAY_TEXTURE_LAYERS", { compatibility.maxArrayTextureLayers.toString() }, true)
        label("GL_EXT_texture_array", { compatibility.extTextureArray.toString() }, true)
        label("GL_ARB_framebuffer_no_attachments", { compatibility.arbFramebufferNoAttachments.toString() }, true)
        label("GL_ARB_sparse_texture", { compatibility.arbSparseTexture.toString() }, true)
        label("GL_ARB_sparse_texture2", { compatibility.arbSparseTexture2.toString() }, true)
    }
}