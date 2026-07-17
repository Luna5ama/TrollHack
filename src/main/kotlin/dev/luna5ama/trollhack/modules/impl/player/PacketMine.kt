package dev.luna5ama.trollhack.modules.impl.player

import dev.luna5ama.trollhack.event.api.handler
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.PacketEvent
import dev.luna5ama.trollhack.event.impl.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.impl.player.PlayerClickBlockEvent
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.event.impl.render.Render3DEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import dev.luna5ama.trollhack.gui.TrollClickGui
import dev.luna5ama.trollhack.gui.TrollHudEditor
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.RotationManager
import dev.luna5ama.trollhack.mixins.accessor.IPlayerMoveC2SPacketAccessor
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.MinecraftWrapper.mc
import dev.luna5ama.trollhack.modules.impl.player.PacketMine.SwitchMode.*
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.extension.*
import dev.luna5ama.trollhack.graphics.animations.AnimationFlag
import dev.luna5ama.trollhack.graphics.animations.Easing
import dev.luna5ama.trollhack.graphics.blaze3d.Render3DScheduler
import dev.luna5ama.trollhack.graphics.blaze3d.WorldProjection
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.inventory.*
import dev.luna5ama.trollhack.utils.inventory.pause.MainHandPause
import dev.luna5ama.trollhack.utils.inventory.pause.withPause
import dev.luna5ama.trollhack.utils.math.RotationUtils
import dev.luna5ama.trollhack.utils.math.fastCeil
import dev.luna5ama.trollhack.utils.math.scale
import dev.luna5ama.trollhack.utils.math.sq
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.toVec3Center
import dev.luna5ama.trollhack.utils.rotation.Priority
import dev.luna5ama.trollhack.utils.timing.TickTimer
import dev.luna5ama.trollhack.utils.world.BlockUtils
import dev.luna5ama.trollhack.utils.world.BlockUtils.getPlaceSide
import dev.luna5ama.trollhack.utils.world.BlockUtils.place
import dev.luna5ama.trollhack.utils.world.CrystalUtils
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.pow

object PacketMine : Module("Packet Mine", category = Category.PLAYER) {
    private val usingPause by setting("Pause On Use", true)
    private val onlyMain by setting("Only Main", true, { usingPause })
    private val delay by setting("Delay", 50, 0..500, 25)
    private val damage by setting("Damage", 1.0f, 0.0f..2.0f, 0.01f)
    private val range by setting("Range", 6f, 2f..10f, 0.1f)
    private val maxBreak by setting("Max Break", 3, 0..20, 1)
    private val instant by setting("Instant", true)
    private val cancelPacket by setting("Cancel Packet", false)
    private val wait by setting("Wait", true, { !instant })
    private val mineAir by setting("Mine Air", true, { wait })
    private val distanceCancel by setting("Distance Cancel", false)
    private val switchMode by setting("Switch Mode", INVENTORY)
    private val checkGround by setting("Check Ground", true)
    private val groundOnly by setting("Ground Only", true)
    private val doubleBreak by setting("Double Break", true)
    private val fastBypass by setting("Fast Bypass", true)
    private val clientRemove by setting("Client Remove", true)
    private val switchDamage by setting("Switch Damage", 95, 0..100, 1)
    private val switchTime by setting("Switch Time", 100, 0..1000, 10)
    private val mineDelay by setting("Mine Delay", 300, 0..1000, 10)
    private val packetDelay by setting("Packet Delay", 200, 0..1000, 10)
    private val swing by setting("Swing", true)
    private val endSwing by setting("End Swing", true)
    private val bypassGround by setting("Bypass Ground", true)
    private val bypassTime by setting("Bypass Time", 400, 0..2000, 20, { bypassGround })
    private val rotate by setting("Rotate", true)
    private val endRotate by setting("End Rotate", true)
    private val switchReset by setting("Switch Reset", false)
    private val crystal by setting("Crystal", false)
    private val onlyHeadBomber by setting("Only Cev", true, { crystal })
    private val awaitPlace by setting("Await Place", true, { crystal })
    private val afterBreak by setting("After Break", true, { crystal })
    private val checkDamage by setting("Check Progress", true, { crystal })
    private val crystalDamage by setting("Progress", 0.9f, 0.0f..1.0f, 0.01f, { crystal && checkDamage })
    private val placeDelay by setting("Place Delay", 100, 0..1000, 50, { crystal })
    private val startColor by setting("Start Color", ColorRGBA.WHITE.alpha(100))
    private val endColor by setting("End Color", ColorRGBA.WHITE.alpha(100))
    private val doubleColor by setting("Double Color", ColorRGBA(88, 94, 255, 100), { doubleBreak })
    private val text by setting("Text", true)
    private val box by setting("Box", true)
    private val outline by setting("Outline", true)
    private val renderMode by setting("Render Mode", RenderMode.SHRINK)
    private val fading by setting("Fading", true)
    private val renderTime by setting("Render Time", 100, 0..5000, 50, { fading })
    private val fadeTime by setting("Fade Time", 200, 0..5000, 50, { fading })

