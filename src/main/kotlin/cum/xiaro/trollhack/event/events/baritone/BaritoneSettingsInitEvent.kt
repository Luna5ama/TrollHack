package cum.xiaro.trollhack.event.events.baritone

import cum.xiaro.trollhack.event.Event
import cum.xiaro.trollhack.event.EventBus
import cum.xiaro.trollhack.event.EventPosting

/**
 * Posted at the return of when Baritone's Settings are initialized.
 */
object BaritoneSettingsInitEvent : Event, EventPosting by EventBus()