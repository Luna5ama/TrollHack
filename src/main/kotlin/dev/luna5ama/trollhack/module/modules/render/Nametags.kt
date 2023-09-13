package dev.luna5ama.trollhack.module.modules.render

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.floorToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.ProjectionUtils
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorGradient
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.Style
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.TotemPopManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.*
import dev.luna5ama.trollhack.util.extension.remove
import dev.luna5ama.trollhack.util.inventory.originalName
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.math.vector.lerp
import dev.luna5ama.trollhack.util.text.unformatted
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.GL_QUADS
import org.lwjgl.opengl.GL11.glColor4f
import org.lwjgl.opengl.GL20.glUseProgram
import java.util.*
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

internal object Nametags : Module(
    name = "Nametags",
    description = "Draws descriptive nametags above entities",
    category = Category.RENDER
) {
    private val page = setting("Page", Page.ENTITY_TYPE)

    /* Entity type settings */
    private val self = setting("Self", false, page.atValue(Page.ENTITY_TYPE))
    private val experience = setting("Experience", false, page.atValue(Page.ENTITY_TYPE))
    private val items = setting("Items", true, page.atValue(Page.ENTITY_TYPE))
    private val players = setting("Players", true, page.atValue(Page.ENTITY_TYPE))
    private val mobs = setting("Mobs", true, page.atValue(Page.ENTITY_TYPE))
    private val passive = setting("Passive Mobs", false, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue())
    private val neutral = setting("Neutral Mobs", true, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue())
    private val hostile = setting("Hostile Mobs", true, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue())
    private val invisible = setting("Invisible", true, page.atValue(Page.ENTITY_TYPE))
    private val range = setting("Range", 64, 0..512, 4, page.atValue(Page.ENTITY_TYPE))

    /* Content */
    private val line1left = setting("Line 1 Left", ContentType.NONE, page.atValue(Page.CONTENT))
    private val line1center = setting("Line 1 Center", ContentType.NONE, page.atValue(Page.CONTENT))
    private val line1right = setting("Line 1 Right", ContentType.NONE, page.atValue(Page.CONTENT))
    private val line2left = setting("Line 2 Left", ContentType.NAME, page.atValue(Page.CONTENT))
    private val line2center = setting("Line 2 Center", ContentType.PING, page.atValue(Page.CONTENT))
    private val line2right = setting("Line 2 Right", ContentType.TOTAL_HP, page.atValue(Page.CONTENT))
    private val dropItemCount = setting("Drop Item Count", true, page.atValue(Page.CONTENT) and items.atTrue())
    private val maxDropItems = setting("Max Drop Items", 5, 2..16, 1, page.atValue(Page.CONTENT) and items.atTrue())

    /* Item */
    private val mainHand = setting("Main Hand", true, page.atValue(Page.ITEM))
    private val offhand = setting("Off Hand", true, page.atValue(Page.ITEM))
    private val invertHand =
        setting("Invert Hand", false, page.atValue(Page.ITEM) and (mainHand.atTrue() or offhand.atTrue()))
    private val armor = setting("Armor", true, page.atValue(Page.ITEM))
    private val count =
        setting("Count", true, page.atValue(Page.ITEM) and (mainHand.atTrue() or offhand.atTrue() or armor.atTrue()))
    private val dura =
        setting("Dura", true, page.atValue(Page.ITEM) and (mainHand.atTrue() or offhand.atTrue() or armor.atTrue()))
    private val enchantment = setting(
        "Enchantment",
        true,
        page.atValue(Page.ITEM) and (mainHand.atTrue() or offhand.atTrue() or armor.atTrue())
    )
    private val itemScale = setting("Item Scale", 0.8f, 0.1f..2.0f, 0.1f, page.atValue(Page.ITEM))

    /* Rendering settings */
    private val margins = setting("Margins", 1.0f, 0.0f..4.0f, 0.1f, page.atValue(Page.RENDERING))
    private val rText = setting("Text Red", 232, 0..255, 1, page.atValue(Page.RENDERING))
    private val gText = setting("Text Green", 229, 0..255, 1, page.atValue(Page.RENDERING))
    private val bText = setting("Text Blue", 255, 0..255, 1, page.atValue(Page.RENDERING))
    private val aText = setting("Text Alpha", 255, 0..255, 1, page.atValue(Page.RENDERING))
    private val scale = setting("Scale", 1.3f, 0.1f..5f, 0.1f, page.atValue(Page.RENDERING))
    private val distScaleFactor =
        setting("Distance Scale Factor", 0.35f, 0.0f..1.0f, 0.05f, page.atValue(Page.RENDERING))
    private val minDistScale = setting("Min Distance Scale", 0.35f, 0.0f..1.0f, 0.05f, page.atValue(Page.RENDERING))

    private enum class Page {
        ENTITY_TYPE, CONTENT, ITEM, RENDERING
    }

    private enum class ContentType {
        NONE, NAME, TYPE, TOTAL_HP, HP, ABSORPTION, PING, DISTANCE, TOTEM_POPS
    }

    private val pingColorGradient = ColorGradient(
        ColorGradient.Stop(0f, ColorRGB(101, 101, 101)),
        ColorGradient.Stop(0.1f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(20f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(150f, ColorRGB(20, 232, 20)),
        ColorGradient.Stop(300f, ColorRGB(150, 0, 0))
    )

    private val healthColorGradient = ColorGradient(
        ColorGradient.Stop(0f, ColorRGB(180, 20, 20)),
        ColorGradient.Stop(50f, ColorRGB(240, 220, 20)),
        ColorGradient.Stop(100f, ColorRGB(20, 232, 20))
    )

    private val line1Settings = arrayOf(line1left, line1center, line1right)
    private val line2Settings = arrayOf(line2left, line2center, line2right)
    private var renderInfos = emptyList<IRenderInfo>()

    init {
        onDisable {
            renderInfos = emptyList()
        }

        listener<Render2DEvent.Absolute> {
            val camPos = RenderUtils3D.camPos
            val partialTicks = RenderUtils3D.partialTicks

            renderInfos.forEach {
                it.render(camPos, partialTicks)
            }
        }
    }

    init {
        safeParallelListener<TickEvent.Post> {
            val rangeSq = range.value.sq
            val list = ArrayList<IRenderInfo>()
            val itemList = ArrayList<ItemGroup>()

            loop@ for (entity in EntityManager.entity) {
                if (!checkEntityType(entity)) continue
                if (player.distanceSqTo(entity) > rangeSq) continue
                when (entity) {
                    is EntityLivingBase -> {
                        list.add(LivingRenderInfo(this, entity))
                    }
                    is EntityXPOrb -> {
                        list.add(XpOrbRenderInfo(this, entity))
                    }
                    is EntityItem -> {
                        for (itemGroup in itemList) {
                            if (itemGroup.add(entity)) continue@loop // If we add the item to any of the group successfully then we continue
                        }
                        itemList.add(ItemGroup().apply { add(entity) })
                    }
                }
            }

            for (itemGroup in itemList) {
                for (otherGroup in itemList) {
                    if (itemGroup === otherGroup) continue
                    itemGroup.merge(otherGroup)
                }
            }

            itemList.asSequence()
                .filterNot { it.isEmpty() }
                .onEach { it.updateText() }
                .mapTo(list) {
                    ItemGroupRenderInfo(this, it)
                }

            renderInfos = list
        }
    }

    private fun SafeClientEvent.getContent(contentType: ContentType, entity: Entity) = when (contentType) {
        ContentType.NONE -> {
            null
        }
        ContentType.NAME -> {
            val name = entity.displayName.unformatted
            val color = if (FriendManager.isFriend(name)) ColorRGB(32, 255, 32, aText.value) else getTextColor()
            TextComponent.TextElement(name, color)
        }
        ContentType.TYPE -> {
            TextComponent.TextElement(getEntityType(entity), getTextColor())
        }
        ContentType.TOTAL_HP -> {
            if (entity !is EntityLivingBase) {
                null
            } else {
                val totalHp = MathUtils.round(entity.health + entity.absorptionAmount, 1).toString()
                TextComponent.TextElement(totalHp, getHpColor(entity))
            }
        }
        ContentType.HP -> {
            if (entity !is EntityLivingBase) {
                null
            } else {
                val hp = MathUtils.round(entity.health, 1).toString()
                TextComponent.TextElement(hp, getHpColor(entity))
            }
        }
        ContentType.ABSORPTION -> {
            if (entity !is EntityLivingBase || entity.absorptionAmount == 0f) {
                null
            } else {
                val absorption = MathUtils.round(entity.absorptionAmount, 1).toString()
                TextComponent.TextElement(absorption, ColorRGB(234, 204, 32, aText.value))
            }
        }
        ContentType.PING -> {
            if (entity !is EntityOtherPlayerMP) {
                null
            } else {
                @Suppress("UNNECESSARY_SAFE_CALL")
                val ping = connection.getPlayerInfo(entity.uniqueID)?.responseTime ?: 0
                TextComponent.TextElement("${ping}ms", pingColorGradient.get(ping.toFloat()).alpha(aText.value))
            }
        }
        ContentType.DISTANCE -> {
            val dist = MathUtils.round(player.distanceTo(entity), 1).toString()
            TextComponent.TextElement("${dist}m", getTextColor())
        }
        ContentType.TOTEM_POPS -> {
            TextComponent.TextElement(TotemPopManager.getPopCount(entity).toString(), getTextColor())
        }
    }

    private fun getTextColor() = ColorRGB(rText.value, gText.value, bText.value, aText.value)

    private fun getEntityType(entity: Entity) = entity.javaClass.simpleName.remove("Entity", "MP", "Other", "SP")
        .remove(' ')

    private fun getHpColor(entity: EntityLivingBase) =
        healthColorGradient.get((entity.health / entity.maxHealth) * 100f).alpha(aText.value)

    fun checkEntityType(entity: Entity): Boolean {
        return (self.value || entity != mc.renderViewEntity)
            && (!entity.isInvisible || invisible.value)
            && (experience.value && entity is EntityXPOrb
            || items.value && entity is EntityItem
            || players.value && entity is EntityPlayer && EntityUtils.playerTypeCheck(
            entity,
            friend = true,
            sleeping = true
        )
            || EntityUtils.mobTypeSettings(entity, mobs.value, passive.value, neutral.value, hostile.value))
    }

    private class ItemGroup {
        private val items = ArrayList<EntityItem>()
        val textComponent = TextComponent()

        fun merge(other: ItemGroup) {
            val thisCenter = this.getCenter()
            val otherCenter = other.getCenter()
            val dist = thisCenter.distanceSqTo(otherCenter)

            if (dist < 20.0f) {
                val iterator = other.items.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if ((items.size >= other.items.size
                            || item.distanceSqTo(thisCenter) < item.distanceSqTo(otherCenter))
                        && add(item)
                    ) {
                        iterator.remove()
                    }
                }
            }
        }

        fun add(item: EntityItem): Boolean {
            for (otherItem in items) {
                if (otherItem.distanceSqTo(item) > 10.0f) return false
            }
            return items.add(item)
        }

        fun isEmpty() = items.isEmpty()

        fun getCenter(): Vec3d {
            if (isEmpty()) return Vec3d.ZERO

            var x = 0.0
            var y = 0.0
            var z = 0.0
            val sizeFactor = 1.0 / items.size

            for (entityItem in items) {
                x += entityItem.posX * sizeFactor
                y += entityItem.posY * sizeFactor
                z += entityItem.posZ * sizeFactor
            }

            return Vec3d(x, y, z)
        }

        fun getCenterPrev(): Vec3d {
            if (isEmpty()) return Vec3d.ZERO

            var x = 0.0
            var y = 0.0
            var z = 0.0
            val sizeFactor = 1.0 / items.size

            for (entityItem in items) {
                x += entityItem.lastTickPosX * sizeFactor
                y += entityItem.lastTickPosY * sizeFactor
                z += entityItem.lastTickPosZ * sizeFactor
            }

            return Vec3d(x, y, z)
        }

        fun updateText() {
            // Updates item count text
            val itemCountMap = TreeMap<String, Int>(Comparator.naturalOrder())
            for (entityItem in items) {
                val itemStack = entityItem.item
                val originalName = itemStack.originalName
                val displayName = itemStack.displayName
                val finalName = if (displayName == originalName) originalName else "$displayName ($originalName)"
                val count = itemCountMap.getOrDefault(finalName, 0) + itemStack.count
                itemCountMap[finalName] = count
            }
            textComponent.clear()
            for ((index, entry) in itemCountMap.entries.sortedByDescending { it.value }.withIndex()) {
                val text = if (dropItemCount.value) "${entry.key} x${entry.value}" else entry.key
                textComponent.addLine(text, getTextColor())
                if (index + 1 >= maxDropItems.value) {
                    val remaining = itemCountMap.size - index - 1
                    if (remaining > 0) textComponent.addLine("...and $remaining more", getTextColor())
                    break
                }
            }
        }
    }

    private class ItemGroupRenderInfo(event: SafeClientEvent, itemGroup: ItemGroup) : IRenderInfo {
        override val distanceSq = event.player.distanceSqTo(itemGroup.getCenter()).toFloat()
        private val textComponent = itemGroup.textComponent
        private val posPrev = itemGroup.getCenterPrev()
        private val pos = itemGroup.getCenter()

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val pos = posPrev.lerp(pos, partialTicks)
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(pos)
            val scale = calcScale(camPos, pos)

            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)

            drawNametag(screenPos, scale, textComponent)

            GlStateManager.popMatrix()
        }
    }

    private class XpOrbRenderInfo(event: SafeClientEvent, private val entity: EntityXPOrb) : IRenderInfo {
        override val distanceSq = event.player.distanceSqTo(entity).toFloat()
        private val textComponent = TextComponent()

        init {
            textComponent.add("${entity.name} x${entity.xpValue}")
        }

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val pos = EntityUtils.getInterpolatedPos(entity, partialTicks).add(0.0, 1.0, 0.0)
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(pos)
            val scale = calcScale(camPos, pos)

            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)

            drawNametag(screenPos, scale, textComponent)

            GlStateManager.popMatrix()
        }
    }

    private class LivingRenderInfo(event: SafeClientEvent, private val entity: EntityLivingBase) : IRenderInfo {
        override val distanceSq = event.player.distanceSqTo(entity).toFloat()
        private val textComponent = TextComponent()
        private val itemList = ArrayList<Pair<ItemStack, TextComponent>>()

        init {
            event {
                var isLine1Empty = true
                for (contentType in line1Settings) {
                    getContent(contentType.value, entity)?.let {
                        textComponent.add(it)
                        isLine1Empty = false
                    }
                }
                if (!isLine1Empty) textComponent.currentLine++
                for (contentType in line2Settings) {
                    getContent(contentType.value, entity)?.let {
                        textComponent.add(it)
                    }
                }

                getEnumHand(if (invertHand.value) EnumHandSide.RIGHT else EnumHandSide.LEFT)?.let { // Hand
                    val itemStack = entity.getHeldItem(it)
                    itemList.add(itemStack to getEnchantmentText(itemStack))
                }

                if (armor.value) for (armor in entity.armorInventoryList.reversed()) itemList.add(
                    armor to getEnchantmentText(
                        armor
                    )
                ) // Armor

                getEnumHand(if (invertHand.value) EnumHandSide.LEFT else EnumHandSide.RIGHT)?.let { // Hand
                    val itemStack = entity.getHeldItem(it)
                    itemList.add(itemStack to getEnchantmentText(itemStack))
                }
            }
        }

        private val empty = itemList.isEmpty() || itemList.all { it.first.isEmpty }
        private val halfHeight = textComponent.getHeight(2, true) / 2.0f + margins.value + 2.0f
        private val halfWidth = (itemList.count { !it.first.isEmpty } * 28) / 2.0f
        private val drawDura = dura.value && itemList.firstOrNull { it.first.isItemStackDamageable } != null

        override fun render(camPos: Vec3d, partialTicks: Float) {
            val pos = EntityUtils.getInterpolatedPos(entity, partialTicks).add(0.0, entity.height + 0.5, 0.0)
            val screenPos = ProjectionUtils.toAbsoluteScreenPos(pos)
            val scale = calcScale(camPos, pos)

            GlStateManager.pushMatrix()
            GlStateManager.translate(screenPos.x.toFloat(), screenPos.y.toFloat(), 0.0f)
            GlStateManager.scale(scale, scale, 1.0f)

            if (drawNametagLiving(screenPos, scale, entity, textComponent) && !empty) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(0.0f, -halfHeight, 0.0f) // Translate to top of nametag
                GlStateManager.scale(itemScale.value, itemScale.value, 1f) // Scale to item scale
                GlStateManager.translate(0.0f, -margins.value - 2.0f, 0.0f)

                GlStateManager.translate(-halfWidth + 4f, -16f, 0.0f)
                if (drawDura) {
                    GlStateManager.translate(0.0f, -MainFontRenderer.getHeight() - 2.0f, 0.0f)
                }

                for ((itemStack, enchantmentText) in itemList) {
                    if (itemStack.isEmpty) continue
                    drawItem(itemStack, enchantmentText, drawDura)
                }

                GlStateManager.popMatrix()
            }

            GlStateManager.popMatrix()
        }
    }

    private interface IRenderInfo : Comparable<IRenderInfo> {
        val distanceSq: Float

        fun render(camPos: Vec3d, partialTicks: Float)

        override fun compareTo(other: IRenderInfo): Int {
            return this.distanceSq.compareTo(other.distanceSq)
        }
    }

    private fun calcScale(camPos: Vec3d, pos: Vec3d): Float {
        val dist = max(camPos.distanceTo(pos).toFloat() - 1.5f, 0.0f) * 0.25f * distScaleFactor.value
        val distFactor = if (distScaleFactor.value == 0f) 1.0f else max(1.0f / 2.0f.pow(dist), minDistScale.value)
        return scale.value * 2.0f * distFactor
    }

    private fun drawItem(itemStack: ItemStack, enchantmentText: TextComponent, drawDura: Boolean) {
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        mc.renderItem.zLevel = -100.0f
        RenderHelper.enableGUIStandardItemLighting()
        glUseProgram(0)
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
        RenderHelper.disableStandardItemLighting()
        mc.renderItem.zLevel = 0.0f
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        if (drawDura && itemStack.isItemStackDamageable) {
            val duraPercentage = 100f - (itemStack.itemDamage.toFloat() / itemStack.maxDamage.toFloat()) * 100f
            val color = healthColorGradient.get(duraPercentage)
            val text = duraPercentage.roundToInt().toString()
            val textWidth = MainFontRenderer.getWidth(text)
            MainFontRenderer.drawString(text, 8.0f - textWidth / 2.0f, 17.0f, color = color)
        }

        if (count.value && itemStack.count > 1) {
            val itemCount = itemStack.count.toString()
            GlStateManager.translate(0.0f, 0.0f, 60.0f)
            val stringWidth = 17.0f - MainFontRenderer.getWidth(itemCount)
            MainFontRenderer.drawString(itemCount, stringWidth, 9.0f)
            GlStateManager.translate(0.0f, 0.0f, -60.0f)
        }

        GlStateManager.translate(0.0f, -2.0f, 0.0f)
        if (enchantment.value) {
            val scale = 0.6f
            enchantmentText.draw(
                lineSpace = 2,
                scale = scale,
                verticalAlign = dev.luna5ama.trollhack.graphics.VAlign.BOTTOM
            )
        }

        GlStateManager.translate(28.0f, 2.0f, 0.0f)
    }

    private fun drawNametagLiving(
        screenPos: Vec3d,
        scale: Float,
        entity: EntityLivingBase,
        textComponent: TextComponent
    ): Boolean {
        val halfWidth = textComponent.getWidth() / 2.0f + margins.value + 2.0f
        val halfHeight = textComponent.getHeight(2, true) / 2.0f + margins.value + 1.0f

        val scaledHalfWidth = halfWidth * scale
        val scaledHalfHeight = halfHeight * scale

        if ((screenPos.x - scaledHalfWidth).floorToInt() !in 0..mc.displayWidth
            && (screenPos.x + scaledHalfWidth).ceilToInt() !in 0..mc.displayWidth
            || (screenPos.y - scaledHalfHeight).floorToInt() !in 0..mc.displayHeight
            && (screenPos.y + scaledHalfHeight).ceilToInt() !in 0..mc.displayHeight
        ) return false

        val lineColor = getHpColor(entity)
        val lineProgress = entity.health / entity.maxHealth
        val absorption = entity.absorptionAmount / entity.maxHealth

        val width = halfWidth + halfWidth

        RenderUtils2D.prepareGL()

        if (absorption > 0.0) {
            val absorptionColor = ColorRGB(234, 204, 32)

            RenderUtils2D.putVertex(-halfWidth, -halfHeight, GuiSetting.backGround)
            RenderUtils2D.putVertex(-halfWidth, halfHeight + 3.0f, GuiSetting.backGround)
            RenderUtils2D.putVertex(halfWidth, halfHeight + 3.0f, GuiSetting.backGround)
            RenderUtils2D.putVertex(halfWidth, -halfHeight, GuiSetting.backGround)

            RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, absorptionColor)
            RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, absorptionColor)
            RenderUtils2D.putVertex(-halfWidth + width * absorption, halfHeight + 1.0f, absorptionColor)
            RenderUtils2D.putVertex(-halfWidth + width * absorption, halfHeight - 1.0f, absorptionColor)

            RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth, halfHeight + 3.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 3.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 1.0f, lineColor)
        } else {
            RenderUtils2D.putVertex(-halfWidth, -halfHeight, GuiSetting.backGround)
            RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, GuiSetting.backGround)
            RenderUtils2D.putVertex(halfWidth, halfHeight + 1.0f, GuiSetting.backGround)
            RenderUtils2D.putVertex(halfWidth, -halfHeight, GuiSetting.backGround)

            RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight + 1.0f, lineColor)
            RenderUtils2D.putVertex(-halfWidth + width * lineProgress, halfHeight - 1.0f, lineColor)
        }

        RenderUtils2D.draw(GL_QUADS)
        RenderUtils2D.releaseGL()

        textComponent.draw(
            skipEmptyLine = true,
            horizontalAlign = dev.luna5ama.trollhack.graphics.HAlign.CENTER,
            verticalAlign = dev.luna5ama.trollhack.graphics.VAlign.CENTER
        )

        return true
    }

    private fun drawNametag(screenPos: Vec3d, scale: Float, textComponent: TextComponent) {
        val halfWidth = textComponent.getWidth() / 2.0f + margins.value + 2.0f
        val halfHeight = textComponent.getHeight(2, true) / 2.0f + margins.value + 1.0f

        val scaledHalfWidth = halfWidth * scale
        val scaledHalfHeight = halfHeight * scale

        if ((screenPos.x - scaledHalfWidth).floorToInt() !in 0..mc.displayWidth
            && (screenPos.x + scaledHalfWidth).ceilToInt() !in 0..mc.displayWidth
            || (screenPos.y - scaledHalfHeight).floorToInt() !in 0..mc.displayHeight
            && (screenPos.y + scaledHalfHeight).ceilToInt() !in 0..mc.displayHeight
        ) return

        val width = halfWidth + halfWidth

        RenderUtils2D.prepareGL()

        RenderUtils2D.putVertex(-halfWidth, -halfHeight, GuiSetting.backGround)
        RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, GuiSetting.backGround)
        RenderUtils2D.putVertex(halfWidth, halfHeight + 1.0f, GuiSetting.backGround)
        RenderUtils2D.putVertex(halfWidth, -halfHeight, GuiSetting.backGround)

        RenderUtils2D.putVertex(-halfWidth, halfHeight - 1.0f, GuiSetting.primary)
        RenderUtils2D.putVertex(-halfWidth, halfHeight + 1.0f, GuiSetting.primary)
        RenderUtils2D.putVertex(-halfWidth + width * 1.0f, halfHeight + 1.0f, GuiSetting.primary)
        RenderUtils2D.putVertex(-halfWidth + width * 1.0f, halfHeight - 1.0f, GuiSetting.primary)
        RenderUtils2D.draw(GL_QUADS)

        RenderUtils2D.releaseGL()

        textComponent.draw(
            skipEmptyLine = true,
            horizontalAlign = dev.luna5ama.trollhack.graphics.HAlign.CENTER,
            verticalAlign = dev.luna5ama.trollhack.graphics.VAlign.CENTER
        )
    }

    private fun getEnchantmentText(itemStack: ItemStack): TextComponent {
        val textComponent = TextComponent()
        val enchantmentList = EnchantmentUtils.getAllEnchantments(itemStack)
        for (leveledEnchantment in enchantmentList) {
            textComponent.add(leveledEnchantment.alias, ColorRGB(255, 255, 255, aText.value), Style.BOLD)
            textComponent.addLine(leveledEnchantment.levelText, ColorRGB(150, 240, 250, aText.value), Style.BOLD)
        }
        return textComponent
    }

    private fun getEnumHand(enumHandSide: EnumHandSide) =
        if (mc.gameSettings.mainHand == enumHandSide && mainHand.value) EnumHand.MAIN_HAND
        else if (mc.gameSettings.mainHand != enumHandSide && offhand.value) EnumHand.OFF_HAND
        else null

}