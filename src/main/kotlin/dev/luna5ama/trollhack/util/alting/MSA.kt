/*
 * Adapted from https://github.com/PlanetTeamSpeakk/DevLogin
 */
package dev.luna5ama.trollhack.util.alting

import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableMap
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.util.UUIDTypeAdapter
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.util.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.*
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.event.HyperlinkEvent

// Thanks to https://wiki.vg/Microsoft_Authentication_Scheme, Microsoft Docs and
// https://github.com/MultiMC/Launcher/blob/develop/launcher/minecraft/auth/flows/AuthContext.cpp for this
@Suppress("UnstableApiUsage")
class MSA {
    private val logger = LogManager.getLogger("DevLogin-MSA")

    private val urlRegex = "<a href=\"(.*?)\">.*?</a>".toRegex()
    private val tagRegex = "<([A-Za-z]*?).*?>(.*?)</\\1>".toRegex()
    private val tokenFile = File(System.getenv("MSA_TOKEN_FILE") ?: "${TrollHackMod.DIRECTORY}/dev_login_token.json")

    private var proxy = Proxy.NO_PROXY
    private var noDialog = false
    private var mainDialog: JFrame? = null
    private var deviceCode: String? = null // Strings sorted by steps they're acquired in.
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var xblToken: String? = null
    private var userHash: String? = null
    private var xstsToken: String? = null
    private var mcToken: String? = null

    /**
     * @return The Minecraft profile associated with the account used to login.
     */
    var profile: MinecraftProfile? = null
        private set

    // Access methods
    val isLoggedIn: Boolean
        /**
         * @return Whether we have successfully logged in.
         */
        get() = mcToken != null

    private var isCancelled = false

    fun loginBlocking(proxy: Proxy, storeRefreshToken: Boolean, noDialog: Boolean) {
        runBlocking {
            login(proxy, storeRefreshToken, noDialog)
        }
    }

    /**
     * Takes all the necessary steps to get a Minecraft token from a Microsoft account.
     * @param proxy The proxy to route requests through.
     * @param storeRefreshToken Whether the refresh token should be stored for later use.
     * @param noDialog Whether to print the code to the console or show a dialog
     * @throws IOException If anything goes wrong with the requests.
     * @throws InterruptedException If the thread gets interrupted while waiting.
     */
    @Throws(IOException::class, InterruptedException::class)
    suspend fun login(proxy: Proxy, storeRefreshToken: Boolean, noDialog: Boolean) {
        if (!noDialog) System.setProperty("java.awt.headless", "false") // Can't display dialogs otherwise.
        this.proxy = proxy
        this.noDialog = noDialog

        if (tokenFile.exists()) {
            val data = MoreObjects.firstNonNull(readData(), emptyMap())
            refreshToken = data!!["refreshToken"]
            mcToken = data["mcToken"]
            if (reqProfile()) {
                logger.info("Cached token is valid.")
                return
            }
            if (refreshToken != null) {
                logger.info("Cached token is invalid, requesting new one using refresh token.")
                refreshToken().onFailure {
                    try {
                        reqTokens()
                    } catch (e: IOException) {
                        // Ignored
                    }
                }
            } else {
                logger.info("Cached token is invalid.")
                reqTokens()
            }
        } else {
            reqTokens()
        }

        if (accessToken == null) return
        reqXBLToken()

        if (xblToken == null) return
        reqXSTSToken()

        if (xstsToken == null) return
        reqMinecraftToken()

        if (mcToken == null) {
            refreshToken = null // It's invalid.
        } else if (reqProfile()) {
            saveData(storeRefreshToken) // It's invalid.
        }
    }

