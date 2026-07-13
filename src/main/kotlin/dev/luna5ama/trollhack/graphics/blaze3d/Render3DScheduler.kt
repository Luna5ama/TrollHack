package dev.luna5ama.trollhack.graphics.blaze3d

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.systems.RenderSystem
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.world.DirectionMask
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.sqrt

object Render3DScheduler {
    private const val INITIAL_BUFFER_SIZE = 64 * 1024

    private val filledBoxes = ArrayList<FilledBoxCommand>()
    private val outlineBoxes = ArrayList<OutlineBoxCommand>()
    private val lines = ArrayList<LineCommand>()
    private val boxBatches = ArrayList<BoxBatchCommand>()
    private val lineStrips = ArrayList<LineStripCommand>()

    fun isEmpty(): Boolean = filledBoxes.isEmpty() && outlineBoxes.isEmpty() && lines.isEmpty() &&
        boxBatches.isEmpty() && lineStrips.isEmpty()

    fun clear() {
        filledBoxes.clear()
        outlineBoxes.clear()
        lines.clear()
        boxBatches.clear()
        lineStrips.clear()
    }

    fun addFilledBox(
        box: AABB,
        color: ColorRGBA,
        sides: Int = DirectionMask.ALL,
        through: Boolean = true
    ) {
        RenderSystem.assertOnRenderThread()
        if (color.a == 0 || sides == 0) return
        filledBoxes.add(FilledBoxCommand(box, color, sides and DirectionMask.ALL, through))
    }

    fun addOutlineBox(
        box: AABB,
        color: ColorRGBA,
        thickness: Float = 2.0f,
        sides: Int = DirectionMask.ALL,
        through: Boolean = true
    ) {
        RenderSystem.assertOnRenderThread()
        if (color.a == 0 || sides == 0 || thickness <= 0.0f) return
        outlineBoxes.add(OutlineBoxCommand(box, color, thickness, sides and DirectionMask.ALL, through))
    }

    fun addLine(
        from: Vec3,
        to: Vec3,
        color: ColorRGBA,
        thickness: Float = 2.0f,
        through: Boolean = true
    ) {
        RenderSystem.assertOnRenderThread()
        if (color.a == 0 || thickness <= 0.0f || from.distanceToSqr(to) < 1.0E-8) return
        lines.add(LineCommand(from, to, color, thickness, through))
    }

    fun addBoxBatch(
        entries: List<BoxEntry>,
        filledAlpha: Int,
        outlineAlpha: Int,
        tracerAlpha: Int,
        tracerStart: Vec3?,
        thickness: Float,
        through: Boolean = true
    ) {
        RenderSystem.assertOnRenderThread()
        val fill = filledAlpha.coerceIn(0, 255)
        val outline = outlineAlpha.coerceIn(0, 255)
        val tracer = tracerAlpha.coerceIn(0, 255)
        if (entries.isEmpty() || thickness <= 0.0f || fill == 0 && outline == 0 && tracer == 0) return
        boxBatches.add(BoxBatchCommand(entries, fill, outline, tracer, tracerStart, thickness, through))
    }

    fun addLineStrip(
        points: List<Vec3>,
        offset: Vec3,
        color: ColorRGBA,
        thickness: Float = 2.0f,
        through: Boolean = true
    ) {
        RenderSystem.assertOnRenderThread()
        if (points.size < 2 || color.a == 0 || thickness <= 0.0f) return
        lineStrips.add(LineStripCommand(points, offset, color, thickness, through))
    }

    fun flush(modelView: Matrix4f, cameraPosition: Vec3) {
        RenderSystem.assertOnRenderThread()
        if (isEmpty()) return

        try {
            flushFilled(modelView, cameraPosition, through = false)
            flushFilled(modelView, cameraPosition, through = true)
            flushLines(modelView, cameraPosition, through = false)
            flushLines(modelView, cameraPosition, through = true)
        } finally {
            clear()
        }
    }

