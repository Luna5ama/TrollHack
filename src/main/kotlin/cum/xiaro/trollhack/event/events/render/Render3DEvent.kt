package cum.xiaro.trollhack.event.events.render

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventPosting
import cum.xiaro.trollhack.event.NamedProfilerEventBus
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.accessor.renderPosX
import cum.xiaro.trollhack.util.accessor.renderPosY
import cum.xiaro.trollhack.util.accessor.renderPosZ
import cum.xiaro.trollhack.util.graphics.RenderUtils3D

object Render3DEvent : Event, EventPosting by NamedProfilerEventBus("trollRender3D")