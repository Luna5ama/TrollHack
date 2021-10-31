package cum.xiaro.trollhack.util.graphics.font

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.graphics.HAlign
import cum.xiaro.trollhack.util.graphics.VAlign
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.Vec2d
import org.lwjgl.opengl.GL11.*
import kotlin.math.max

/**
 * Renders multi line text easily
 */
class TextComponent(val separator: String = " ") {
    private val textLines = ArrayList<TextLine?>()
    var currentLine = 0
        set(value) {
            field = max(value, 0)
        } // Can not be smaller than 0

    /**
     * Create a new copy of a text component
     */
    constructor(textComponent: TextComponent) : this(textComponent.separator) {
        this.textLines.addAll(textComponent.textLines)
        this.currentLine = textComponent.currentLine
    }

    /**
     * Create a new text component from a multi line string
     */
    constructor(string: String, separator: String = " ", vararg delimiters: String = arrayOf(separator)) : this(separator) {
        val lines = string.lines()
        for (line in lines) {
            for (splitText in line.split(delimiters = delimiters)) {
                add(splitText)
            }
            if (line != lines.last()) currentLine++
        }
    }

    /**
     * Adds new text element to [currentLine], and goes to the next line
     */
    fun addLine(text: String, color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        add(text, color, style)
        currentLine++
    }

    /**
     * Adds new text element to [currentLine], and goes to the next line
     */
    fun addLine(textElement: TextElement) {
        add(textElement)
        currentLine++
    }

    /**
     * Adds new text element to [currentLine]
     */
    fun add(text: String, color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        add(TextElement(text, color, style))
    }

    /**
     * Adds new text element to [currentLine]
     */
    fun add(textElement: TextElement) {
        // Adds new lines until we reached the current line
        while (textLines.size <= currentLine) textLines.add(null)

        // Add text element to current line, and create new text line object if current line has null
        textLines[currentLine] = (textLines[currentLine] ?: TextLine(separator)).apply { this.add(textElement) }
    }

    /**
     * Clear all lines in this component, and reset [currentLine]
     */
    fun clear() {
        textLines.clear()
        currentLine = 0
    }

    /**
     * Draws all lines in this component
     */
    fun draw(
        pos: Vec2d = Vec2d.ZERO,
        lineSpace: Int = 2,
        alpha: Float = 1.0f,
        scale: Float = 1f,
        skipEmptyLine: Boolean = true,
        horizontalAlign: HAlign = HAlign.LEFT,
        verticalAlign: VAlign = VAlign.TOP
    ) {
        if (isEmpty()) return

        glPushMatrix()
        glTranslated(pos.x, pos.y - 1.0, 0.0)
        glScalef(scale, scale, 1f)

        if (verticalAlign != VAlign.TOP) {
            var height = getHeight(lineSpace)
            if (verticalAlign == VAlign.CENTER) height /= 2
            glTranslatef(0f, -height, 0f)
        }

        for (line in textLines) {
            if (skipEmptyLine && (line == null || line.isEmpty())) continue
            line?.drawLine(alpha, horizontalAlign)
            glTranslatef(0f, (MainFontRenderer.getHeight() + lineSpace), 0f)
        }

        glPopMatrix()
    }

    fun isEmpty() = textLines.firstOrNull { it?.isEmpty() == false } == null

    fun getWidth() = textLines
        .maxOfOrNull { it?.getWidth() ?: 0.0f } ?: 0.0f

    fun getHeight(lineSpace: Int, skipEmptyLines: Boolean = true) =
        MainFontRenderer.getHeight() * getLines(skipEmptyLines) + lineSpace * (getLines(skipEmptyLines) - 1)

    fun getLines(skipEmptyLines: Boolean = true) = textLines.count { !skipEmptyLines || (it != null && !it.isEmpty()) }

    override fun toString() = textLines.joinToString(separator = "\n")

    class TextLine(private val separator: String) {
        private val textElementList = ArrayList<TextElement>()
        private var cachedString = ""

        fun isEmpty() = textElementList.size == 0

        fun add(textElement: TextElement) {
            textElementList.add(textElement)
            updateCache()
        }

        fun drawLine(alpha: Float, horizontalAlign: HAlign) {
            glPushMatrix()

            if (horizontalAlign != HAlign.LEFT) {
                var width = getWidth()
                if (horizontalAlign == HAlign.CENTER) width /= 2.0f
                glTranslatef(-width, 0f, 0f)
            }

            for (textElement in textElementList) {
                var color = textElement.color
                color = color.alpha((color.a * alpha).toInt())
                MainFontRenderer.drawString(textElement.text, color = color)
                val adjustedSeparator = if (separator == " ") "  " else separator
                glTranslatef(MainFontRenderer.getWidth(textElement.text + adjustedSeparator), 0f, 0f)
            }

            glPopMatrix()
        }

        fun getWidth(): Float {
            return MainFontRenderer.getWidth(cachedString)
        }

        fun reverse() {
            textElementList.reverse()
            updateCache()
        }

        private fun updateCache() {
            val adjustedSeparator = if (separator == " ") "  " else separator
            cachedString = textElementList.joinToString(separator = adjustedSeparator)
        }
    }

    class TextElement(textIn: String, val color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        val text = "${style.code}$textIn"

        override fun toString(): String {
            return text
        }
    }
}