    private fun flushFilled(modelView: Matrix4f, cameraPosition: Vec3, through: Boolean) {
        if (filledBoxes.none { it.through == through } &&
            boxBatches.none { it.through == through && it.filledAlpha != 0 }
        ) return

        buildAndDraw(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR,
            if (through) Blaze3DRenderPipelines.FILL_THROUGH else Blaze3DRenderPipelines.FILL_DEPTH,
            modelView
        ) { builder ->
            for (command in filledBoxes) {
                if (command.through == through) emitFilledBox(builder, command, cameraPosition)
            }
            for (batch in boxBatches) {
                if (batch.through != through || batch.filledAlpha == 0) continue
                for (entry in batch.entries) {
                    emitFilledBox(
                        builder,
                        entry.box,
                        entry.color.withMultipliedAlpha(batch.filledAlpha),
                        DirectionMask.ALL,
                        cameraPosition
                    )
                }
            }
        }
    }

    private fun flushLines(modelView: Matrix4f, cameraPosition: Vec3, through: Boolean) {
        if (outlineBoxes.none { it.through == through } &&
            lines.none { it.through == through } &&
            lineStrips.none { it.through == through } &&
            boxBatches.none {
                it.through == through && (it.outlineAlpha != 0 || it.tracerAlpha != 0 && it.tracerStart != null)
            }
        ) return

        buildAndDraw(
            VertexFormat.Mode.LINES,
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
            if (through) Blaze3DRenderPipelines.LINE_THROUGH else Blaze3DRenderPipelines.LINE_DEPTH,
            modelView
        ) { builder ->
            for (command in outlineBoxes) {
                if (command.through == through) emitOutlineBox(builder, command, cameraPosition)
            }
            for (command in lines) {
                if (command.through == through) emitLine(builder, command, cameraPosition)
            }
            for (strip in lineStrips) {
                if (strip.through == through) emitLineStrip(builder, strip, cameraPosition)
            }
            for (batch in boxBatches) {
                if (batch.through != through) continue
                for (entry in batch.entries) {
                    if (batch.outlineAlpha != 0) {
                        emitOutlineBox(
                            builder,
                            entry.box,
                            entry.color.withMultipliedAlpha(batch.outlineAlpha),
                            batch.thickness,
                            DirectionMask.ALL,
                            cameraPosition
                        )
                    }
                    if (batch.tracerAlpha != 0 && batch.tracerStart != null) {
                        emitTracer(
                            builder,
                            batch.tracerStart,
                            entry.box,
                            entry.color.withMultipliedAlpha(batch.tracerAlpha),
                            batch.thickness,
                            cameraPosition
                        )
                    }
                }
            }
        }
    }

    private inline fun buildAndDraw(
        mode: VertexFormat.Mode,
        format: VertexFormat,
        pipeline: RenderPipeline,
        modelView: Matrix4f,
        emit: (BufferBuilder) -> Unit
    ) {
        ByteBufferBuilder(INITIAL_BUFFER_SIZE).use { byteBuffer ->
            val builder = BufferBuilder(byteBuffer, mode, format)
            emit(builder)
            val mesh = builder.build() ?: return
            mesh.use { meshData ->
                if (mode == VertexFormat.Mode.QUADS) {
                    ByteBufferBuilder(INITIAL_BUFFER_SIZE).use { indexBuffer ->
                        meshData.sortQuads(indexBuffer, RenderSystem.getProjectionType().vertexSorting())
                        Blaze3DImmediateRenderer.draw(meshData, pipeline, modelView)
                    }
                } else {
                    Blaze3DImmediateRenderer.draw(meshData, pipeline, modelView)
                }
            }
        }
    }

    private fun emitFilledBox(builder: VertexConsumer, command: FilledBoxCommand, camera: Vec3) {
        emitFilledBox(builder, command.box, command.color, command.sides, camera)
    }

