package cum.xiaro.trollhack.util.graphics

import cum.xiaro.trollhack.util.extension.*
import cum.xiaro.trollhack.util.math.MathUtils
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class Easing {
    LINEAR {
        override val opposite: Easing
            get() = this

        override fun inc0(x: Float): Float {
            return x
        }
    },
    IN_SINE {
        override val opposite: Easing
            get() = OUT_SINE

        override fun inc0(x: Float): Float {
            return 1.0f - cos((x * PI_FLOAT) / 2.0f)
        }
    },
    OUT_SINE {
        override val opposite: Easing
            get() = OUT_SINE

        override fun inc0(x: Float): Float {
            return sin((x * PI_FLOAT) / 2.0f)
        }
    },
    IN_OUT_SINE {
        override val opposite: Easing
            get() = IN_OUT_SINE

        override fun inc0(x: Float): Float {
            return -(cos(PI_FLOAT * x) - 1.0f) / 2.0f
        }
    },
    IN_QUAD {
        override val opposite: Easing
            get() = OUT_QUAD

        override fun inc0(x: Float): Float {
            return x.sq
        }
    },
    OUT_QUAD {
        override val opposite: Easing
            get() = IN_QUAD

        override fun inc0(x: Float): Float {
            return 1.0f - (1.0f - x).sq
        }
    },
    IN_OUT_QUAD {
        override val opposite: Easing
            get() = IN_OUT_QUAD

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) 2.0f * x * x else 1.0f - (-2.0f * x + 2.0f).sq / 2.0f
        }
    },
    IN_CUBIC {
        override val opposite: Easing
            get() = OUT_CUBIC

        override fun inc0(x: Float): Float {
            return x.cubic
        }
    },
    OUT_CUBIC {
        override val opposite: Easing
            get() = IN_CUBIC

        override fun inc0(x: Float): Float {
            return 1.0f - (1.0f - x).cubic
        }
    },
    IN_OUT_CUBIC {
        override val opposite: Easing
            get() = IN_OUT_CUBIC

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) 4.0f * x.cubic else 1.0f - (-2.0f * x + 2.0f).cubic / 2.0f
        }
    },
    IN_QUART {
        override val opposite: Easing
            get() = OUT_QUART

        override fun inc0(x: Float): Float {
            return x.quart
        }
    },
    OUT_QUART {
        override val opposite: Easing
            get() = IN_QUART

        override fun inc0(x: Float): Float {
            return 1.0f - (1.0f - x).quart
        }
    },
    IN_OUT_QUART {
        override val opposite: Easing
            get() = IN_OUT_QUART

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) 8.0f * x.quart else 1.0f - (-2.0f * x + 2.0f).quart / 2.0f
        }
    },
    IN_QUINT {
        override val opposite: Easing
            get() = OUT_QUINT

        override fun inc0(x: Float): Float {
            return x.quint
        }
    },
    OUT_QUINT {
        override val opposite: Easing
            get() = IN_QUINT

        override fun inc0(x: Float): Float {
            return 1.0f - (1.0f - x).quint
        }
    },
    IN_OUT_QUINT {
        override val opposite: Easing
            get() = IN_OUT_QUINT

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) 16.0f * x.quint else 1.0f - (-2 * x + 2).quint / 2.0f
        }
    },
    IN_EXPO {
        override val opposite: Easing
            get() = OUT_EXPO

        override fun inc0(x: Float): Float {
            return if (x == 0.0f) 0.0f else 2.0f.pow(10.0f * x - 10.0f)
        }
    },
    OUT_EXPO {
        override val opposite: Easing
            get() = IN_EXPO

        override fun inc0(x: Float): Float {
            return if (x == 1.0f) 1.0f else 1.0f - 2.0f.pow(-10.0f * x)
        }
    },
    IN_OUT_EXPO {
        override val opposite: Easing
            get() = IN_OUT_EXPO

        override fun inc0(x: Float): Float {
            return when {
                x == 0.0f -> 0.0f
                x == 1.0f -> 1.0f
                x < 0.5f -> 2.0f.pow(20.0f * x - 10.0f) / 2.0f
                else -> (2.0f - 2.0f.pow(-20.0f * x + 10.0f)) / 2.0f
            }
        }
    },
    IN_CIRC {
        override val opposite: Easing
            get() = OUT_CIRC

        override fun inc0(x: Float): Float {
            return 1.0f - sqrt(1.0f - x.sq)
        }
    },
    OUT_CIRC {
        override val opposite: Easing
            get() = IN_CIRC

        override fun inc0(x: Float): Float {
            return sqrt(1.0f - (x - 1.0f).sq)
        }
    },
    IN_OUT_CIRC {
        override val opposite: Easing
            get() = IN_OUT_CIRC

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) (1.0f - sqrt(1.0f - (2.0f * x).sq)) / 2.0f
            else (sqrt(1.0f - (-2.0f * x + 2.0f).sq) + 1.0f) / 2.0f
        }
    },
    IN_BACK {
        override val opposite: Easing
            get() = OUT_BACK

        override fun inc0(x: Float): Float {
            return 2.70158f * x.cubic - 1.70158f * x.sq;
        }
    },
    OUT_BACK {
        override val opposite: Easing
            get() = IN_BACK

        override fun inc0(x: Float): Float {
            return 1.0f + 2.70158f * (x - 1.0f).cubic + 1.70158f * (x - 1.0f).sq
        }
    },
    IN_OUT_BACK {
        override val opposite: Easing
            get() = IN_OUT_BACK

        override fun inc0(x: Float): Float {
            return if (x < 0.5f) (2.0f * x).sq * ((2.5949094f + 1.0f) * 2.0f * x - 2.5949094f) / 2.0f
            else ((2.0f * x - 2.0f).sq * ((2.5949094f + 1.0f) * (x * 2.0f - 2.0f) + 2.5949094f) + 2.0f) / 2.0f
        }
    };

    fun incOrDecOpposite(x: Float, min: Float, max: Float): Float {
        val delta = when {
            max == min -> return min
            max > min -> inc(x)
            else -> opposite.inc(x)
        }
        return MathUtils.lerp(min, max, delta)
    }

    fun incOrDec(x: Float, min: Float, max: Float): Float {
        return MathUtils.lerp(min, max, inc(x))
    }

    @Suppress("NAME_SHADOWING")
    fun inc(x: Float, min: Float, max: Float): Float {
        var min = min
        var max = max

        if (max == min) {
            return 0.0f
        } else if (max < min) {
            val oldMax = max
            max = min
            min = oldMax
        }

        if (x <= 0.0f) {
            return min
        } else if (x >= 1.0f) {
            return max
        }

        return MathUtils.lerp(min, max, inc0(x))
    }

    fun inc(x: Float, max: Float): Float {
        if (max == 0.0f) {
            return 0.0f
        }

        if (x <= 0.0f) {
            return 0.0f
        } else if (x >= 1.0f) {
            return max
        }

        return inc0(x) * max
    }

    fun inc(x: Float): Float {
        if (x <= 0.0f) {
            return 0.0f
        } else if (x >= 1.0f) {
            return 1.0f
        }

        return inc0(x)
    }

    @Suppress("NAME_SHADOWING")
    fun dec(x: Float, min: Float, max: Float): Float {
        var min = min
        var max = max

        if (max == min) {
            return 0.0f
        } else if (max < min) {
            val oldMax = max
            max = min
            min = oldMax
        }

        if (x <= 0.0f) {
            return max
        } else if (x >= 1.0f) {
            return min
        }

        return MathUtils.lerp(min, max, dec0(x))
    }

    fun dec(x: Float, max: Float): Float {
        if (max == 0.0f) {
            return 0.0f
        }

        if (x <= 0.0f) {
            return max
        } else if (x >= 1.0f) {
            return 0.0f
        }

        return dec0(x) * max
    }

    fun dec(x: Float): Float {
        if (x <= 0.0f) {
            return 1.0f
        } else if (x >= 1.0f) {
            return 0.0f
        }

        return dec0(x)
    }

    abstract val opposite: Easing

    protected abstract fun inc0(x: Float): Float

    private fun dec0(x: Float): Float {
        return 1.0f - inc0(x)
    }

    companion object {
        @JvmStatic
        fun toDelta(start: Long, length: Int): Float {
            return toDelta(start, length.toFloat())
        }

        @JvmStatic
        fun toDelta(start: Long, length: Long): Float {
            return toDelta(start, length.toFloat())
        }

        @JvmStatic
        fun toDelta(start: Long, length: Float): Float {
            return (toDelta(start).toFloat() / length).coerceIn(0.0f, 1.0f)
        }

        @JvmStatic
        fun toDelta(start: Long): Long {
            return System.currentTimeMillis() - start
        }
    }
}