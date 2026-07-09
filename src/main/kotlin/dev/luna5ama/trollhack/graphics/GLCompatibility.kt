package dev.luna5ama.trollhack.graphics

import org.lwjgl.opengl.ARBSparseTexture.GL_MAX_SPARSE_ARRAY_TEXTURE_LAYERS_ARB
import org.lwjgl.opengl.ARBSparseTexture.GL_MAX_SPARSE_TEXTURE_SIZE_ARB
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_MAX_ARRAY_TEXTURE_LAYERS
import org.lwjgl.opengl.GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.GLXCapabilities
import org.lwjgl.opengl.WGLCapabilities

class GLCompatibility(
    val glContext: GLCapabilities,
    val wglContext: WGLCapabilities? = null,
    val glxContext: GLXCapabilities? = null
) {

    // OpenGL version
    val glVersion = glGetString(GL_VERSION) ?: ""
    val gpuManufacturer = glGetString(GL_VENDOR) ?: ""
    val gpuName = glGetString(GL_RENDERER)?.substringBefore("/") ?: ""

    val intelGraphics = glVersion.lowercase().contains("intel")
            || gpuManufacturer.lowercase().contains("intel")
            || gpuName.lowercase().contains("intel")

    val amdGraphics = glVersion.lowercase().contains("amd")
            || gpuManufacturer.lowercase().contains("amd")
            || gpuName.lowercase().contains("amd")

    val nvidiaGraphics = glVersion.lowercase().contains("nvidia")
            || gpuManufacturer.lowercase().contains("nvidia")
            || gpuName.lowercase().contains("nvidia")

    val otherGraphics = !intelGraphics && !amdGraphics && !nvidiaGraphics

    val openGL11 = glContext.OpenGL11
    val openGL12 = glContext.OpenGL12
    val openGL13 = glContext.OpenGL13
    val openGL14 = glContext.OpenGL14
    val openGL15 = glContext.OpenGL15
    val openGL20 = glContext.OpenGL20
    val openGL21 = glContext.OpenGL21
    val openGL30 = glContext.OpenGL30
    val openGL31 = glContext.OpenGL31
    val openGL32 = glContext.OpenGL32
    val openGL33 = glContext.OpenGL33
    val openGL40 = glContext.OpenGL40
    val openGL41 = glContext.OpenGL41
    val openGL42 = glContext.OpenGL42
    val openGL43 = glContext.OpenGL43
    val openGL44 = glContext.OpenGL44
    val openGL45 = glContext.OpenGL45
    val openGL46 = glContext.OpenGL46 || (openGL45 && intelGraphics) // 傻逼Intel

    // ARB
    val arbShaders: Boolean = !glContext.OpenGL21
    val arbVbo: Boolean = !glContext.OpenGL15 && glContext.GL_ARB_vertex_buffer_object
    val arbMultiTexture: Boolean = glContext.GL_ARB_multitexture && !glContext.OpenGL13
    val arbFramebufferNoAttachments = glContext.GL_ARB_framebuffer_no_attachments && glContext.OpenGL42
    val arbDirectStateAccess = glContext.GL_ARB_direct_state_access
    val arbDebugOutput = glContext.GL_ARB_debug_output
    val arbSparseTexture = glContext.GL_ARB_sparse_texture
    val arbSparseTexture2 = glContext.GL_ARB_sparse_texture2

    // NV
    val nvDXInterop = wglContext?.WGL_NV_DX_interop == true

    // EXT
    val extBlendFuncSeparate = glContext.GL_EXT_blend_func_separate && !glContext.OpenGL14
    val extFramebufferObject = glContext.GL_EXT_framebuffer_object && !glContext.OpenGL30
    val extTextureArray = glContext.GL_EXT_texture_array && openGL45

    val maxArrayTextureLayers = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS)
    val maxSparseTextureSizeARB = glGetInteger(GL_MAX_SPARSE_TEXTURE_SIZE_ARB)
    val maxSparseArrayTextureLayersARB = glGetInteger(GL_MAX_SPARSE_ARRAY_TEXTURE_LAYERS_ARB)
    val maxComputeWorkGroupSize = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE)

    val openGLVersion = run {
        when {
            openGL46 -> "4.6"
            openGL45 -> "4.5"
            openGL44 -> "4.4"
            openGL43 -> "4.3"
            openGL42 -> "4.2"
            openGL41 -> "4.1"
            openGL40 -> "4.0"
            openGL33 -> "3.3"
            openGL32 -> "3.2"
            openGL31 -> "3.1"
            openGL30 -> "3.0"
            openGL21 -> "2.1"
            openGL20 -> "2.0"
            openGL15 -> "1.5"
            openGL14 -> "1.4"
            openGL13 -> "1.3"
            openGL12 -> "1.2"
            openGL11 -> "1.1"
            else -> throw Exception("Unsupported graphics card")
        }
    }

}