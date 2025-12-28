package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingEntity mixin to modify visibility percent when feigning death
 * This makes the player less visible to hostile mobs, similar to invisibility effect
 */
@Mixin(LivingEntity.class)
public class PlayerVisibilityMixin {
    
    /**
     * Inject into getVisibilityPercent method to reduce visibility when feigning death
     * This is similar to how invisibility effect works in Minecraft
     */
    @Inject(method = "getVisibilityPercent", at = @At("HEAD"), cancellable = true)
    private void modifyVisibilityPercent( CallbackInfoReturnable<Double> cir) {
        // Get the actual LivingEntity instance being mixed into
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only apply to Player entities
        if (entity instanceof Player) {
            Player player = (Player) entity;
            
            // Check if the player is currently feigning death
            if (AxolotlContract.isPlayerFeigningDeath(player)) {
                // 大幅降低可见度，让敌对生物忽略玩家
                cir.setReturnValue(0.1d); // 10%可见度
            }
        }
    }
}