    val godBlocks: List<Block> = listOf(
        Blocks.COMMAND_BLOCK,
        Blocks.LAVA_CAULDRON,
        Blocks.LAVA,
        Blocks.WATER_CAULDRON,
        Blocks.WATER,
        Blocks.BEDROCK,
        Blocks.BARRIER,
        Blocks.END_PORTAL,
        Blocks.NETHER_PORTAL,
        Blocks.END_PORTAL_FRAME
    )

    var lastSlot = -1
    var breakPos: BlockPos? = null
    var secondPos: BlockPos? = null
    var progress = 0.0
    private val firstTimer = TickTimer()
    private val secondTimer = TickTimer()
    var breakLength1 = 1000f
    var breakLength2 = 1000f
    private val firstAnim = AnimationFlag(Easing.OUT_CUBIC) { breakLength1 }
    private val secondAnim = AnimationFlag(Easing.OUT_CUBIC) { breakLength2 }
    private var breakNumber = 0
    private var startMine = false
    private val delayTimer = TickTimer()
    private val mineTimer = TickTimer()
    private val packetTimer = TickTimer()
    private val switchTimer = TickTimer()
    private val placeTimer = TickTimer()
    private var sendGroundPacket = false
    private var delayedPacketPos: BlockPos? = null
    private var switchPending = false
    private var restoreSlot = -1
    private var restoreClientSlot = false
    private var mainAirSince = -1L
    private val df = DecimalFormat("0.0")

