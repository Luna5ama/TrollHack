package dev.luna5ama.trollhack.mixins.devfix;

import dev.luna5ama.trollhack.util.alting.MSA;
import dev.luna5ama.trollhack.util.alting.MinecraftProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.util.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    private static final Logger LOG = LogManager.getLogger("MSA Login");

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/main/GameConfiguration$UserInformation;session:Lnet/minecraft/util/Session;"))
    public Session Redirect$init$Field$UserInformation$session(GameConfiguration.UserInformation instance) {
        String msaStore = System.getenv("MSA_STORE");
        if (msaStore == null) {
            LOG.info("Environment variable MSA_STORE not set, using default login.");
            return instance.session;
        }

        boolean store = Boolean.parseBoolean(msaStore);
        boolean noDialog = Boolean.parseBoolean(System.getenv("MSA_NO_DIALOG"));

        MSA msa = new MSA();
        try {
            msa.loginBlocking(Proxy.NO_PROXY, store, noDialog);
        } catch (Exception e) {
            LOG.error("Could not login using Microsoft account.", e);
            return instance.session;
        }

        MinecraftProfile profile = msa.getProfile();
        if (!msa.isLoggedIn() || profile == null) {
            String message = "Either something went wrong or the account you used to login does not own Minecraft.";
            if (noDialog) {
                LOG.error(message);
            } else {
                msa.showDialog("DevLogin MSA Authentication - error", message);
            }
            return instance.session;
        }

        LOG.info("Logged in as " + profile.getName());
        return new Session(profile.getName(), profile.getUuid().toString(), profile.getToken(), "msa");
    }
}
