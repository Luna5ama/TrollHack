package dev.luna5ama.trollhack.mixins.player;

import com.mojang.authlib.GameProfile;
import dev.luna5ama.trollhack.event.impl.UpdateEvent;
import dev.luna5ama.trollhack.event.impl.player.*;
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager;
import dev.luna5ama.trollhack.manager.managers.RotationManager;
import dev.luna5ama.trollhack.modules.impl.movement.NoSlowDown;
import dev.luna5ama.trollhack.modules.impl.movement.Velocity;
import dev.luna5ama.trollhack.utils.MinecraftWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {

    @Shadow private double xLast;

    @Shadow private double yLast;

    @Shadow private double zLast;

    @Shadow private float yRotLast;

    @Shadow private float xRotLast;

    @Shadow private boolean wasSprinting;

    @Shadow @Final public ClientPacketListener connection;

    @Shadow protected abstract boolean isControlledCamera();

    @Shadow private boolean lastOnGround;

    @Shadow private int positionReminder;

    @Shadow private boolean autoJumpEnabled;

    @Shadow @Final protected Minecraft minecraft;

    @Shadow protected abstract void updateAutoJump(float dx, float dz);

    @Shadow public abstract boolean isShiftKeyDown();

    @Unique
    private boolean resetBodyRotation = false;

    @Unique
    private boolean trollhack$wasShiftKeyDown = false;

    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At(value = "INVOKE",
            target = "net/minecraft/client/player/AbstractClientPlayer.tick()V",
            ordinal = 0), method = "tick()V")
    private void onTick(CallbackInfo ci)
    {
        UpdateEvent.INSTANCE.post();
    }


    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double d, CallbackInfo ci) {
        PlayerPushOutOfBlockEvent.Push event = new PlayerPushOutOfBlockEvent.Push();
        event.post();
        if (event.getCancelled()){
            ci.cancel();
        }

    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", shift = At.Shift.AFTER))
    private void onTick$sendMovementPackets(CallbackInfo ci) {
        Vec3 serverSidePos = PlayerPacketManager.INSTANCE.getPosition();
        float serverSideYRot = PlayerPacketManager.INSTANCE.getRotationX();
        float serverSideXRot = PlayerPacketManager.INSTANCE.getRotationY();

        this.xLast = serverSidePos.x;
        this.yLast = serverSidePos.y;
        this.zLast = serverSidePos.z;

        this.yRotLast = serverSideYRot;
        this.xRotLast = serverSideXRot;

        this.yHeadRot = serverSideYRot;
        if (resetBodyRotation) this.yBodyRot = serverSideYRot;
    }

    @Unique
    private void trollhack$sendSprintPacket() {
        boolean sprinting = this.isSprinting();

        if (sprinting != this.wasSprinting) {
            if (sprinting) {
                this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            } else {
                this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
            this.wasSprinting = sprinting;
        }
    }

    @Unique
    private void trollhack$sendSneakPacket() {
        boolean sneaking = this.isShiftKeyDown();

        if (sneaking != this.trollhack$wasShiftKeyDown) {
            if (sneaking) {
                this.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, this.isSprinting())));
            } else {
                this.connection.send(new ServerboundPlayerInputPacket(Input.EMPTY));
            }
            this.trollhack$wasShiftKeyDown = sneaking;
        }
    }

    @Unique
    private void trollhack$sendPlayerPacket(
            boolean moving,
            boolean rotating,
            Vec3 position,
            float yRot,
            float xRot,
            boolean onGround,
            boolean horizontalCollision
    ) {
        if (!this.isControlledCamera()) return;

        if (this.isPassenger()) {
            Vec3 Vec3 = this.getDeltaMovement();
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    Vec3.x,
                    -999.0,
                    Vec3.z,
                    yRot,
                    xRot,
                    onGround,
                    horizontalCollision
            ));
            moving = false;
        } else if (moving && rotating) {
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    position.x,
                    position.y,
                    position.z,
                    yRot,
                    xRot,
                    onGround,
                    horizontalCollision
            ));
        } else if (moving) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(position.x, position.y, position.z, onGround, horizontalCollision));
        } else if (rotating) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, horizontalCollision));
        } else if (this.lastOnGround != onGround) {
            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision));
        }

        if (moving) {
            this.positionReminder = 0;
        }
    }

    @Unique
    private boolean trollhack$isMoving(Vec3 position) {
        double xDiff = position.x - this.xLast;
        double yDiff = position.y - this.yLast;
        double zDiff = position.z - this.zLast;

        return this.positionReminder >= 20 || xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D;
    }

    @Unique
    private boolean trollhack$isRotating(float yRot, float xRot) {
        return (yRot - this.yRotLast) != 0.0D || (xRot - this.xRotLast) != 0.0D;
    }

    @Inject(method = {"sendPosition"}, at = {@At(value = "HEAD")}, cancellable = true)
    private void preMotion(CallbackInfo ci) {
        resetBodyRotation = false;
        Vec3 position = new Vec3(this.getX(), this.getBoundingBox().minY, this.getZ());
        float yRot = this.getYRot();
        float xRot = this.getXRot();
        boolean onGround = this.onGround();

        OnUpdateWalkingPlayerEvent.Pre eventPre = new OnUpdateWalkingPlayerEvent.Pre(
                position,
                yRot,
                xRot,
                onGround
        );
        eventPre.post();
        PlayerPacketManager.INSTANCE.applyPacket(eventPre);
        RotationManager.INSTANCE.applyRotation(eventPre);

        if (eventPre.getCancelled()) {
            ci.cancel();

            if (!eventPre.getCancelAll()) {
                resetBodyRotation = true;
                // Copy flags from event
                position = eventPre.getPosition();
                yRot = eventPre.getRotationX();
                xRot = eventPre.getRotationY();
                onGround = eventPre.isOnGround();

                boolean moving = !eventPre.getCancelMove() && trollhack$isMoving(position);
                boolean rotating = !eventPre.getCancelRotate() && trollhack$isRotating(yRot, xRot);

                trollhack$sendSprintPacket();
                trollhack$sendSneakPacket();
                trollhack$sendPlayerPacket(moving, rotating, position, yRot, xRot, onGround, horizontalCollision);

                this.lastOnGround = onGround;
                if (rotating) {
                    this.yRotLast = yRot;
                    this.xRotLast = xRot;
                }
            }

            ++this.positionReminder;
            this.autoJumpEnabled = this.minecraft.options.autoJump().get();

            OnUpdateWalkingPlayerEvent.Post eventPos = new OnUpdateWalkingPlayerEvent.Post(
                    position,
                    yRot,
                    xRot,
                    onGround
            );
            eventPos.post();
        }
    }

    @Inject(method = {"sendPosition"}, at = {@At(value = "RETURN")})
    private void postMotion(CallbackInfo info) {

        Vec3 position = new Vec3(this.getX(), this.getBoundingBox().minY, this.getZ());
        float rotationX = this.getYRot();
        float rotationY = this.getXRot();
        boolean onGround = this.onGround();

        OnUpdateWalkingPlayerEvent.Post eventPos = new OnUpdateWalkingPlayerEvent.Post(
                position,
                rotationX,
                rotationY,
                onGround
        );
        eventPos.post();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void onMoveHook(MoverType type, Vec3 pos, CallbackInfo ci) {
        switch (type) {
            case MoverType.SELF: {
                LocalPlayer player = MinecraftWrapper.getPlayer();
                if (player == null) return;

                PlayerMoveEvent.Pre event = new PlayerMoveEvent.Pre(player);
                event.post();

                if (event.isModified()) {
                    double prevX = this.getX();
                    double prevZ = this.getZ();

                    super.move(type, new Vec3(event.getX(), event.getY(), event.getZ()));
                    this.updateAutoJump((float) (this.getX() - prevX), (float) (this.getZ() - prevZ));
                    PlayerMoveEvent.Post.INSTANCE.post();

                    ci.cancel();
                }
            }
            case PLAYER: {
                break;
            }
            default: {
                if (/*AntiAntiBurrow.INSTANCE.isEnabled() || */Velocity.shouldCancelMove()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void move$Inject$RETURN(MoverType type, Vec3 pos, CallbackInfo ci) {
        PlayerMoveEvent.Post.INSTANCE.post();
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(LocalPlayer player) {
        return NoSlowDown.shouldApplySlowdown(player);
    }

    /**
     * Getter method for what used to be airStrafingSpeed.
     * Overridden to allow for the speed to be modified by hacks.
     */
    @Override
    protected float getFlyingSpeed() {
        AirStrafingSpeedEvent event =
                new AirStrafingSpeedEvent(super.getFlyingSpeed());
        event.post();
        return event.getSpeed();
    }

    @Override
    public boolean isInWater() {
        boolean inWater = super.isInWater();
        IsPlayerInWaterEvent event = new IsPlayerInWaterEvent(inWater);
        event.post();

        return event.getInWater();
    }
}
