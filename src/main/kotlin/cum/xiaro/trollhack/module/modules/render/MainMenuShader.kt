package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.extension.mapEach
import cum.xiaro.trollhack.util.extension.normalizeCase
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.graphics.shaders.GLSLSandbox
import org.lwjgl.input.Mouse
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*

internal object MainMenuShader : Module(
    name = "MainMenuShader",
    description = "Replace main menu background with shader",
    visible = false,
    category = Category.RENDER,
    enabledByDefault = true
) {
    private val mode by setting("Mode", Mode.SET)
    private val shader by setting("Shader", ShaderEnum.SPACE, { mode == Mode.SET })
    private val fpsLimit by setting("Fps Limit", 60, 10..240, 10)

    private enum class Mode {
        RANDOM, SET
    }

    @Suppress("UNUSED")
    private enum class ShaderEnum {
        BLUE_GRID,
        BLUE_LANDSCAPE,
        CIRCUITS,
        CUBE_CAVE,
        GREEN_NEBULA,
        GRID_CAVE,
        MATRIX,
        MINECRAFT,
        PURPLE_GRID,
        RECT_WAVES,
        RED_LANDSCAPE,
        SPACE,
        TUBE;

        val path = name
            .mapEach('_') { it.normalizeCase() }
            .joinToString("", "/assets/trollhack/shaders/menu/", ".fsh")
    }

    private val shaderCache = EnumMap<ShaderEnum, GLSLSandbox>(ShaderEnum::class.java)
    private var initTime: Long = 0x22
    private var currentShader = getShader()

    @JvmStatic
    fun handleGetLimitFramerate(cir: CallbackInfoReturnable<Int>) {
        if (isEnabled && mc.world == null && mc.currentScreen != null) {
            cir.returnValue = fpsLimit
        }
    }

    @JvmStatic
    fun render() {
        val width = mc.displayWidth.toFloat()
        val height = mc.displayHeight.toFloat()
        val mouseX = Mouse.getX() - 1.0f
        val mouseY = height - Mouse.getY() - 1.0f

        currentShader.render(width, height, mouseX, mouseY, initTime)
    }

    @JvmStatic
    fun reset() {
        initTime = System.currentTimeMillis()
        currentShader = getShader()
    }

    private fun getShader(): GLSLSandbox {
        val shader = if (mode == Mode.RANDOM) {
            ShaderEnum.values().random()
        } else {
            shader
        }

        return shaderCache.getOrPut(shader) {
            GLSLSandbox(shader.path)
        }
    }
}