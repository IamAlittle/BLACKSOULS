package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.FishContract;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player mixin to modify swimming speed when fish contracts are active
 * This modifies the underwater movement calculation in Minecraft
 */
@Mixin(Player.class)
public class PlayerSwimSpeedMixin {
    
    /**
     * 修改游泳速度：在travel方法的HEAD位置注入
     * 这样可以确保在移动计算开始前应用速度提升
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = false)
    private void modifySwimSpeed(Vec3 travelVector, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有鱼类契约、在水中且不是飞行状态
        if (FishContract.hasFishContract(player) && player.isInWater() && !player.getAbilities().flying) {
            // 获取契约层数
            int level = FishContract.getFishContractLevel(player);
            if (level > 0) {
                double boostFactor = 1.0d + (level * 0.02666d);
                
                // 修改玩家的移动速度（只影响X和Z轴，不影响Y轴）
                Vec3 currentMovement = player.getDeltaMovement();
                Vec3 boostedMovement = new Vec3(
                    currentMovement.x * boostFactor,
                    currentMovement.y,  // Y轴保持不变
                    currentMovement.z * boostFactor
                );
                player.setDeltaMovement(boostedMovement);
            }
        }
    }
}