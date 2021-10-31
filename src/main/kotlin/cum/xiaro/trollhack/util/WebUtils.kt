package cum.xiaro.trollhack.util

import java.net.InetSocketAddress
import java.net.Socket

object WebUtils {
    var isInternetDown = false; private set

    fun update() {
        isInternetDown = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 80), 100)
                false
            }
        } catch (e: Exception) {
            true // Either timeout or unreachable or failed DNS lookup.
        }
    }
}