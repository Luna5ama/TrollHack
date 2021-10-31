package cum.xiaro.trollhack.event.events.player

import cum.xiaro.trollhack.event.*

class PlayerTravelEvent : Event, ICancellable by Cancellable(), EventPosting by Companion {
    companion object : NamedProfilerEventBus("trollPlayerTravel")
}