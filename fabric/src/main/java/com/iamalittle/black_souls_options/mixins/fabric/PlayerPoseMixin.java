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
        
        // 检查玩家是否正在装死
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 设置玩家姿势为睡觉（躺下）
            player.setPose(Pose.SLEEPING);
            
            // 取消原方法的执行，避免姿势被重置
            ci.cancel();
        }
    }
}