    init {
        handler<WorldEvent.Load> {
            startMine = false
            breakPos = null
            secondPos = null
            delayedPacketPos = null
            switchPending = false
            restoreSlot = -1
            restoreClientSlot = false
            mainAirSince = -1L
        }

        nonNullHandler<Skia2DEvent> { event ->
            if (!text) return@nonNullHandler
            breakPos?.let { pos ->
                val slot = getTool(pos) ?: player.hotbarSlots[HotbarSwitchManager.serverSideHotbar]
                val breakTime = getBreakTime(pos, slot)
                val text = if (isAir(pos)) "Waiting..."
                else if (firstTimer.passed < breakTime) df.format(firstTimer.passed / breakTime * 100) + "%"
                else "100.0%"
                val camera = mc.entityRenderDispatcher.camera ?: return@let
                if (RotationUtils.getRotationDiff(
                        Vec2f(camera.yRot(), camera.xRot()),
                        RotationUtils.getRotationTo(pos.toVec3Center())) < mc.options.fov().get()) {
                    val screenPos = WorldProjection.worldToScreen(
                        pos.toVec3Center(),
                        event.framebufferWidth,
                        event.framebufferHeight,
                        event.width,
                        event.height
                    ) ?: return@let
                    val alpha = (255.0f * firstAnim.getAndUpdate(100f) / 100).toInt()
                    val color = if (firstAnim.getAndUpdate(100f) / 100 >= 1.0f)
                        ColorRGBA(255, 255, 255)
                    else ColorRGBA(255, 255, 255, alpha)

                    event.draw.centeredText(
                        text,
                        screenPos.x,
                        screenPos.y - 9f,
                        size = 18f,
                        color = color,
                        shadow = true
                    )
                }
            }
        }

        nonNullHandler<Render3DEvent> {
            if (!player.isCreative) {
                secondPos?.let { pos ->
                    if (isAir(pos)) {
                        secondPos = null
                    } else {
                        val size = secondAnim.getAndUpdate(100f)
                        val renderScale = renderScale(size)
                        val renderBox = AABB(pos).scale(renderScale)
                        if (box) {
                            Render3DScheduler.addFilledBox(renderBox, doubleColor, through = true)
                        }
                        if (outline) {
                            Render3DScheduler.addOutlineBox(
                                renderBox,
                                doubleColor.alpha(255),
                                thickness = 1.0f,
                                through = true
                            )
                        }
                    }
                }
                breakPos?.let { pos ->
                    val slot = getTool(pos) ?: player.hotbarSlots[HotbarSwitchManager.serverSideHotbar]
                    val breakTime = getBreakTime(pos, slot)
                    progress = (firstTimer.passed / breakTime).toInt().toDouble()
                    breakLength1 = getBreakTime(pos, slot).toFloat()
                    val size = firstAnim.getAndUpdate(100f)
                    val progressFactor = (size / 100f).coerceIn(0f, 1f)
                    val color = interpolateColor(progressFactor)
                    val fadeFactor = if (fading && firstTimer.passed > breakTime + renderTime) {
                        1f - ((firstTimer.passed - breakTime - renderTime).toFloat() / fadeTime.coerceAtLeast(1)).coerceIn(0f, 1f)
                    } else 1f
                    val renderBox = AABB(pos).scale(renderScale(size))
                    if (box) {
                        Render3DScheduler.addFilledBox(renderBox, color.alpha((color.a * fadeFactor).toInt()), through = true)
                    }
                    if (outline) {
                        Render3DScheduler.addOutlineBox(
                            renderBox,
                            color.alpha((255 * fadeFactor).toInt()),
                            thickness = 1.0f,
                            through = true
                        )
                    }
                } ?: run {
                    progress = 0.0
                }
            } else {
                progress = 0.0
            }
        }

        nonNullHandler<PlayerClickBlockEvent> { event ->
            if (!player.isCreative) {
                event.cancel()
                if (!mineTimer.tickAndReset(mineDelay)) return@nonNullHandler
                mine(event.pos)
            }
        }
        
        nonNullHandler<PacketEvent.Send> { event ->
            if (event.packet is ServerboundMovePlayerPacket) {
                if (bypassGround && breakPos != null && !isAir(breakPos!!) && bypassTime > 0
                    && (breakPos!!.center.distanceToSqr(player.eyePosition).toFloat() <= (range + 2).sq)) {
                    val slot = getTool(breakPos!!) ?: player.hotbarSlots[HotbarSwitchManager.serverSideHotbar]
                    val breakTime: Double = (getBreakTime(breakPos!!, slot) - bypassTime)
                    if (breakTime <= 0 || firstTimer.tick(breakTime.toLong())) {
                        sendGroundPacket = true
                        (event.packet as IPlayerMoveC2SPacketAccessor).setOnGround(true)
                    }
                } else {
                    sendGroundPacket = false
                }
            } else if (event.packet is ServerboundSetCarriedItemPacket) {
                if (event.packet.slot != lastSlot) {
                    lastSlot = event.packet.slot
                    if (switchReset) {
                        startMine = false
                        firstTimer.reset()
                        firstAnim.forceUpdate(0f, 0f)
                    }
                }
            } else if (event.packet !is ServerboundPlayerActionPacket) {
                return@nonNullHandler
            } else if (event.packet.action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                if (breakPos == null || event.packet.pos != breakPos) {
                    if (cancelPacket) event.cancel()
                    return@nonNullHandler
                }
                startMine = true
            } else if (event.packet.action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                if (breakPos == null || event.packet.pos != breakPos) {
                    if (cancelPacket) event.cancel()
                    return@nonNullHandler
                }
                if (!instant) {
                    startMine = false
                }
            }
        }

        nonNullHandler<OnUpdateWalkingPlayerEvent.Pre> {
            update()
        }

        onDisabled {
            restoreSwitchedSlot()
            startMine = false
            breakPos = null
            secondPos = null
            delayedPacketPos = null
        }
    }

