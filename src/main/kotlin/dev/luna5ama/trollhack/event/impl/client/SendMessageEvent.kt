/*
 * Copyright (c) 2023 TrollHack 保留所有权利�?All Right Reserved.
 */

package dev.luna5ama.trollhack.event.impl.client

import dev.luna5ama.trollhack.event.api.*

class SendMessageEvent(var string: String) : IEvent, ICancellable by Cancellable(), IPosting by Companion {
    companion object : EventBus()
}