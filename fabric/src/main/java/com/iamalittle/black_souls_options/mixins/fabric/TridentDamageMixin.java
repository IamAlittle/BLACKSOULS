package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.DrownedContract;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle trident damage for Drowned contract slowness effect
 * This mixin intercepts trident damage to apply slowness effect when player has Drowned contract
 */
@Mixin(ThrownTrident.class)
public class TridentDamageMixin {
    
    /**
     * Intercept the onHitEntity method to apply slowness effect when trident hits an entity
     * This method is called when a thrown trident hits an entity
     */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void onTridentHitEntity(net.minecraft.world.phys.EntityHitResult entityHitResult, CallbackInfo ci) {
        ThrownTrident trident = (ThrownTrident) (Object) this;
        
        // 获取投掷者（玩家）
        Entity owner = trident.getOwner();
        if (owner instanceof Player player) {
            // 获取被击中的目标
            Entity target = entityHitResult.getEntity();
            
            // 应用溺尸契约的减速效果
            DrownedContract.applySlownessToTarget(target, player);
        }
    }
}