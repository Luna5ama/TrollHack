package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.manager.managers.UnicodeFontManager
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.matrix.scalef
import dev.luna5ama.trollhack.graphics.matrix.scope
import dev.luna5ama.trollhack.graphics.matrix.translatef
import dev.luna5ama.trollhack.utils.math.vectors.HAlign
import dev.luna5ama.trollhack.utils.math.vectors.VAlign
import dev.luna5ama.trollhack.utils.math.vectors.Vec2d
import kotlin.math.max
import dev.luna5ama.trollhack.RenderSystem as RS

class TextComponent(val separator: String = " ", val font: FontRenderer = UnicodeFontManager.CURRENT_FONT) {
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
    constructor(
        string: String, separator: String = " ",
        vararg delimiters: String = arrayOf(separator)) : this(separator) {
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
    fun addLine(text: String, color: ColorRGBA = ColorRGBA(255, 255, 255), style: Style = Style.REGULAR) {
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
    fun add(text: String, color: ColorRGBA = ColorRGBA(255, 255, 255), style: Style = Style.REGULAR) {
        add(TextElement(text, color, style))
    }

    /**
     * Adds new text element to [currentLine]
     */
    fun add(textElement: TextElement) {
        // Adds new lines until we reached the current line
        while (textLines.size <= currentLine) textLines.add(null)

        // Add text element to current line, and create new text line object if current line has null
        textLines[currentLine] = (textLines[currentLine] ?: TextLine(separator, font)).apply { this.add(textElement) }
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
    @JvmOverloads
    fun draw(
        pos: Vec2d = Vec2d.ZERO,
        lineSpace: Int = 0,
        alpha: Float = 1f,
        scale: Float = 1f,
        skipEmptyLine: Boolean = true,
        horizontalAlign: HAlign = HAlign.LEFT,
        verticalAlign: VAlign = VAlign.TOP,
        shadow: Boolean = true
    ) {
        if (isEmpty()) return

        RS.matrixLayer.scope {
            translatef(pos.x.toFloat(), pos.y.toFloat() - 1f, 0f)
            scalef(scale, scale, 1f)

            if (verticalAlign != VAlign.TOP) {
                var height = getHeight(lineSpace)
                if (verticalAlign == VAlign.CENTER) height /= 2
                translatef(0f, -height, 0f)
            }

            for (line in textLines) {
                if (skipEmptyLine && (line == null || line.isEmpty())) continue
                line?.drawLine(alpha, horizontalAlign, shadow)
                translatef(0f, (font.height + lineSpace), 0f)
            }
        }
    }

    fun isEmpty() = textLines.firstOrNull { it?.isEmpty() == false } == null
    fun isNotEmpty() = !isEmpty()

    fun getWidth() = textLines
        .maxOfOrNull { it?.getWidth() ?: 0.0f } ?: 0.0f

    fun getHeight(lineSpace: Int, skipEmptyLines: Boolean = true) =
        font.height * getLines(skipEmptyLines) + lineSpace * (getLines(skipEmptyLines) - 1)

    fun getLines(skipEmptyLines: Boolean = true) = textLines.count { !skipEmptyLines || (it != null && !it.isEmpty()) }

    override fun toString() = textLines.joinToString(separator = "\n")

    class TextLine(private val separator: String, val font: FontRenderer = UnicodeFontManager.CURRENT_FONT) {
        private val textElementList = ArrayList<TextElement>()
        var cachedString = ""; private set

        fun isEmpty() = textElementList.size == 0

        fun add(textElement: TextElement) {
            textElementList.add(textElement)
            updateCache()
        }

        fun drawLine(alpha: Float, horizontalAlign: HAlign, shadow: Boolean = true) {
            RS.matrixLayer.scope {
                if (horizontalAlign != HAlign.LEFT) {
                    var width = getWidth()
                    if (horizontalAlign == HAlign.CENTER) width /= 2.0f
                    translatef(-width, 0f, 0f)
                }

                for (textElement in textElementList) {
                    var color = textElement.color
                    color = color.alpha((255 * alpha).toInt())
                    if (shadow) font.drawStringWithShadow(textElement.text, color = color)
                    else font.drawString(textElement.text, color = color)
                    val adjustedSeparator = if (separator == " ") "  " else separator
                    translatef(
                        font.getWidth(textElement.text) + font.getWidth(adjustedSeparator),
                        0f,
                        0f
                    )
                }
            }
        }

        fun getWidth(): Float {
            return font.getWidth(cachedString)
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

    class TextElement(textIn: String, val color: ColorRGBA = ColorRGBA(255, 255, 255), style: Style = Style.REGULAR) {
        val text = "${style.code}$textIn"

        override fun toString(): String {
            return text
        }
    }
}

