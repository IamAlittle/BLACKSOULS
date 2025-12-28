package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.FoxContract;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player mixin to provide immunity to sweet berry bush effects when fox contract is active
 * This modifies the berry bush interaction and damage mechanics
 */
@Mixin(Player.class)
public class PlayerBerryBushMixin {
    
    /**
     * 修改浆果丛对玩家的减速效果
     * 当狐狸契约激活时，玩家在浆果丛中不会减速
     */
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = false)
    private void modifyBerryBushSlowdown(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有狐狸契约
        if (FoxContract.hasFoxContract(player)) {
            Level level = player.level();
            BlockPos playerPos = player.blockPosition();
            BlockState blockState = level.getBlockState(playerPos);
            
            // 检查玩家是否站在浆果丛中
            if (blockState.getBlock() instanceof SweetBerryBushBlock) {
                // 取消浆果丛的减速效果
                // 这里不需要做任何操作，因为我们会通过其他方式阻止减速
            }
        }
    }
    
    /**
     * 修改浆果丛对玩家的伤害效果
     * 当狐狸契约激活时，玩家在浆果丛中不会受到伤害
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void preventBerryBushDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有狐狸契约
        if (FoxContract.hasFoxContract(player)) {
            // 检查伤害来源是否为浆果丛
            if (source == player.damageSources().sweetBerryBush()) {
                // 取消浆果丛伤害
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
    
    /**
     * 修改实体在浆果丛中的移动速度
     * 当狐狸契约激活时，玩家在浆果丛中不会减速
     */
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void preventBerryBushSlowdown(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否拥有狐狸契约且方块是浆果丛
        if (FoxContract.hasFoxContract(player) && state.getBlock() instanceof SweetBerryBushBlock) {
            // 取消浆果丛的减速效果
            ci.cancel();
        }
    }
}