    private fun emitFilledBox(builder: VertexConsumer, box: AABB, color: ColorRGBA, sides: Int, camera: Vec3) {
        val x0 = (box.minX - camera.x).toFloat()
        val y0 = (box.minY - camera.y).toFloat()
        val z0 = (box.minZ - camera.z).toFloat()
        val x1 = (box.maxX - camera.x).toFloat()
        val y1 = (box.maxY - camera.y).toFloat()
        val z1 = (box.maxZ - camera.z).toFloat()

        if (hasSide(sides, DirectionMask.DOWN)) {
            quad(builder, color, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0)
        }
        if (hasSide(sides, DirectionMask.UP)) {
            quad(builder, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1)
        }
        if (hasSide(sides, DirectionMask.NORTH)) {
            quad(builder, color, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0)
        }
        if (hasSide(sides, DirectionMask.SOUTH)) {
            quad(builder, color, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1)
        }
        if (hasSide(sides, DirectionMask.WEST)) {
            quad(builder, color, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1)
        }
        if (hasSide(sides, DirectionMask.EAST)) {
            quad(builder, color, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0)
        }
    }

    private fun emitOutlineBox(builder: VertexConsumer, command: OutlineBoxCommand, camera: Vec3) {
        emitOutlineBox(builder, command.box, command.color, command.thickness, command.sides, camera)
    }

    private fun emitOutlineBox(
        builder: VertexConsumer,
        box: AABB,
        color: ColorRGBA,
        thickness: Float,
        sides: Int,
        camera: Vec3
    ) {
        val x0 = (box.minX - camera.x).toFloat()
        val y0 = (box.minY - camera.y).toFloat()
        val z0 = (box.minZ - camera.z).toFloat()
        val x1 = (box.maxX - camera.x).toFloat()
        val y1 = (box.maxY - camera.y).toFloat()
        val z1 = (box.maxZ - camera.z).toFloat()

        fun edge(
            sideA: Int,
            sideB: Int,
            ax: Float,
            ay: Float,
            az: Float,
            bx: Float,
            by: Float,
            bz: Float
        ) {
            if (hasSide(sides, sideA) || hasSide(sides, sideB)) {
                line(builder, ax, ay, az, bx, by, bz, color, thickness)
            }
        }

        edge(DirectionMask.DOWN, DirectionMask.NORTH, x0, y0, z0, x1, y0, z0)
        edge(DirectionMask.DOWN, DirectionMask.SOUTH, x0, y0, z1, x1, y0, z1)
        edge(DirectionMask.DOWN, DirectionMask.WEST, x0, y0, z0, x0, y0, z1)
        edge(DirectionMask.DOWN, DirectionMask.EAST, x1, y0, z0, x1, y0, z1)
        edge(DirectionMask.UP, DirectionMask.NORTH, x0, y1, z0, x1, y1, z0)
        edge(DirectionMask.UP, DirectionMask.SOUTH, x0, y1, z1, x1, y1, z1)
        edge(DirectionMask.UP, DirectionMask.WEST, x0, y1, z0, x0, y1, z1)
        edge(DirectionMask.UP, DirectionMask.EAST, x1, y1, z0, x1, y1, z1)
        edge(DirectionMask.NORTH, DirectionMask.WEST, x0, y0, z0, x0, y1, z0)
        edge(DirectionMask.NORTH, DirectionMask.EAST, x1, y0, z0, x1, y1, z0)
        edge(DirectionMask.SOUTH, DirectionMask.WEST, x0, y0, z1, x0, y1, z1)
        edge(DirectionMask.SOUTH, DirectionMask.EAST, x1, y0, z1, x1, y1, z1)
    }

    private fun emitLine(builder: VertexConsumer, command: LineCommand, camera: Vec3) {
        line(
            builder,
            (command.from.x - camera.x).toFloat(),
            (command.from.y - camera.y).toFloat(),
            (command.from.z - camera.z).toFloat(),
            (command.to.x - camera.x).toFloat(),
            (command.to.y - camera.y).toFloat(),
            (command.to.z - camera.z).toFloat(),
            command.color,
            command.thickness
        )
    }

