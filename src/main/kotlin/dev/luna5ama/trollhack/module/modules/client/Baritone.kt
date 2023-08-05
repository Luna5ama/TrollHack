package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.baritone.BaritoneSettingsInitEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.graphics.color.ColorRGB

/**
 * Created by Dewy on the 21st of April, 2020
 */
internal object Baritone : Module(
    name = "Baritone",
    category = Category.CLIENT,
    description = "Configures Baritone settings",
    visible = false,
    alwaysEnabled = true
) {
    private val page by setting("Page", Page.BASIC)

    /* Basic */
    private val allowBreak by setting("Allow Break", true, { page == Page.BASIC }, description = "Allow Baritone to break blocks.")
    private val allowPlace by setting("Allow Place", true, { page == Page.BASIC }, description = "Allow Baritone to place blocks.")
    private val allowSprint by setting("Allow Sprint", true, { page == Page.BASIC }, description = "Allow Baritone to sprint.")
    private val allowInventory by setting("Allow Inventory", false, { page == Page.BASIC }, description = "Allow Baritone to move items in your inventory to your hotbar.")
    private val allowDownwardTunneling by setting("Downward Tunneling", true, { page == Page.BASIC }, description = "Allow mining blocks directly beneath you.")
    private val allowParkour by setting("Allow Parkour", true, { page == Page.BASIC })
    private val allowParkourPlace by setting("Allow Parkour Place", true, { allowParkour && page == Page.BASIC })

    /* Visual */
    private val freeLook by setting("Free Look", true, { page == Page.VISUAL }, description = "Move without having to force the client-sided rotations.")
    private val renderGoal by setting("Render Goals", true, { page == Page.VISUAL }, description = "Render the goal.")
    private val censorCoordinates by setting("Censor Coordinates", false, { page == Page.VISUAL }, description = "Censor coordinates in goals and block positions.")
    private val censorRanCommands by setting("Censor RanCommands", false, { page == Page.VISUAL }, description = "Censor arguments to ran commands, to hide, for example, coordinates to #goal.")
    private val showOnRadar by setting("Show Path on Radar", true, { page == Page.VISUAL }, description = "Show the current path on radar.")
    private val color by setting("Path Color", ColorRGB(32, 250, 32), visibility = { showOnRadar && page == Page.VISUAL }, description = "Path color for the radar.")

    /* Fall */
    private val maxFallHeightNoWater by setting("Max Fall Height", 3, 3..5, 1, { page == Page.FALL }, description = "Distance baritone can fall without water.")
    private val allowWaterBucketFall by setting("Water Bucket Clutch", true, { page == Page.FALL }, description = "Uses a water bucket to get down quickly.")
    private val maxFallHeightBucket by setting("Max Bucket Height", 20, 10..250, 10, { allowWaterBucketFall && page == Page.FALL }, description = "Max height that baritone can use a water bucket.")

    /* Build */
    private val buildInLayers by setting("Build In Layers", false, { page == Page.BUILD }, description = "Build/mine one layer at a time in schematics and selections.")
    private val layerOrder by setting("Top To Bottom", false, { buildInLayers && page == Page.BUILD }, description = "Build/mine from top to bottom in schematics and selections.")
    private val startAtLayer by setting("Start At Layer", 0, 0..256, 1, visibility = { buildInLayers && page == Page.BUILD }, description = "Start building the schematic at a specific layer.")
    private val layerHeight by setting("Layer Height", 1, 1..50, 1, visibility = { buildInLayers && page == Page.BUILD }, description = "How high should the individual layers be?")
    private val skipFailedLayers by setting("Skip Failed Layers", false, { buildInLayers && page == Page.BUILD }, description = "If a layer is unable to be constructed, just skip it.")
    private val buildOnlySelection by setting("Build Only Selection", false, { page == Page.BUILD }, description = "Only build the selected part of schematics")
    private val buildIgnoreExisting by setting("Build Ignore Existing", false, { page == Page.BUILD }, description = "If this is true, the builder will treat all non-air blocks as correct. It will only place new blocks.")
    private val buildIgnoreDirection by setting("Build Ignore Direction", false, { page == Page.BUILD }, description = "If this is true, the builder will ignore directionality of certain blocks like glazed terracotta.")
    private val mapArtMode by setting("Map Art Mode", false, { page == Page.BUILD }, description = "Build in map art mode, which makes baritone only care about the top block in each column")
    private val schematicOrientationX by setting("Schematic Orientation X", false, { page == Page.BUILD }, description = "When this setting is true, build a schematic with the highest X coordinate being the origin, instead of the lowest")
    private val schematicOrientationY by setting("Schematic Orientation Y", false, { page == Page.BUILD }, description = "When this setting is true, build a schematic with the highest Y coordinate being the origin, instead of the lowest")
    private val schematicOrientationZ by setting("Schematic Orientation Z", false, { page == Page.BUILD }, description = "When this setting is true, build a schematic with the highest Z coordinate being the origin, instead of the lowest")
    private val okIfWater by setting("Ok If Water", false, { page == Page.BUILD }, description = "Override builder's behavior to not attempt to correct blocks that are currently water")
    private val incorrectSize by setting("Incorrect Size", 100, 1..1000, 1, { page == Page.BUILD }, description = "The set of incorrect blocks can never grow beyond this size")

    /* Advanced */
    private val blockReachDistance by setting("Reach Distance", 4.5f, 1.0f..10.0f, 0.5f, { page == Page.ADVANCED }, description = "Max distance baritone can place blocks.")
    private val enterPortals by setting("Enter Portals", true, { page == Page.ADVANCED }, description = "Baritone will walk all the way into the portal, instead of stopping one block before.")
    private val blockPlacementPenalty by setting("Block Placement Penalty", 20, 0..40, 5, { page == Page.ADVANCED }, description = "Decrease to make Baritone more often consider paths that would require placing blocks.")
    private val jumpPenalty by setting("Jump Penalty", 2, 0..10, 1, { page == Page.ADVANCED }, description = "Additional penalty for hitting the space bar (ascend, pillar, or parkour) because it uses hunger.")
    private val assumeWalkOnWater by setting("Assume Walk On Water", false, { page == Page.ADVANCED }, description = "Allow Baritone to assume it can walk on still water just like any other block. Requires jesus to be enabled.")
    private val failureTimeout by setting("Fail Timeout", 2, 1..20, 1, { page == Page.ADVANCED } )
    private val avoidance by setting("Avoidance", false, { page == Page.ADVANCED }, description = "Enables the 4 avoidance settings. It's disabled by default because of the noticeable performance impact.")
    private val mobAvoidanceRadius by setting("Mob Avoidance Radius", 15, 0..65, 5, visibility = { avoidance && page == Page.ADVANCED }, description = "Distance to avoid mobs.")
    private val mobAvoidanceCoefficient by setting("Mob Avoidance Coefficient", 1.5f, 0f..5f, 0.5f, visibility = { avoidance && page == Page.ADVANCED }, description = "Set to 1.0 to effectively disable this feature. Set below 1.0 to go out of your way to walk near mobs.")
    private val mobSpawnerAvoidanceRadius by setting("Mob Spawner Avoidance Radius", 10, 0..60, 10, visibility = { avoidance && page == Page.ADVANCED }, description = "Distance to avoid mob spawners.")
    private val mobSpawnerAvoidanceCoefficient by setting("Mob Spawner Avoidance Coefficient", 1.5f, 0f..5f, 0.5f, visibility = { avoidance && page == Page.ADVANCED }, description = "Set to 1.0 to effectively disable this feature. Set below 1.0 to go out of your way to walk near mob spawners.")
    private val preferSilkTouch by setting("Prefer Silk Touch", false, { page == Page.ADVANCED }, description = "Always prefer silk touch tools over regular tools.")
    private val backfill by setting("Backfill", false, { page == Page.ADVANCED }, description = "Fill in blocks behind you")
    private val chunkCaching by setting("Chunk Caching", true, { page == Page.ADVANCED }, description = "Download all chunks in simplified 2-bit format and save them for better very-long-distance pathing.")
    private val assumeStep by setting("Assume Step", false, { page == Page.ADVANCED }, description = "Assume step functionality; don't jump on an Ascend.")
    private val assumeExternalAutoTool by setting("Assume External AutoTool", false, { page == Page.ADVANCED }, description = "Disable baritone's auto-tool at runtime, but still assume that another mod will provide auto tool functionality")
    private val autoTool by setting("AutoTool", true, { page == Page.ADVANCED }, description = "Automatically select the best available tool")
    private val assumeSafeWalk by setting("Assume Safe Walk", false, { page == Page.ADVANCED }, description = "Assume safe walk functionality; don't sneak on a backplace traverse.")
    private val allowJumpAt256 by setting("Allow Jump At 256", false, { page == Page.ADVANCED }, description = "If true, parkour is allowed to make jumps when standing on blocks at the maximum height, so player feet is y=256")
    private val allowDiagonalDescend by setting("Allow Diagonal Descend", false, { page == Page.ADVANCED }, description = "Allow descending diagonally. Safer than allowParkour yet still slightly unsafe, can make contact with unchecked adjacent blocks, so it's unsafe in the nether.")
    private val allowDiagonalAscend by setting("Allow Diagonal Ascend", false, { page == Page.ADVANCED }, description = "Allow diagonal ascending. Actually pretty safe, much safer than diagonal descend tbh")

    private enum class Page {
        BASIC, VISUAL, FALL, BUILD, ADVANCED
    }

    init {
        settingList.forEach {
            it.listeners.add { sync() }
        }

        listener<BaritoneSettingsInitEvent> {
            sync()
        }
    }

    private fun sync() {
        BaritoneUtils.settings?.let {
            it.chatControl.value = false // enable chatControlAnyway if you want to use it
            /* Basic */
            it.allowBreak.value = allowBreak
            it.allowSprint.value = allowSprint
            it.allowPlace.value = allowPlace
            it.allowInventory.value = allowInventory
            it.allowDownward.value = allowDownwardTunneling
            it.allowParkour.value = allowParkour
            it.allowParkourPlace.value = allowParkourPlace
            /* Visual */
            it.freeLook.value = freeLook
            it.renderGoal.value = renderGoal
            it.censorCoordinates.value = censorCoordinates
            it.censorRanCommands.value = censorRanCommands
            /* Fall */
            it.maxFallHeightNoWater.value = maxFallHeightNoWater
            it.allowWaterBucketFall.value = allowWaterBucketFall
            it.maxFallHeightBucket.value = maxFallHeightBucket
            /* Build */
            it.buildInLayers.value = buildInLayers
            it.layerOrder.value = layerOrder
            it.layerHeight.value = layerHeight
            it.startAtLayer.value = startAtLayer
            it.skipFailedLayers.value = skipFailedLayers
            it.buildOnlySelection.value = buildOnlySelection
            it.buildIgnoreExisting.value = buildIgnoreExisting
            it.buildIgnoreDirection.value = buildIgnoreDirection
            it.mapArtMode.value = mapArtMode
            it.schematicOrientationX.value = schematicOrientationX
            it.schematicOrientationY.value = schematicOrientationY
            it.schematicOrientationZ.value = schematicOrientationZ
            it.okIfWater.value = okIfWater
            it.incorrectSize.value = incorrectSize
            /* Advanced */
            it.preferSilkTouch.value = preferSilkTouch
            it.backfill.value = backfill
            it.chunkCaching.value = chunkCaching
            it.blockReachDistance.value = blockReachDistance
            it.enterPortal.value = enterPortals
            it.blockPlacementPenalty.value = blockPlacementPenalty.toDouble()
            it.jumpPenalty.value = jumpPenalty.toDouble()
            it.assumeWalkOnWater.value = assumeWalkOnWater
            it.assumeStep.value = assumeStep
            it.assumeExternalAutoTool.value = assumeExternalAutoTool
            it.autoTool.value = autoTool
            it.assumeSafeWalk.value = assumeSafeWalk
            it.allowJumpAt256.value = allowJumpAt256
            it.allowDiagonalDescend.value = allowDiagonalDescend
            it.allowDiagonalAscend.value = allowDiagonalAscend
            it.failureTimeoutMS.value = failureTimeout * 1000L
            it.avoidance.value = avoidance
            it.mobAvoidanceRadius.value = mobAvoidanceRadius
            it.mobAvoidanceCoefficient.value = mobAvoidanceCoefficient.toDouble()
            it.mobSpawnerAvoidanceRadius.value = mobSpawnerAvoidanceRadius
            it.mobSpawnerAvoidanceCoefficient.value = mobSpawnerAvoidanceCoefficient.toDouble()
        }
    }
}
