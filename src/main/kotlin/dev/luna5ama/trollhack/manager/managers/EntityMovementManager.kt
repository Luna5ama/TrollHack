/*
 * Copyright (c) 2021-2022, SagiriXiguajerry. All rights reserved.
 * This repository will be transformed to SuperMic_233.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.LoopEvent
import dev.luna5ama.trollhack.event.impl.world.WorldEvent
import net.minecraft.world.entity.Entity
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.NonNullContext
import dev.luna5ama.trollhack.utils.combat.MotionTracker
import dev.luna5ama.trollhack.utils.timing.TickTimer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap

object EntityMovementManager : AlwaysListening, AbstractManager() {

    var isSafeWalk = false

    private val calculatorMap = ConcurrentHashMap<Player, MotionTracker>()
    private val timer = TickTimer()
    var partialTicks = 0.0f; private set
    init {
        nonNullHandler<LoopEvent.Tick> {
            partialTicks = if (mc.isPaused) mc.deltaTracker.gameTimeDeltaTicks else mc.deltaTracker.getGameTimeDeltaPartialTick(true)
            if (timer.tickAndReset(ClientSettings.predictionUpdateDelay)) update()
        }

        nonNullHandler<WorldEvent.Load> {
            calculatorMap.clear()
        }
    }

    fun NonNullContext.update() {
        EntityManager.entity.filterIsInstance<Player>()
            .filter { calculatorMap[it] == null}.forEach { calculatorMap[it] = MotionTracker(it) }
        calculatorMap.forEach { (_, tracker) ->
            tracker.tick()
        }
    }

    fun withPredictContext(ticksAhead: Int = 2, smooth: Boolean = true): Map<Entity, Vec3> {
        return getPredictedVec3Map(ticksAhead, smooth)
    }

    fun clear() = calculatorMap.clear()

    fun getTrackers(): List<MotionTracker> {
        return calculatorMap.values.toList()
    }

    fun getPredictedVec3Map(ticksAhead: Int = 2, smooth: Boolean = true) =
        getTrackers().associate { it.entity to it.calcPosAhead(ticksAhead, smooth) }

    fun getRelativeVec3Map(ticksAhead: Int = 2, smooth: Boolean = true) =
        getTrackers().associate { it.entity to it.calcRelativePosAhead(ticksAhead, smooth) }

    fun getPredictedBoundingBoxMap(ticksAhead: Int = 2, smooth: Boolean = true) = getTrackers().associate {
        it.entity to it.entity.boundingBox.move(
            it.calcRelativePosAhead(
                ticksAhead,
                smooth
            )
        )
    }
}