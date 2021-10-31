package cum.xiaro.trollhack.gui.mc

import cum.xiaro.trollhack.util.command.args.AbstractArg
import cum.xiaro.trollhack.util.command.args.AutoComplete
import cum.xiaro.trollhack.util.command.args.GreedyStringArg
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.command.CommandManager
import cum.xiaro.trollhack.module.modules.client.GuiSetting
import cum.xiaro.trollhack.util.accessor.historyBuffer
import cum.xiaro.trollhack.util.accessor.sentHistoryCursor
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.shaders.WindowBlurShader
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.threads.defaultScope
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiChat
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP
import kotlin.math.min

class TrollGuiChat(
    startStringIn: String,
    private val historyBufferIn: String? = null,
    private val sentHistoryCursorIn: Int? = null
) : GuiChat(startStringIn) {

    override fun initGui() {
        super.initGui()
        historyBufferIn?.let { historyBuffer = it }
        sentHistoryCursorIn?.let { sentHistoryCursor = it }
    }

    private var predictString = ""
    private var cachePredict = ""
    private var canAutoComplete = false

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (guiChatKeyTyped(typedChar, keyCode)) return

        if (!inputField.text.startsWith(CommandManager.prefix)) {
            displayNormalChatGUI()
            return
        }

        if (canAutoComplete && keyCode == Keyboard.KEY_TAB && predictString.isNotBlank()) {
            inputField.text += predictString
            predictString = ""
        }

        // Async offloading
        defaultScope.launch {
            cachePredict = ""
            canAutoComplete = false
            autoComplete()
            predictString = cachePredict
        }
    }

    private fun guiChatKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        return if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            true
        } else if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_NUMPADENTER) {
            val chatGUI = mc.ingameGUI.chatGUI
            when (keyCode) {
                Keyboard.KEY_UP -> getSentHistory(-1)
                Keyboard.KEY_DOWN -> getSentHistory(1)
                Keyboard.KEY_PRIOR -> chatGUI.scroll(chatGUI.lineCount - 1)
                Keyboard.KEY_NEXT -> chatGUI.scroll(-chatGUI.lineCount + 1)
                else -> inputField.textboxKeyTyped(typedChar, keyCode)
            }
            false
        } else {
            val message = inputField.text.trim()
            if (message.isNotEmpty()) sendChatMessage(message)
            mc.ingameGUI.chatGUI.addToSentMessages(message)
            mc.displayGuiScreen(null)
            true
        }
    }

    private fun displayNormalChatGUI() {
        GuiChat(inputField.text).also {
            mc.displayGuiScreen(it)
            it.historyBuffer = this.historyBuffer
            it.sentHistoryCursor = this.sentHistoryCursor
        }
    }

    private suspend fun autoComplete() {
        val string = inputField.text.removePrefix(CommandManager.prefix)
        val parsedArgs = runCatching { CommandManager.parseArguments(string) }.getOrNull() ?: return
        var argCount = parsedArgs.size - 1
        val inputName = parsedArgs[0]

        // If the string ends with only one space (typing the next arg), adds 1 to the arg count
        if (string.endsWith(' ') && !string.endsWith("  ")) {
            argCount += 1
        }

        // Run commandAutoComplete() and return if there are only one arg
        if (argCount == 0) {
            commandAutoComplete(inputName)
            return
        }

        val ignoredStringArg = getArgTypeForAtIndex(parsedArgs, argCount, true)
        val withStringArg = getArgTypeForAtIndex(parsedArgs, argCount, false)

        // Get available arg types for current arg index
        val args = ignoredStringArg
            ?: withStringArg
            ?: return

        val both = if (ignoredStringArg != null && withStringArg != null) {
            ignoredStringArg + withStringArg
        } else {
            args
        }

        // Get the current input string
        val inputString = parsedArgs.getOrNull(argCount)

        if (inputString.isNullOrEmpty()) {
            // If we haven't input anything yet, prints list of available arg types
            if (args.isNotEmpty()) cachePredict = both.distinct().joinToString("/")
            return
        }

        // Set cache predict to the first arg that impls AutoComplete
        // And the auto complete result isn't null
        for (arg in args) {
            if (arg !is AutoComplete) continue
            val result = arg.completeForInput(inputString)
            if (result != null) {
                cachePredict = result.substring(min(inputString.length, result.length))
                canAutoComplete = true
                break // Stop the iteration here because we get the non null result already
            }
        }
    }

    private fun commandAutoComplete(inputName: String) {
        CommandManager.getCommands().asSequence()
            .flatMap { command -> command.allNames.asSequence().map { it.toString() } }
            .filter { it.length >= inputName.length }
            .filter { it.startsWith(inputName) }
            .minOrNull()
            ?.let {
                cachePredict = it.substring(min(inputName.length, it.length))
                canAutoComplete = true
            }
    }

    private suspend fun getArgTypeForAtIndex(parsedArgs: Array<String>, argIndex: Int, ignoreStringArg: Boolean): List<AbstractArg<*>>? {
        // Get the command for input name, map the arg trees to the count of match args
        val command = CommandManager.getCommandOrNull(parsedArgs[0]) ?: return null
        val treeMatchedCounts = command.finalArgs.mapNotNull {
            if (ignoreStringArg && it.getArgTree().getOrNull(argIndex) is GreedyStringArg) {
                null
            } else {
                it.countArgs(parsedArgs) to it
            }
        }

        // Get the max matched number of args, filter all trees that has less matched args
        // And map to the current arg in the tree if exists
        val maxMatches = treeMatchedCounts.maxOfOrNull { it.first } ?: return null
        return treeMatchedCounts.asSequence()
            .filter { it.first == maxMatches }
            .mapNotNull { it.second.getArgTree().getOrNull(argIndex) }
            .toList()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        glEnable(GL_DEPTH_CLAMP)

        // Draw rect background
        val pos1 = Vec2f(2.0f, height - 14.0f)
        val pos2 = Vec2f(width - 2.0f, height - 2.0f)
        if (GuiSetting.windowBlur) {
            WindowBlurShader.render(2.0f, height - 14.0f, width - 2.0f, height - 2.0f)
        }
        RenderUtils2D.drawRectFilled(pos1, pos2, ColorRGB(0, 0, 0, 128))

        // Draw predict string
        if (predictString.isNotBlank()) {
            val posX = (fontRenderer.getStringWidth(inputField.text) + inputField.x).toFloat()
            val posY = (inputField.y).toFloat()
            fontRenderer.drawStringWithShadow(predictString, posX, posY, 0x808080)
        }

        // Draw normal string
        inputField.drawTextBox()

        // Draw outline around input field
        val pos3 = Vec2f(inputField.x - 2.0f, inputField.y - 2.0f)
        val pos4 = pos3.plus(inputField.width.toFloat(), inputField.height.toFloat())
        RenderUtils2D.drawRectOutline(pos3, pos4, 1.0f, GuiSetting.primary)
    }

}