    /**
     * Acquires a device code and asks the user to authenticate
     * with it. Then gets the access token and refresh token from
     * Microsoft once the user has authenticated.
     * @throws IOException If anything goes wrong with the request.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(IOException::class)
    private suspend fun reqTokens() {
        doRequest(
            "POST",
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
            "client_id=${URLEncoder.encode(CLIENT_ID, "UTF-8")}&" +
                "scope=${URLEncoder.encode("XboxLive.signin offline_access", "UTF-8")}",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
        ).onSuccess {
            val respObj = JsonParser().parse(it).asJsonObject
            deviceCode = respObj["device_code"].asString
            val verificationUri = respObj["verification_uri"].asString
            val userCode = respObj["user_code"].asString

            mainDialog = showDialog(
                "DevLogin MSA Authentication",
                "Please visit <a href=\"$verificationUri\">$verificationUri</a> and enter code <b>$userCode</b>."
            ) { isCancelled = true }

            val interval = respObj["interval"].asInt
            val expires = System.currentTimeMillis() + respObj["expires_in"].asInt * 1000L

            delay(interval * 1000L)
            try {
                tryReqToken(interval, expires)
            } catch (ignored: UnsupportedEncodingException) {
                // Impossible at this stage.
            }

        }.onFailure {
            showDialog(
                "DevLogin MSA Authentication - error",
                "Could not acquire a code to authenticate your Microsoft account with (${it.javaClass.simpleName})."
            )
            logger.error("Could not acquire a code to authenticate your Microsoft account with", it)
        }
    }

    /**
     * Continuously polls every set interval (should be 5 seconds)
     * to see if the user has authenticated yet.
     * @param interval The interval the Microsoft API would like us to use (should be 5 seconds).
     * @param expires Epoch when the device code expires.
     * @throws UnsupportedEncodingException Should not be possible.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(UnsupportedEncodingException::class)
    private suspend fun tryReqToken(interval: Int, expires: Long) {
        doRequest(
            "POST",
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
            String.format(
                "grant_type=urn:ietf:params:oauth:grant-type:device_code&scope=%s&client_id=%s&device_code=%s",
                URLEncoder.encode("XboxLive.signin offline_access", "UTF-8"),
                URLEncoder.encode(CLIENT_ID, "UTF-8"),
                URLEncoder.encode(deviceCode, "UTF-8")
            ),
            mapOf("Content-Type" to "application/x-www-form-urlencoded")
        ).onSuccess {
            val resp1Obj = Gson().fromJson(it, JsonObject::class.java)
            if (resp1Obj.has("error")) {
                if ("authorization_pending" == resp1Obj["error"].asString && System.currentTimeMillis() < expires && !isCancelled) try {
                    delay(interval * 1000L)
                    tryReqToken(interval, expires)
                    return
                } catch (e: InterruptedException) {
                    // Ignored
                } catch (e: UnsupportedEncodingException) {
                    // Ignored
                }
            } else {
                val mainDialog = mainDialog
                if (mainDialog != null) {
                    mainDialog.dispose()
                } else {
                    logger.info("Authentication complete, requesting tokens...")
                }
                accessToken = resp1Obj["access_token"].asString
                refreshToken = resp1Obj["refresh_token"].asString
            }
        }.onFailure {
            showDialog(
                "DevLogin MSA Authentication - error",
                "Could not acquire a token to authenticate your Microsoft account with (${it.javaClass.simpleName})."
            )
            logger.error("Could not acquire a token to authenticate your Microsoft account with", it)
        }
    }

    /**
     * Acquires a new access token using the stored refresh token.
     * when finished indicating whether a new access token was acquired.
     * @throws IOException If anything goes wrong with the request.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(IOException::class)
    private suspend fun refreshToken(): Result<String> {
        return doRequest(
            "POST",
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
            "grant_type=refresh_token&" +
                "scope=${URLEncoder.encode("XboxLive.signin offline_access", "UTF-8")}&" +
                "client_id=${URLEncoder.encode(CLIENT_ID, "UTF-8")}&" +
                "refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}",
            ImmutableMap.of("Content-Type", "application/x-www-form-urlencoded")
        ).onSuccess {
            val resp1Obj = Gson().fromJson(it, JsonObject::class.java)
            if (!resp1Obj.has("error")) {
                accessToken = resp1Obj["access_token"].asString
                refreshToken = resp1Obj["refresh_token"].asString
            }
        }.onFailure {
            showDialog(
                "DevLogin MSA Authentication - error",
                "Could not acquire a token to authenticate your Microsoft account with (${it.javaClass.simpleName})."
            )
            logger.error("Could not refresh token", it)
        }
    }

    /**
     * Requests the XBL token from Xbox Live using the Microsoft access token.
     */
    private suspend fun reqXBLToken() {
        val body =
            """
            {
                "Properties": {
                    "AuthMethod": "RPS",
                    "SiteName": "user.auth.xboxlive.com",
                    "RpsTicket": "d=$accessToken"    
                },
                "RelyingParty": "http://auth.xboxlive.com",
                "TokenType": "JWT"
            }
            """.trimIndent()

        doRequest(
            "POST",
            "https://user.auth.xboxlive.com/user/authenticate",
            body,
            ImmutableMap.of("Content-Type", "application/json", "Accept", "application/json"),
        ).onSuccess { resp ->
            val respObj = Gson().fromJson(resp, JsonObject::class.java)
            xblToken = respObj["Token"].asString
            userHash = respObj["DisplayClaims"].asJsonObject["xui"].asJsonArray[0].asJsonObject["uhs"].asString
        }.onFailure { e ->
            showDialog(
                "DevLogin MSA Authentication - error",
                "Could not acquire XBL token (${e.javaClass.simpleName})."
            )
            logger.error("Could not acquire XBL token", e)
        }
    }

