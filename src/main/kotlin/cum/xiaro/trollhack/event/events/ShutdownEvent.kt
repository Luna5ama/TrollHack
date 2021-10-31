package cum.xiaro.trollhack.event.events

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

object ShutdownEvent : Event, EventPosting by EventBus()