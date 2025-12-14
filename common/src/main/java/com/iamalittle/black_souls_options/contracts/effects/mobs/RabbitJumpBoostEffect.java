package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;

public class RabbitJumpBoostEffect extends ContractEffect {
    
    private float jumpBoostMultiplier;
    
    public RabbitJumpBoostEffect(String id, String name, String description) {
        super(id, name, description);
        this.jumpBoostMultiplier = 1.5f; // 默认跳跃提升倍数
    }
    
    @Override
    public void onActivate(Player player) {
        // 激活效果时的逻辑
        System.out.println("Rabbit jump boost effect activated for player: " + player.getName().getString());
    }
    
    @Override
    public void onDeactivate(Player player) {
        // 停用效果时的逻辑
        System.out.println("Rabbit jump boost effect deactivated for player: " + player.getName().getString());
    }
    
    @Override
    public void onTick(Player player) {
        // 每tick更新的逻辑
        if (player != null && player.isAlive()) {
            applyJumpBoost(player);
        }
    }
    
    private void applyJumpBoost(Player player) {
        // 应用跳跃提升效果
        if (player.getDeltaMovement().y > 0) {
            // 当玩家正在向上跳跃时应用提升
            double currentVerticalSpeed = player.getDeltaMovement().y;
            double baseJumpSpeed = 0.42; // Minecraft默认跳跃速度
            
            if (currentVerticalSpeed < baseJumpSpeed * jumpBoostMultiplier) {
                // 增加跳跃速度，但不超过最大限制
                double newVerticalSpeed = Math.min(currentVerticalSpeed * 1.2, baseJumpSpeed * jumpBoostMultiplier);
                player.setDeltaMovement(player.getDeltaMovement().x, newVerticalSpeed, player.getDeltaMovement().z);
            }
        }
    }
    
    public void setJumpBoostMultiplier(float multiplier) {
        this.jumpBoostMultiplier = Math.max(1.0f, Math.min(3.0f, multiplier)); // 限制在1.0-3.0之间
    }
    
    public float getJumpBoostMultiplier() {
        return jumpBoostMultiplier;
    }
}