    context(ctx: NonNullContext)
    fun mine(pos: BlockPos): Unit = ctx.run {
        if (player.isCreative) {
//            interaction.attackBlock(pos, BlockUtils.getClickSide(pos))
            return
        }
        if (godBlocks.contains(world.getBlockState(pos).block)) {
            return
        }
        if (pos == breakPos) {
            return
        }
        if (BlockUtils.getClickSideStrict(pos) == null) {
            return
        }
        if (player.eyePosition.distanceToSqr(pos.center) > range.sq) {
            return
        }
        breakPos = pos
        breakNumber = 0
        startMine = false
        startMine()
    }

    context(ctx: NonNullContext)
    private fun update(): Unit = ctx.run {
        if (switchPending && switchTimer.tick(switchTime)) {
            restoreSwitchedSlot()
        }
        delayedPacketPos?.let { delayedPos ->
            if (packetTimer.tick(packetDelay)) {
                netHandler.send(
                    ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        delayedPos,
                        BlockUtils.getClickSide(delayedPos) ?: Direction.DOWN
                    )
                )
                delayedPacketPos = null
            }
        }
        if (player.isDeadOrDying) {
            secondPos = null
        }
        secondPos?.let { pos ->
            if (secondTimer.tick(getBreakTime(pos, player.hotbarSlots[HotbarSwitchManager.serverSideHotbar], 1.1).toInt())) {
                secondPos = null
            }
        }
        secondPos?.let { if (isAir(it)) secondPos = null }
        if (player.isCreative) {
            startMine = false
            breakNumber = 0
            breakPos = null
            return
        }
        val pos = breakPos ?: run {
            breakNumber = 0
            startMine = false
            return
        }
        if (isAir(pos)) {
            breakNumber = 0
        }
        if (breakNumber > maxBreak - 1 && maxBreak > 0 || !wait && isAir(pos) && !instant) {
            if (pos == secondPos) {
                secondPos = null
            }
            startMine = false
            breakNumber = 0
            breakPos = null
            return
        }
        if (godBlocks.contains(world.getBlockState(pos).block)) {
            breakPos = null
            startMine = false
            return
        }
        if (usingPause && player.isUsingItem && (!onlyMain || player.usingItemHand == InteractionHand.MAIN_HAND)) {
            return
        }
        if (player.eyePosition.distanceToSqr(pos.center) > range.sq) {
            if (distanceCancel) {
                startMine = false
                breakNumber = 0
                breakPos = null
            }
            return
        }
        if (switchMode == NONE && mc.screen != null && mc.screen !is ChatScreen
            && mc.screen !is InventoryScreen && mc.screen !== TrollClickGui && mc.screen !== TrollHudEditor) {
            return
        }
        val slot = getTool(pos) ?: player.hotbarSlots[HotbarSwitchManager.serverSideHotbar]
        val breakProgress = (firstTimer.passed / getBreakTime(pos, slot)).coerceIn(0.0, 1.0)
        if (!switchPending && breakProgress * 100.0 >= switchDamage) {
            beginTimedSwitch(slot)
        }

        if (isAir(pos)) {
            if (shouldCrystal(pos)) {
                for (facing in Direction.entries.toTypedArray()) {
                    attackCrystal(pos.relative(facing), rotate, true)
                }
            }

            breakNumber = 0
        } else if (CrystalUtils.canPlaceCrystal(pos.above())) {
            if (shouldCrystal(pos)) {
                if (placeTimer.tick(placeDelay)) {
                    if (checkDamage) {
                        if (firstTimer.passed / getBreakTime(pos, slot) >= crystalDamage) {
                            if (!placeCrystal()) return
                        }
                    } else {
                        if (!placeCrystal()) return
                    }
                } else if (startMine) {
                    return
                }
            }
        }
        