    /**
     * Requests the XSTS token from Xbox Live using the XBL token.
     */
    private suspend fun reqXSTSToken() {
        val body =
            """
            {
                "Properties": {
                    "SandboxId": "RETAIL",
                    "UserTokens": ["$xblToken"]
                },
                "RelyingParty": "rp://api.minecraftservices.com/",
                "TokenType": "JWT"
            }
            """.trimIndent()

        doRequest(
            "POST",
            "https://xsts.auth.xboxlive.com/xsts/authorize",
            body,
            ImmutableMap.of("Content-Type", "application/json", "Accept", "application/json")
        ).onSuccess {
            val respObject = Gson().fromJson(it, JsonObject::class.java)
            //respObject.addProperty("XErr", 2148916238L);
            if (respObject.has("XErr")) showDialog(
                "DevLogin MSA Authentication - error", "Could not acquire XSTS token<br>" +
                    "Error code: " + respObject["XErr"] + ", message: " + respObject["Message"] + ", redirect: " +
                    (if (respObject.has("Redirect")) "<a href=\"" + respObject["Redirect"] + "\">" +
                        respObject["Redirect"] + "</a>" else "null") + "<br>" +
                    "Have a look <a href=\"https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XSTS\">here</a> " +
                    "for a short list of known error codes."
            ) else xstsToken = respObject["Token"].asString
        }.onFailure {
            showDialog(
                "DevLogin MSA Authentication - error",
                """Could not acquire XSTS token (${it.javaClass.simpleName})."""
            )
            logger.error("Could not acquire XSTS token", it)
        }
    }

    /**
     * Requests the Minecraft token from Minecraft Services using the XSTS token.
     */
    private suspend fun reqMinecraftToken() {
        val body = """{"identityToken": "XBL3.0 x=$userHash;$xstsToken"}"""

        doRequest(
            "POST",
            "https://api.minecraftservices.com/authentication/login_with_xbox",
            body,
            ImmutableMap.of("Content-Type", "application/json", "Accept", "application/json")
        ).onSuccess { resp: String ->
            val respObject = Gson().fromJson(resp, JsonObject::class.java)
            mcToken = if (respObject.has("error") && respObject["error"].asString == "UnauthorizedOperationException") {
                null
            } else {
                respObject["access_token"].asString
            }
        }.onFailure {
            showDialog(
                "DevLogin MSA Authentication - error",
                "Could not acquire Minecraft token (${it.javaClass.simpleName})."
            )
            logger.error("Could not acquire Minecraft token", it)
        }
    }

    /**
     * Requests the profile from Minecraft Services using the Minecraft token.
     * Required to login as logging in required the username and uuid of the player.
     * Also used to check if the token is valid.
     *
     * @return Whether the account associated with this token owns Minecraft.
     */
    private suspend fun reqProfile(): Boolean {
        if (profile != null) return true
        var ownsMc = false

        doRequest(
            "GET",
            "https://api.minecraftservices.com/minecraft/profile",
            null,
            ImmutableMap.of("Authorization", "Bearer $mcToken")
        ).onSuccess { it: String? ->
            val respObj = Gson().fromJson(it, JsonObject::class.java)
            ownsMc = respObj != null
                && !respObj.has("error")
                && respObj.has("name")
            if (ownsMc) {
                profile = MinecraftProfile(
                    respObj["name"].asString,
                    UUIDTypeAdapter.fromString(respObj["id"].asString),
                    mcToken!!
                )
            }
        }.onFailure {
            logger.catching(it)
        }

        return ownsMc
    }