    private fun emitLineStrip(builder: VertexConsumer, command: LineStripCommand, camera: Vec3) {
        val offsetX = command.offset.x - camera.x
        val offsetY = command.offset.y - camera.y
        val offsetZ = command.offset.z - camera.z
        for (index in 0 until command.points.lastIndex) {
            val from = command.points[index]
            val to = command.points[index + 1]
            line(
                builder,
                (from.x + offsetX).toFloat(),
                (from.y + offsetY).toFloat(),
                (from.z + offsetZ).toFloat(),
                (to.x + offsetX).toFloat(),
                (to.y + offsetY).toFloat(),
                (to.z + offsetZ).toFloat(),
                command.color,
                command.thickness
            )
        }
    }

    private fun emitTracer(
        builder: VertexConsumer,
        start: Vec3,
        box: AABB,
        color: ColorRGBA,
        thickness: Float,
        camera: Vec3
    ) {
        line(
            builder,
            (start.x - camera.x).toFloat(),
            (start.y - camera.y).toFloat(),
            (start.z - camera.z).toFloat(),
            ((box.minX + box.maxX) * 0.5 - camera.x).toFloat(),
            ((box.minY + box.maxY) * 0.5 - camera.y).toFloat(),
            ((box.minZ + box.maxZ) * 0.5 - camera.z).toFloat(),
            color,
            thickness
        )
    }

    private fun quad(
        builder: VertexConsumer,
        color: ColorRGBA,
        x0: Float,
        y0: Float,
        z0: Float,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        x3: Float,
        y3: Float,
        z3: Float
    ) {
        vertex(builder, x0, y0, z0, color)
        vertex(builder, x1, y1, z1, color)
        vertex(builder, x2, y2, z2, color)
        vertex(builder, x3, y3, z3, color)
    }

    private fun line(
        builder: VertexConsumer,
        x0: Float,
        y0: Float,
        z0: Float,
        x1: Float,
        y1: Float,
        z1: Float,
        color: ColorRGBA,
        thickness: Float
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val dz = z1 - z0
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length < 1.0E-6f) return
        val nx = dx / length
        val ny = dy / length
        val nz = dz / length

        lineVertex(builder, x0, y0, z0, color, nx, ny, nz, thickness)
        lineVertex(builder, x1, y1, z1, color, nx, ny, nz, thickness)
    }

    private fun vertex(builder: VertexConsumer, x: Float, y: Float, z: Float, color: ColorRGBA) {
        builder.addVertex(x, y, z).setColor(color.r, color.g, color.b, color.a)
    }

    private fun lineVertex(
        builder: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        color: ColorRGBA,
        nx: Float,
        ny: Float,
        nz: Float,
        thickness: Float
    ) {
        builder.addVertex(x, y, z)
            .setColor(color.r, color.g, color.b, color.a)
            .setNormal(nx, ny, nz)
            .setLineWidth(thickness)
    }

    private fun hasSide(sides: Int, side: Int): Boolean = sides and side != 0
    private fun ColorRGBA.withMultipliedAlpha(alphaMultiplier: Int): ColorRGBA =
        alpha((a * alphaMultiplier / 255.0f).toInt())

    data class BoxEntry(val box: AABB, val color: ColorRGBA)

    private data class FilledBoxCommand(val box: AABB, val color: ColorRGBA, val sides: Int, val through: Boolean)
    private data class OutlineBoxCommand(
        val box: AABB,
        val color: ColorRGBA,
        val thickness: Float,
        val sides: Int,
        val through: Boolean
    )
    private data class LineCommand(
        val from: Vec3,
        val to: Vec3,
        val color: ColorRGBA,
        val thickness: Float,
        val through: Boolean
    )
    private data class BoxBatchCommand(
        val entries: List<BoxEntry>,
        val filledAlpha: Int,
        val outlineAlpha: Int,
        val tracerAlpha: Int,
        val tracerStart: Vec3?,
        val thickness: Float,
        val through: Boolean
    )
    private data class LineStripCommand(
        val points: List<Vec3>,
        val offset: Vec3,
        val color: ColorRGBA,
        val thickness: Float,
        val through: Boolean
    )
}
