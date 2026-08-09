package za.co.infernos.mekanismelements.mixin;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.client.ClientTickHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.item.interfaces.IJetpackItem;
import mekanism.common.item.interfaces.IJetpackItem.JetpackMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import za.co.infernos.mekanismelements.common.registries.MSGases;

/**
 * Ammonia hover used to multiply existing horizontal velocity (motion * 1.1) while any
 * move key was held. That preserves the old direction every tick, so turning the mouse
 * feels like ice drift until you release keys or hit a wall.
 *
 * Fix: accelerate along the player's current wish direction (input + yaw) and damp the
 * perpendicular component so steering bites immediately, while keeping a similar top speed.
 */
@Mixin(value = ClientTickHandler.class, remap = false)
public class MixinClientTickHandler {

    /** Match previous speedSq < 0.6 cap (max horizontal speed ~0.775). */
    private static final double MAX_SPEED_SQ = 0.6D;
    private static final double MAX_SPEED = Math.sqrt(MAX_SPEED_SQ);
    /** Per-tick acceleration along wish dir (after air drag this still climbs). */
    private static final double ACCEL = 0.14D;
    /**
     * Keep this fraction of velocity perpendicular to wish each tick.
     * Lower = snappier turns / less skate. 0.45 bleeds ~half the lateral every tick.
     */
    private static final double LATERAL_KEEP = 0.45D;
    /** Strong brake when movement keys released (previous behaviour). */
    private static final double BRAKE = 0.5D;

    @Inject(method = "onTick", at = @At("TAIL"), remap = false)
    private void onClientTickEnd(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack primaryJetpack = IJetpackItem.getPrimaryJetpack(player);
        if (primaryJetpack.isEmpty()) {
            return;
        }

        IJetpackItem jetpackItem = (IJetpackItem) primaryJetpack.getItem();
        JetpackMode primaryMode = jetpackItem.getJetpackMode(primaryJetpack);
        JetpackMode mode = IJetpackItem.getPlayerJetpackMode(player, primaryMode, p -> p.input.jumping);
        if (mode != JetpackMode.HOVER) {
            return;
        }

        IChemicalHandler chemicalHandler = Capabilities.CHEMICAL.getCapability(primaryJetpack);
        if (chemicalHandler == null || chemicalHandler.getChemicalTanks() <= 0) {
            return;
        }
        ChemicalStack stored = chemicalHandler.getChemicalInTank(0);
        if (!stored.is(MSGases.AMMONIA)) {
            return;
        }

        applyAmmoniaHoverHorizontal(player);
    }

    private static void applyAmmoniaHoverHorizontal(LocalPlayer player) {
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        Vec3 motion = player.getDeltaMovement();

        if (forward == 0.0F && strafe == 0.0F) {
            // Stronger braking when keys released (sliding-on-ice fix).
            player.setDeltaMovement(motion.x * BRAKE, motion.y, motion.z * BRAKE);
            return;
        }

        // Normalize input so diagonal isn't faster.
        float inputMag = Mth.sqrt(forward * forward + strafe * strafe);
        if (inputMag > 1.0F) {
            forward /= inputMag;
            strafe /= inputMag;
        }

        // World-space wish direction (same basis as LivingEntity#moveRelative).
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0F);
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);
        double wishX = strafe * cos - forward * sin;
        double wishZ = forward * cos + strafe * sin;

        double vx = motion.x;
        double vz = motion.z;

        // Accelerate along current look/input direction (not along old velocity).
        double alongWish = vx * wishX + vz * wishZ;
        double addSpeed = MAX_SPEED - alongWish;
        if (addSpeed > 0.0D) {
            double accelAmount = Math.min(ACCEL, addSpeed);
            vx += wishX * accelAmount;
            vz += wishZ * accelAmount;
            alongWish = vx * wishX + vz * wishZ;
        }

        // Kill sideways drift so mouse yaw changes redirect thrust promptly.
        double latX = vx - wishX * alongWish;
        double latZ = vz - wishZ * alongWish;
        vx = wishX * alongWish + latX * LATERAL_KEEP;
        vz = wishZ * alongWish + latZ * LATERAL_KEEP;

        // Soft top-speed clamp (previous behaviour capped speedSq at 0.6).
        double speedSq = vx * vx + vz * vz;
        if (speedSq > MAX_SPEED_SQ) {
            double scale = MAX_SPEED / Math.sqrt(speedSq);
            vx *= scale;
            vz *= scale;
        }

        player.setDeltaMovement(vx, motion.y, vz);
    }
}
