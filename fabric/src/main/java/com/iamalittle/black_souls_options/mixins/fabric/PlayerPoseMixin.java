package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player mixin to change player pose when feigning death
 * Makes the player lie down (sleep pose) when feigning death
 */
@Mixin(Player.class)
public class PlayerPoseMixin {
    
    /**
     * Inject into updatePlayerPose method to set sleeping pose when feigning death
     */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void modifyPlayerPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        

    }
}