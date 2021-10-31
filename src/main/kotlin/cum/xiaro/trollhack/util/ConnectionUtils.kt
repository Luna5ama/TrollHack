package cum.xiaro.trollhack.util

import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ConnectionUtils {

    fun requestRawJsonFrom(url: String, catch: (Exception) -> Unit = { it.printStackTrace() }): String? {
        return runConnection(url, { connection ->
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.requestMethod = "GET"
            connection.inputStream.readBytes().toString(Charsets.UTF_8)
        }, catch)
    }

    fun <T> runConnection(url: String, block: (HttpsURLConnection) -> T?, catch: (Exception) -> Unit = { it.printStackTrace() }): T? {
        (URL(url).openConnection() as HttpsURLConnection).run {
            return try {
                doOutput = true
                doInput = true
                block(this)
            } catch (e: Exception) {
                catch(e)
                null
            } finally {
                disconnect()
            }
        }
    }

}