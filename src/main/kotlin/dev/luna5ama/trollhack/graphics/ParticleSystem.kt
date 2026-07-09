package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.RS
import dev.luna5ama.trollhack.graphics.buffer.Render2DUtils
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects
import dev.luna5ama.trollhack.graphics.buffer.pmvbo.PMVBObjects.draw
import dev.luna5ama.trollhack.graphics.color.ColorHSVA
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.utils.math.vectors.Vec2f
import dev.luna5ama.trollhack.utils.math.vectors.distanceSq
import dev.luna5ama.trollhack.utils.timing.TickTimer
import org.lwjgl.opengl.GL11.GL_POINTS
import org.lwjgl.opengl.GL11.glPointSize
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import kotlin.random.Random

class ParticleSystem(
    private val speed: Float = 0.01f,
    private val amount: Int = 150,
    private val lineRadius: Float = 200f,
    private val delta: Int = 10,
    private val followMouse: Boolean = true,
    private val rainbowColor: Boolean = false,
    private val initialColor: ColorRGBA = ColorRGBA.WHITE,
    instantInit: Boolean = true
) {

    private val lineRadiusSq = lineRadius * lineRadius
    private val initialAlpha = if (rainbowColor) 255 else initialColor.a

    private val tickTimer = TickTimer()
    private val startTime = System.currentTimeMillis()
    private val particleList: MutableList<Particle> = ArrayList()
    private val particles = mutableMapOf<Float, MutableList<Particle>>()
    private var drawLines = true
    val initialized = AtomicBoolean(false)
    var hue = 0

    init {
        if (instantInit) {
            init()
        }
    }

    fun init() {
        if (initialized.getAndSet(true)) return
        //println("Initializing")
        repeat(amount) {
            particleList.add(generateParticle())
        }
        particleList.forEach {
            particles.getOrPut(it.size) { mutableListOf() }.add(it)
        }
    }

    private fun tick() {
        hue = (((System.currentTimeMillis() - startTime).toInt() % 10000 / 10000F) * 255).toInt().coerceIn(0, 255)
        for (particle in particleList) {
            particle.tick(delta, speed, hue)
        }
    }

    fun render(alpha: Float = 1f) {
        if (!initialized.get()) init()
        if (tickTimer.tickAndReset(10)) { tick() }
        for (size in particles.keys) {
            val list = particles[size]!!
            glPointSize(size)
            GL_POINTS.draw(PMVBObjects.VertexMode.Universal) {
                list.forEach {
                    universal(it.x, it.y, it.color.alpha((it.alpha.toInt().coerceIn(0, initialAlpha) * alpha).toInt()))
                }
            }
        }
        var count = 0
        for (particle in particleList) {
            if (distanceSq(
                    if (followMouse) RS.mouseXF else RS.widthF / 2f,
                    if (followMouse) RS.mouseYF else RS.heightF / 2f,
                    particle.x,
                    particle.y
                ) > lineRadiusSq
            ) continue
            var nearestDistance = Float.MAX_VALUE
            var nearestParticle: Particle? = null
            for (otherParticle in particleList) {
                if (otherParticle == particle) continue
                val distance = particle.getDistanceSqTo(otherParticle)
                if (distance > lineRadiusSq) continue
                if (distance >= nearestDistance) continue
                if (distanceSq(
                        if (followMouse) RS.mouseXF else RS.widthF / 2f,
                        if (followMouse) RS.mouseYF else RS.heightF / 2f,
                        otherParticle.x,
                        otherParticle.y
                    ) > lineRadiusSq
                ) continue
                nearestDistance = distance
                nearestParticle = otherParticle
            }
            if (nearestParticle == null || !drawLines) continue
            val lineAlpha = (1.0f - sqrt(nearestDistance) / lineRadius).coerceIn(0.0f, 1.0f) * alpha
            Render2DUtils.drawLine(
                particle.x, particle.y,
                nearestParticle.x, nearestParticle.y,
                1f,
                particle.color.alpha((initialAlpha * lineAlpha).toInt()),
                nearestParticle.color.alpha((initialAlpha * lineAlpha).toInt())
            )
            count += 2
        }
    }

    inner class Particle(private var velocity: Vec2f, x: Float, y: Float, var size: Float, private val hue: Int) {

        var color = initialColor
        private var pos: Vec2f = Vec2f(x, y)
        var alpha = 0f; private set

        fun getDistanceSqTo(particle1: Particle): Float = getDistanceSqTo(particle1.x, particle1.y)

        private fun getDistanceSqTo(f: Float, f2: Float): Float = distanceSq(x, y, f, f2)

        var x: Float
            get() = pos.x
            set(f) {
                pos = Vec2f(f, pos.y)
            }

        var y: Float
            get() = pos.y
            set(f) {
                pos = Vec2f(pos.x, f)
            }

        fun tick(delta: Int, speed: Float, hue: Int) {
            x += velocity.x * delta * speed
            y += velocity.y * delta * speed
            if (alpha < initialAlpha) alpha += 0.05f * delta
            if (pos.x > RS.width) x = 0f
            if (pos.x < 0) x = RS.widthF
            if (pos.y > RS.height) y = 0f
            if (pos.y < 0) y = RS.heightF
            if (rainbowColor) color = ColorHSVA((hue + this.hue) % 255, 128, 240).toRGBA()
        }

    }

    private fun generateParticle(): Particle {
        val velocity = Vec2f((Math.random() * 2.0f - 1.0f).toFloat(), (Math.random() * 2.0f - 1.0f).toFloat())
        val x = Random.nextInt(RS.width).toFloat()
        val y = Random.nextInt(RS.height).toFloat()
        val size = (Math.random() * 5.0f).toFloat() + 2.0f
        return Particle(velocity, x, y, size, Random.nextInt(0, 32))
    }

}