        if (!delayTimer.tick(delay)) return
        if (startMine) {
            if (isAir(pos)) {
                return
            }
            if (groundOnly && !player.onGround()) return
            if (firstTimer.tick(getBreakTime(pos, slot).toLong())) {

                if (endRotate) {
                    RotationManager.setRotations(
                        RotationUtils.getRotationTo(pos.toVec3Center()),
                        priority = Priority.High
                    )
                }

                if (endSwing) player.swing(InteractionHand.MAIN_HAND)
                if (switchMode != NONE) {
                    swap(slot) {
                        netHandler.send(
                            ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                pos,
                                BlockUtils.getClickSide(pos) ?: return@swap
                            )
                        )
                    }
                } else netHandler.send(
                    ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        pos,
                        BlockUtils.getClickSide(pos) ?: return
                    )
                )
                if (clientRemove && !isAir(pos)) {
                    interaction.destroyBlock(pos)
                }
                breakNumber++
                delayTimer.reset()
                if (afterBreak && shouldCrystal(pos)) {
                    for (facing in Direction.entries) {
                        attackCrystal(pos.relative(facing), rotate, true)
                    }
                }
            }
        } else {
            if (!mineAir && isAir(pos)) {
                return
            }
            startMine()
        }
    }

    context(ctx: NonNullContext)
    private fun startMine(): Unit = ctx.run {
        val pos = breakPos ?: return
        firstTimer.reset()
        firstAnim.forceUpdate(0f, 0f)
        if (rotate) {
            val clickSide = BlockUtils.getClickSide(pos) ?: return
            val vec3i = clickSide.unitVec3
            RotationManager.setRotations(
                RotationUtils.getRotationTo(
                    pos.center.add(
                        Vec3(vec3i.x * 0.5,
                            vec3i.y * 0.5,
                            vec3i.z * 0.5
                        )
                    )
                ),
                priority = Priority.High
            )
        }
        netHandler.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                BlockUtils.getClickSide(pos) ?: return
            )
        )
        if (fastBypass) {
            netHandler.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    BlockPos.containing(player.x, 321.0, player.z),
                    Direction.DOWN
                )
            )
        }
        if (doubleBreak) {
            if (secondPos == null || isAir(secondPos!!)) {
                val slot = getTool(pos) ?: player.hotbarSlots[HotbarSwitchManager.serverSideHotbar]
                val breakTime = (getBreakTime(pos, slot, 1.0))
                secondAnim.forceUpdate(0f, 0f)
                breakLength2 = breakTime.toFloat()
                secondTimer.reset()
                secondPos = pos
            }
            delayedPacketPos = pos
            packetTimer.reset()
        }
        if (swing) {
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    context(ctx: NonNullContext)
    private fun placeCrystal(): Boolean = ctx.run {
        findCrystal()?.let {
            swap(it) {
                place(breakPos!!.above(), rotate = rotate)
                placeTimer.reset()
            }
            return !awaitPlace
        }
        return true
    }

    context(ctx: NonNullContext)
    private fun swap(slot: Slot, func: () -> Unit): Unit = ctx.run {
        when (switchMode) {
            NONE -> return
            LEGIT -> MainHandPause.withPause(PacketMine) {
                swapToSlot(slot as HotbarSlot)
                func()
            }
            GHOST, INVENTORY -> {
                HotbarSwitchManager.ghostSwitch(slot, func)
            }
        }
    }

    context(ctx: NonNullContext)
    private fun beginTimedSwitch(slot: Slot): Unit = ctx.run {
        val hotbarSlot = slot as? HotbarSlot ?: return
        val target = hotbarSlot.hotbarSlot
        when (switchMode) {
            NONE, INVENTORY -> return
            LEGIT -> {
                restoreSlot = player.inventory.selectedSlot
                restoreClientSlot = true
                swapToSlot(hotbarSlot)
            }
            GHOST -> {
                restoreSlot = HotbarSwitchManager.serverSideHotbar
                restoreClientSlot = false
                netHandler.send(ServerboundSetCarriedItemPacket(target))
            }
        }
        if (restoreSlot == target) {
            restoreSlot = -1
            restoreClientSlot = false
            return
        }
        switchPending = true
        switchTimer.reset()
    }

    private fun restoreSwitchedSlot() {
        val slot = restoreSlot
        if (!switchPending || slot !in 0..8) {
            switchPending = false
            restoreSlot = -1
            restoreClientSlot = false
            return
        }
        val player = mc.player
        if (restoreClientSlot && player != null) {
            player.inventory.selectedSlot = slot
        }
        mc.connection?.send(ServerboundSetCarriedItemPacket(slot))
        switchPending = false
        restoreSlot = -1
        restoreClientSlot = false
    }

    context(ctx: NonNullContext)
    private fun canPlace(pos: BlockPos) = ctx.run {
        getPlaceSide(pos, false, false) != null && isReplaceable(pos)
    }

    context(ctx: NonNullContext)
    private fun isReplaceable(pos: BlockPos): Boolean = ctx.run {
        return world.getBlockState(pos).canBeReplaced()
    }

    context(ctx: NonNullContext)
    private fun attackCrystal(pos: BlockPos, rotate: Boolean, eatingPause: Boolean): Unit = ctx.run {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, AABB(pos))) {
            attackCrystal(entity, rotate, eatingPause)
            break
        }
    }

    context(ctx: NonNullContext)
    private fun attackCrystal(crystal: Entity, rotate: Boolean, usingPause: Boolean): Unit = ctx.run {
        if (usingPause && player.usingItemHand != null) return
        netHandler.send(ServerboundAttackPacket(crystal.id))
        player.resetAttackStrengthTicker()
    }

    private fun BlockPos.isAbove(entity: Entity): Boolean {
        return x == entity.x.toInt() && y >= entity.y.toInt() + 1 && z == entity.z.toInt()
    }

    private fun shouldCrystal(pos: BlockPos): Boolean {
        return crystal && (!onlyHeadBomber || CombatManager.target?.let { pos.isAbove(it) } == true)
    }

    context(ctx: NonNullContext)
    private fun getTool(pos: BlockPos): Slot? = ctx.run {
        val result = when (switchMode) {
            LEGIT, GHOST -> {
                player.hotbarSlots.maxByOrNull { slot ->
                    val it = slot.item
                    val digSpeed = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY.entry, it).toFloat()
                    val destroySpeed = it.getDestroySpeed(world.getBlockState(pos))
                    digSpeed + destroySpeed
                }
            }
            INVENTORY -> {
                player.everySlots.maxByOrNull { slot ->
                    val it = slot.item
                    val digSpeed = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY.entry, it).toFloat()
                    val destroySpeed = it.getDestroySpeed(world.getBlockState(pos))
                    digSpeed + destroySpeed
                }
            }
            else -> null
        }
        return result
    }

    override fun getDisplayInfo(): Any {
        if (progress >= 1) {
            return "Done"
        }
        return df.format(progress * 100) + "%"
    }

    context(ctx: NonNullContext)
    private fun findCrystal(): Slot? = ctx.run {
        if (switchMode == LEGIT || switchMode == GHOST) {
            return player.hotbarSlots.firstItem(Items.END_CRYSTAL)
        } else if (switchMode == INVENTORY) {
            return player.everySlots.firstItem(Items.END_CRYSTAL)
        }
        return null
    }

    context(ctx: NonNullContext)
    private fun findBlock(block: Block): Slot? = ctx.run {
        if (switchMode == LEGIT || switchMode == GHOST) {
            return player.hotbarSlots.firstBlock(block)
        } else if (switchMode == INVENTORY) {
            return player.everySlots.firstBlock(block)
        }
        return null
    }

    private fun interpolateColor(quad: Float): ColorRGBA {
        val sR: Int = startColor.r
        val sG: Int = startColor.g
        val sB: Int = startColor.b
        val sA: Int = startColor.a

        val eR: Int = endColor.r
        val eG: Int = endColor.g
        val eB: Int = endColor.b
        val eA: Int = endColor.a
        return ColorRGBA(
            (sR + (eR - sR) * quad).toInt(),
            (sG + (eG - sG) * quad).toInt(),
            (sB + (eB - sB) * quad).toInt(),
            (sA + (eA - sA) * quad).toInt()
        )
    }

    context(ctx: NonNullContext)
    fun getBreakTime(pos: BlockPos, slot: Slot): Double = ctx.run {
        return 50.0 * ((pos.state.getDestroySpeed(world, pos) * 30
                / getDigSpeed(pos.state, slot.item)).fastCeil() + 1) / damage
    }

    context(ctx: NonNullContext)
    fun getBreakTime(pos: BlockPos, slot: Slot, damage: Double): Double = ctx.run {
        return 50.0 * ((pos.state.getDestroySpeed(world, pos) * 30
                / getDigSpeed(pos.state, slot.item)).fastCeil() + 1) / damage
    }

    context(ctx: NonNullContext)
    private fun canBreak(pos: BlockPos): Boolean = ctx.run {
        val blockState: BlockState = world.getBlockState(pos)
        val block = blockState.block
        return block.defaultDestroyTime() != -1f
    }

    context(ctx: NonNullContext)
    private fun getBlockStrength(position: BlockPos, itemStack: ItemStack): Float = ctx.run {
        val state: BlockState = world.getBlockState(position)
        val hardness = state.getDestroySpeed(world, position)
        if (hardness < 0) {
            return 0f
        }
        return if (!canBreak(position)) {
            getDigSpeed(state, itemStack) / hardness / 100f
        } else {
            getDigSpeed(state, itemStack) / hardness / 30f
        }
    }

    context(ctx: NonNullContext)
    private fun getDigSpeed(state: BlockState, itemStack: ItemStack): Float = ctx.run {
        var digSpeed = getDestroySpeed(state, itemStack)

        if (digSpeed > 1) {
            val efficiencyModifier = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.EFFICIENCY.entry, itemStack
            )
            if (efficiencyModifier > 0 && !itemStack.isEmpty) {
                digSpeed += (efficiencyModifier.toDouble().pow(2.0) + 1).toFloat()
            }
        }
        if (player.hasEffect(MobEffects.HASTE))
            digSpeed *= 1 + (player.getEffect(MobEffects.HASTE)!!.amplifier + 1) * 0.2f

        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            val miningFatigue: Float = when (player.getEffect(MobEffects.MINING_FATIGUE)!!.amplifier) {
                0 -> 0.3f
                1 -> 0.09f
                2 -> 0.0027f
                3 -> 8.1E-4f
                else -> 8.1E-4f
            }
            digSpeed *= miningFatigue
        }
        if (player.isUnderWater)
            digSpeed *= player.getAttribute(Attributes.SUBMERGED_MINING_SPEED)!!.value.toFloat()

        if (!player.onGround() && checkGround) digSpeed /= 5f

        return if (digSpeed < 0) 0f else digSpeed
    }

    fun getDestroySpeed(state: BlockState, stack: ItemStack, ): Float {
        val str = stack.getDestroySpeed(state).toDouble()
        val effect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY.entry, stack)
        return max(str + if (str > 1.0) effect * effect + 1.0 else 0.0, 0.0).toFloat()
    }

    context(ctx: NonNullContext)
    private fun isAir(breakPos: BlockPos): Boolean = ctx.run {
        return world.isEmptyBlock(breakPos) || breakPos.block == Blocks.FIRE && hasCrystal(breakPos)
    }

    context(ctx: NonNullContext)
    private fun hasCrystal(pos: BlockPos): Boolean = ctx.run {
        for (entity in world.getEntitiesOfClass(EndCrystal::class.java, AABB(pos))) {
            if (!entity.isAlive || entity !is EndCrystal) continue
            return true
        }
        return false
    }

    fun isMine(pos:BlockPos, checkDoubleMine: Boolean = true): Boolean{
        if (!checkDoubleMine){
            return breakPos == pos
        }
        return breakPos == pos || secondPos == pos
    }

    private enum class SwitchMode : Displayable {
        NONE, LEGIT, GHOST, INVENTORY
    }

    private enum class RenderMode(override val displayName: CharSequence) : Displayable {
        BOX("Box"),
        NORMAL("Normal"),
        SHRINK("Shrink"),
        GROW("Grow")
    }

    private fun renderScale(size: Float): Float {
        val progress = (size / 100f).coerceIn(0f, 1f)
        return when (renderMode) {
            RenderMode.BOX, RenderMode.NORMAL -> 1.0f
            RenderMode.SHRINK -> progress
            RenderMode.GROW -> 1.0f + progress
        }
    }
}
