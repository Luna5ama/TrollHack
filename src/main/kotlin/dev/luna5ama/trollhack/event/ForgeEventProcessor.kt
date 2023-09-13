package dev.luna5ama.trollhack.event

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.InputEvent.*
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent.Load
import dev.luna5ama.trollhack.event.events.WorldEvent.Unload
import dev.luna5ama.trollhack.event.events.baritone.BaritoneCommandEvent
import dev.luna5ama.trollhack.event.events.player.InteractEvent
import dev.luna5ama.trollhack.event.events.player.PlayerPushOutOfBlockEvent
import dev.luna5ama.trollhack.event.events.render.FogColorEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.events.render.RenderOverlayEvent
import dev.luna5ama.trollhack.event.events.render.ResolutionUpdateEvent
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.ProjectionUtils
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.gui.mc.TrollGuiChat
import dev.luna5ama.trollhack.manager.managers.WorldManager
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.text.MessageDetection
import net.minecraftforge.client.event.*
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL20.glUseProgram
import java.util.*

@Suppress("UNUSED_PARAMETER")
internal object ForgeEventProcessor : ListenerOwner() {
    private val mc = Wrapper.minecraft
    private var prevWidth = -1
    private var prevHeight = -1

    init {
        listener<TickEvent.Post>(true) {
            if (prevWidth != mc.displayWidth || prevHeight != mc.displayHeight) {
                prevWidth = mc.displayWidth
                prevHeight = mc.displayHeight
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
                glUseProgram(0)
            }
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        ProjectionUtils.updateMatrix()
        RenderUtils3D.prepareGL()
        Render3DEvent.post()
        RenderUtils3D.releaseGL()
        glUseProgram(0)
    }

    @SubscribeEvent
    fun onRenderGameOverlayPre(event: RenderGameOverlayEvent.Pre) {
        GlStateUtils.alpha(false)
        RenderOverlayEvent.Pre(event).post()
        GlStateUtils.alpha(true)
    }

    @SubscribeEvent
    fun onRenderGameOverlayPost(event: RenderGameOverlayEvent.Post) {
        GlStateUtils.alpha(false)
        RenderOverlayEvent.Post(event).post()
        GlStateUtils.alpha(true)
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        val state = Keyboard.getEventKeyState()
        Keyboard(key, state).post()

        if (!state) return

        if (!mc.gameSettings.keyBindSneak.isKeyDown) {
            val prefix = CommandManager.prefix
            val typedChar = Keyboard.getEventCharacter().toString()
            if (prefix.length == 1 && typedChar.equals(CommandManager.prefix, true)) {
                mc.displayGuiScreen(TrollGuiChat(CommandManager.prefix))
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChatSent(event: ClientChatEvent) {
        MessageDetection.Command.BARITONE.removedOrNull(event.message)?.let {
            BaritoneCommandEvent(it.toString().substringBefore(' ').lowercase(Locale.ROOT)).post()
        }

        if (MessageDetection.Command.TROLL_HACK detect event.message) {
            CommandManager.runCommand(event.message.removePrefix(CommandManager.prefix))
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEventMouse(event: InputEvent.MouseInputEvent) {
        Mouse(Mouse.getEventButton(), Mouse.getEventButtonState()).post()
    }

    @SubscribeEvent
    fun onRenderBlockOverlay(event: RenderBlockOverlayEvent) {
        dev.luna5ama.trollhack.event.events.render.RenderBlockOverlayEvent(event).post()
    }

    @SubscribeEvent
    fun onInputUpdate(event: InputUpdateEvent) {
        dev.luna5ama.trollhack.event.events.player.InputUpdateEvent(event).post()
    }

    @SubscribeEvent
    fun onClientDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        ConnectionEvent.Disconnect.post()
    }

    @SubscribeEvent
    fun onClientConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        ConnectionEvent.Connect.post()
    }

    @SubscribeEvent
    fun onRenderFogColors(event: EntityViewRenderEvent.FogColors) {
        FogColorEvent(event).post()
    }

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        InteractEvent.Item.RightClick(event.hand).post()
    }

    @SubscribeEvent
    fun onLoadWorld(event: WorldEvent.Load) {
        if (event.world.isRemote) {
            event.world.addEventListener(WorldManager)
            Load.post()
        }
    }

    @SubscribeEvent
    fun onUnloadWorld(event: WorldEvent.Unload) {
        if (event.world.isRemote) {
            event.world.removeEventListener(WorldManager)
            Unload.post()
        }
    }

    @SubscribeEvent
    fun onPlayerSPPushOutOfBlocks(event: PlayerSPPushOutOfBlocksEvent) {
        PlayerPushOutOfBlockEvent(event).post()
    }
}