    // Utility methods
    /**
     * Performs an HTTP request.
     * @param method The method this HTTP request uses. E.g. GET, POST, DELETE, etc.
     * @param url The URL to make this request to.
     * @param body The body of the request. Used for most request methods except GET.
     * @param headers The headers to attach to this request. E.g. Content-Type or User-Agent.
     */
    private suspend fun doRequest(
        method: String,
        url: String,
        body: String?,
        headers: Map<String, String>?,
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection(proxy) as HttpURLConnection
                connection.requestMethod = method
                if (headers != null) for ((key, value) in headers) connection.setRequestProperty(key, value)
                if (body != null) {
                    connection.doOutput = true
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                        it.write(body)
                    }
                }

                val stream = try {
                    connection.inputStream
                } catch (e: IOException) {
                    connection.errorStream
                }

                stream?.use { it.readText() } ?: ""
            }
        }
    }

    /**
     * Shows a basic Swing dialog with a title and a message.
     * @param title The title of the dialog.
     * @param message The message this dialog should contain.
     */
    @Suppress("SameParameterValue")
    fun showDialog(title: String, message: String) {
        showDialog(title, message, null)
    }

    /**
     * Shows a basic Swing dialog with a title and a message.
     * @param title The title of the dialog.
     * @param message The message this dialog should contain.
     * @param onDispose The runnable called when the dialog is disposed (closed).
     */
    private fun showDialog(title: String?, message: String, onDispose: Runnable?): JFrame? {
        if (noDialog) {
            logger.info(
                """
                
                -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                ${tagRegex.replace(urlRegex.replace(message, "$1"), "$2").replace("<br>", "\n")}
                -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                """.trimIndent()
            )
            return null
        }

        // Calls to setLookAndFeel on Mac appear to freeze the game.
        if (!Minecraft.IS_RUNNING_ON_MAC) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: ClassNotFoundException) {
                logger.error("Could not set system look and feel.", e)
            } catch (e: InstantiationException) {
                logger.error("Could not set system look and feel.", e)
            } catch (e: IllegalAccessException) {
                logger.error("Could not set system look and feel.", e)
            } catch (e: UnsupportedLookAndFeelException) {
                logger.error("Could not set system look and feel.", e)
            }
        }

        val frame = JFrame(title)
        frame.layout = GridBagLayout()
        val textPane = JEditorPane()
        textPane.contentType = "text/html"
        textPane.text = "<html>$message</html>"
        textPane.addHyperlinkListener {
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(it.url.toURI())
                } catch (ex: IOException) {
                    logger.error("Error while trying to browse to " + it.url, ex)
                } catch (ex: URISyntaxException) {
                    logger.error("Error while trying to browse to " + it.url, ex)
                }
            }
        }

        textPane.isEditable = false
        textPane.isOpaque = false
        frame.add(textPane)
        frame.pack()
        frame.setSize(frame.width + 20, frame.height + 50)
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                onDispose?.run()
            }
        })
        frame.isVisible = true

        return frame
    }

    /**
     * Stores some of the tokens that are required to login.
     * @param storeRefreshToken Whether the refresh token should be stored.
     */
    private fun saveData(storeRefreshToken: Boolean) {
        val data: MutableMap<String, String?> = HashMap()
        data["refreshToken"] = if (storeRefreshToken) refreshToken else null
        data["mcToken"] = mcToken
        try {
            PrintWriter(tokenFile, "UTF-8").use { writer ->
                writer.print(GsonBuilder().setPrettyPrinting().create().toJson(data))
                writer.flush()
            }
        } catch (e: IOException) {
            logger.error("Could not save token data.", e)
        }
    }

    /**
     * Reads data that was potentially saved before.
     * @return The data that was saved before or null if an error occurs or there is no stored data.
     */
    private fun readData(): Map<String, String>? {
        return try {
            if (tokenFile.exists()) {
                Gson().fromJson<Map<String, String>>(
                    tokenFile.readText(),
                    object : TypeToken<Map<String, String>>() {}.type
                )
            } else {
                null
            }
        } catch (e: IOException) {
            logger.error("Could not read token data.", e)
            null
        }
    }

    companion object {
        private const val CLIENT_ID = "bfcbedc1-f14e-441f-a136-15aec874e6c2" // DevLogin Azure application client id
    }
}