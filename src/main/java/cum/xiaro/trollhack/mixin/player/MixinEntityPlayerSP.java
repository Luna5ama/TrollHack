package cum.xiaro.trollhack.mixin.player;

import com.mojang.authlib.GameProfile;
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent;
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent;
import cum.xiaro.trollhack.manager.managers.MessageManager;
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager;
import cum.xiaro.trollhack.module.modules.chat.PortalChat;
import cum.xiaro.trollhack.module.modules.combat.AntiAntiBurrow;
import cum.xiaro.trollhack.module.modules.movement.Velocity;
import cum.xiaro.trollhack.module.modules.player.Freecam;
import cum.xiaro.trollhack.module.modules.player.SwingLimiter;
import cum.xiaro.trollhack.util.Wrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

;

@Mixin(value = EntityPlayerSP.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityPlayerSP extends EntityPlayer {
    @Shadow @Final public NetHandlerPlayClient connection;
    @Shadow protected Minecraft mc;
    @Shadow private double lastReportedPosX;
    @Shadow private double lastReportedPosY;
    @Shadow private double lastReportedPosZ;
    @Shadow private float lastReportedYaw;
    @Shadow private int positionUpdateTicks;
    @Shadow private float lastReportedPitch;
    @Shadow private boolean serverSprintState;
    @Shadow private boolean serverSneakState;
    @Shadow private boolean prevOnGround;
    @Shadow private boolean autoJumpEnabled;

    public MixinEntityPlayerSP(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    @Shadow
    protected abstract void updateAutoJump(float p_189810_1_, float p_189810_2_);

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;closeScreen()V"))
    public void closeScreen(EntityPlayerSP player) {
        if (PortalChat.INSTANCE.isDisabled()) player.closeScreen();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    public void closeScreen(Minecraft minecraft, GuiScreen screen) {
        if (PortalChat.INSTANCE.isDisabled()) Wrapper.getMinecraft().displayGuiScreen(screen);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move$Inject$HEAD(MoverType type, double x, double y, double z, CallbackInfo ci) {
        switch (type) {
            case SELF: {
                EntityPlayerSP player = Wrapper.getPlayer();
                if (player == null) return;

                PlayerMoveEvent.Pre event = new PlayerMoveEvent.Pre(player);
                event.post();

                if (event.isModified()) {
                    double prevX = this.posX;
                    double prevZ = this.posZ;

                    super.move(type, event.getX(), event.getY(), event.getZ());
                    this.updateAutoJump((float) (this.posX - prevX), (float) (this.posZ - prevZ));
                    PlayerMoveEvent.Post.INSTANCE.post();

                    ci.cancel();
                }
            }
            case PLAYER: {
                break;
            }
            default: {
                if (AntiAntiBurrow.INSTANCE.isEnabled() || Velocity.shouldCancelMove()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void move$Inject$RETURN(MoverType type, double x, double y, double z, CallbackInfo ci) {
        PlayerMoveEvent.Post.INSTANCE.post();
    }

    // We have to return true here so it would still update movement inputs from Baritone and send packets
    @Inject(method = "isCurrentViewEntity", at = @At("HEAD"), cancellable = true)
    protected void mixinIsCurrentViewEntity(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.INSTANCE.isEnabled() && Freecam.INSTANCE.getCameraGuy() != null) {
            cir.setReturnValue(mc.getRenderViewEntity() == Freecam.INSTANCE.getCameraGuy());
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    public void sendChatMessage(String message, CallbackInfo ci) {
        MessageManager.INSTANCE.setLastPlayerMessage(message);
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    public void swingArm$Inject$HEAD(EnumHand hand, CallbackInfo ci) {
        if (!SwingLimiter.checkSwingDelay()) {
            ci.cancel();
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V", shift = At.Shift.AFTER))
    private void onUpdateInvokeOnUpdateWalkingPlayer(CallbackInfo ci) {
        Vec3d serverSidePos = PlayerPacketManager.INSTANCE.getPosition();
        float serverSideRotationX = PlayerPacketManager.INSTANCE.getRotationX();
        float serverSideRotationY = PlayerPacketManager.INSTANCE.getRotationY();

        this.lastReportedPosX = serverSidePos.x;
        this.lastReportedPosY = serverSidePos.y;
        this.lastReportedPosZ = serverSidePos.z;

        this.lastReportedYaw = serverSideRotationX;
        this.lastReportedPitch = serverSideRotationY;

        this.rotationYawHead = serverSideRotationX;
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateWalkingPlayerHead(CallbackInfo ci) {
        // Setup flags
        Vec3d position = new Vec3d(this.posX, this.getEntityBoundingBox().minY, this.posZ);
        float rotationX = this.rotationYaw;
        float rotationY = this.rotationPitch;
        boolean onGround = this.onGround;

        OnUpdateWalkingPlayerEvent.Pre eventPre = new OnUpdateWalkingPlayerEvent.Pre(position, rotationX, rotationY, onGround);
        eventPre.post();
        PlayerPacketManager.INSTANCE.applyPacket(eventPre);

        if (eventPre.getCancelled()) {
            ci.cancel();

            if (!eventPre.getCancelAll()) {
                // Copy flags from event
                position = eventPre.getPosition();
                rotationX = eventPre.getRotationX();
                rotationY = eventPre.getRotationY();
                onGround = eventPre.isOnGround();

                boolean moving = !eventPre.getCancelMove() && isMoving(position);
                boolean rotating = !eventPre.getCancelRotate() && isRotating(rotationX, rotationY);

                sendSprintPacket();
                sendSneakPacket();
                sendPlayerPacket(moving, rotating, position, rotationX, rotationY, onGround);

                this.prevOnGround = onGround;
            }

            ++this.positionUpdateTicks;
            this.autoJumpEnabled = this.mc.gameSettings.autoJump;
        }

        OnUpdateWalkingPlayerEvent.Post eventPos = new OnUpdateWalkingPlayerEvent.Post(position, rotationX, rotationY, onGround);
        eventPos.post();
    }

    private void sendSprintPacket() {
        boolean sprinting = this.isSprinting();

        if (sprinting != this.serverSprintState) {
            if (sprinting) {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SPRINTING));
            }
            this.serverSprintState = sprinting;
        }
    }

    private void sendSneakPacket() {
        boolean sneaking = this.isSneaking();

        if (sneaking != this.serverSneakState) {
            if (sneaking) {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING));
            }
            this.serverSneakState = sneaking;
        }
    }

    private void sendPlayerPacket(boolean moving, boolean rotating, Vec3d position, float rotationX, float rotationY, boolean onGround) {
        if (!this.isCurrentViewEntity()) return;

        if (this.isRiding()) {
            this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.motionX, -999.0D, this.motionZ, rotationX, rotationY, onGround));
            moving = false;
        } else if (moving && rotating) {
            this.connection.sendPacket(new CPacketPlayer.PositionRotation(position.x, position.y, position.z, rotationX, rotationY, onGround));
        } else if (moving) {
            this.connection.sendPacket(new CPacketPlayer.Position(position.x, position.y, position.z, onGround));
        } else if (rotating) {
            this.connection.sendPacket(new CPacketPlayer.Rotation(rotationX, rotationY, onGround));
        } else if (this.prevOnGround != onGround) {
            this.connection.sendPacket(new CPacketPlayer(onGround));
        }

        if (moving) {
            this.positionUpdateTicks = 0;
        }
    }

    private boolean isMoving(Vec3d position) {
        double xDiff = position.x - this.lastReportedPosX;
        double yDiff = position.y - this.lastReportedPosY;
        double zDiff = position.z - this.lastReportedPosZ;

        return this.positionUpdateTicks >= 20 || xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D;
    }

    private boolean isRotating(float rotationX, float rotationY) {
        return (rotationX - this.lastReportedYaw) != 0.0D || (rotationY - this.lastReportedPitch) != 0.0D;
    }
}
