package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player mixin to modify canBeSeenAsEnemy method
 * This makes hostile mobs ignore the player when feigning death
 */
@Mixin(Player.class)
public class PlayerCanBeSeenAsEnemyMixin {
    
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void modifyCanBeSeenAsEnemy(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        

    }
}