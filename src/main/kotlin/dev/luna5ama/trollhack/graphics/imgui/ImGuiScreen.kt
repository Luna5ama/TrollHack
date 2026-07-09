package dev.luna5ama.trollhack.graphics.imgui

import com.mojang.blaze3d.vertex.PoseStack
import imgui.ImGui
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

open class ImGuiScreen(val name: String) : Screen(Component.literal(name)) {
    private var frames = 0

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        startFrame()
        preProcess()
        process()
        postProcess()
        endFrame()
    }

    override fun init() {
        frames = 0
    }

    protected open fun preProcess() {
        ImGui.pushFont(ClientSettings.guiFont.font().imGuiFont)
        ImGui.getStyle()!!.apply {
            windowRounding = 5f
            frameRounding = 5f
            grabRounding = 5f
            colors[ImGuiCol.Text] = colorConvertU32ToFloat4(Spectrum.GRAY800) // text on hovered controls is gray900
            colors[ImGuiCol.TextDisabled] = colorConvertU32ToFloat4(Spectrum.GRAY500)
            colors[ImGuiCol.WindowBg] = colorConvertU32ToFloat4(Spectrum.GRAY100)
            colors[ImGuiCol.ChildBg] = ImVec4(0.00f, 0.00f, 0.00f, 0.00f)
            colors[ImGuiCol.PopupBg] = colorConvertU32ToFloat4(Spectrum.GRAY50) // not sure about this. Note: applies to tooltips too.
            colors[ImGuiCol.Border] = colorConvertU32ToFloat4(Spectrum.GRAY300)
            colors[ImGuiCol.BorderShadow] = colorConvertU32ToFloat4(Spectrum.NONE) // We don't want shadows. Ever.
            colors[ImGuiCol.FrameBg] = colorConvertU32ToFloat4(Spectrum.GRAY75) // this isnt right, spectrum does not do this, but it's a good fallback
            colors[ImGuiCol.FrameBgHovered] = colorConvertU32ToFloat4(Spectrum.GRAY50)
            colors[ImGuiCol.FrameBgActive] = colorConvertU32ToFloat4(Spectrum.GRAY200)
            colors[ImGuiCol.TitleBg] = colorConvertU32ToFloat4(Spectrum.GRAY300) // those titlebar values are totally made up, spectrum does not have this.
            colors[ImGuiCol.TitleBgActive] = colorConvertU32ToFloat4(Spectrum.GRAY200)
            colors[ImGuiCol.TitleBgCollapsed] = colorConvertU32ToFloat4(Spectrum.GRAY400)
            colors[ImGuiCol.MenuBarBg] = colorConvertU32ToFloat4(Spectrum.GRAY100)
            colors[ImGuiCol.ScrollbarBg] = colorConvertU32ToFloat4(Spectrum.GRAY100) // same as regular background
            colors[ImGuiCol.ScrollbarGrab] = colorConvertU32ToFloat4(Spectrum.GRAY400)
            colors[ImGuiCol.ScrollbarGrabHovered] = colorConvertU32ToFloat4(Spectrum.GRAY600)
            colors[ImGuiCol.ScrollbarGrabActive] = colorConvertU32ToFloat4(Spectrum.GRAY700)
            colors[ImGuiCol.CheckMark] = colorConvertU32ToFloat4(Spectrum.BLUE500)
            colors[ImGuiCol.SliderGrab] = colorConvertU32ToFloat4(Spectrum.GRAY700)
            colors[ImGuiCol.SliderGrabActive] = colorConvertU32ToFloat4(Spectrum.GRAY800)
            colors[ImGuiCol.Button] = colorConvertU32ToFloat4(Spectrum.GRAY75) // match default button to Spectrum's 'Action Button'.
            colors[ImGuiCol.ButtonHovered] = colorConvertU32ToFloat4(Spectrum.GRAY50)
            colors[ImGuiCol.ButtonActive] = colorConvertU32ToFloat4(Spectrum.GRAY200)
            colors[ImGuiCol.Header] = colorConvertU32ToFloat4(Spectrum.BLUE400)
            colors[ImGuiCol.HeaderHovered] = colorConvertU32ToFloat4(Spectrum.BLUE500)
            colors[ImGuiCol.HeaderActive] = colorConvertU32ToFloat4(Spectrum.BLUE600)
            colors[ImGuiCol.Separator] = colorConvertU32ToFloat4(Spectrum.GRAY400)
            colors[ImGuiCol.SeparatorHovered] = colorConvertU32ToFloat4(Spectrum.GRAY600)
            colors[ImGuiCol.SeparatorActive] = colorConvertU32ToFloat4(Spectrum.GRAY700)
            colors[ImGuiCol.ResizeGrip] = colorConvertU32ToFloat4(Spectrum.GRAY400)
            colors[ImGuiCol.ResizeGripHovered] = colorConvertU32ToFloat4(Spectrum.GRAY600)
            colors[ImGuiCol.ResizeGripActive] = colorConvertU32ToFloat4(Spectrum.GRAY700)
            colors[ImGuiCol.PlotLines] = colorConvertU32ToFloat4(Spectrum.BLUE400)
            colors[ImGuiCol.PlotLinesHovered] = colorConvertU32ToFloat4(Spectrum.BLUE600)
            colors[ImGuiCol.PlotHistogram] = colorConvertU32ToFloat4(Spectrum.BLUE400)
            colors[ImGuiCol.PlotHistogramHovered] = colorConvertU32ToFloat4(Spectrum.BLUE600)
            colors[ImGuiCol.TextSelectedBg] = colorConvertU32ToFloat4((Spectrum.BLUE400 and 0x00FFFFFF) or 0x33000000)
            colors[ImGuiCol.DragDropTarget] = ImVec4(1.00f, 1.00f, 0.00f, 0.90f)
            colors[ImGuiCol.NavHighlight] = colorConvertU32ToFloat4((Spectrum.GRAY900 and 0x00FFFFFF) or 0x0A000000)
            colors[ImGuiCol.NavWindowingHighlight] = ImVec4(1.00f, 1.00f, 1.00f, 0.70f)
            colors[ImGuiCol.NavWindowingDimBg] = ImVec4(0.80f, 0.80f, 0.80f, 0.20f)
            colors[ImGuiCol.ModalWindowDimBg] = ImVec4(0.20f, 0.20f, 0.20f, 0.35f)
        }
    }

    protected open fun process() {

    }

    protected open fun postProcess() {
        ImGui.popFont()
    }

    protected open fun startFrame() {
        imGuiImplGl3.newFrame()
        imGuiImplGlfw.newFrame()
        ImGui.newFrame()
    }

    protected fun endFrame() {
        ImGui.render()
        imGuiImplGl3.renderDrawData(ImGui.getDrawData())

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val backupWindowPtr = GLFW.glfwGetCurrentContext()
            ImGui.updatePlatformWindows()
            ImGui.renderPlatformWindowsDefault()
            GLFW.glfwMakeContextCurrent(backupWindowPtr)
        }

        frames++
    }

    companion object {
        val imGuiImplGlfw = ImGuiImplGlfw()
        val imGuiImplGl3 = ImGuiImplGl3()

        init {
            ImGui.createContext()

            ImGui.styleColorsLight()
            imGuiImplGlfw.init(mc.window.handle(), true)
            imGuiImplGl3.init("#version 130")
        }
    }
}