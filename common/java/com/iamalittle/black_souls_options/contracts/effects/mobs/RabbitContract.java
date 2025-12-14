package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 兔子契约效果 - 跳跃提升
 * 玩家契约兔子后可以获得跳跃提升效果
 */
public class RabbitContract extends ContractEffect {
    private static final String EFFECT_ID = "rabbit_jump_boost";
    private static final String DISPLAY_NAME = "兔子跳跃";
    private static final String DESCRIPTION = "跳得更高";
    
    // 跳跃提升等级
    private int jumpBoostLevel = 2;
    
    public RabbitContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player) {
        if (player != null) {
            // 激活时立即应用跳跃提升效果
            applyJumpBoost(player);
            sendActivationMessage(player);
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            // 停用效果时移除跳跃提升
            removeJumpBoost(player);
            sendDeactivationMessage(player);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        if (player != null) {
            // 每tick检查效果是否存在，如果不存在则重新应用
            if (!player.hasEffect(MobEffects.JUMP)) {
                applyJumpBoost(player);
            }
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b跳跃提升效果："));
        details.add(Component.literal("§7获得" + jumpBoostLevel + "级跳跃提升效果"));
        details.add(Component.literal("§7让你能够跳得更高更远"));
        return details;
    }
    
    /**
     * 应用跳跃提升效果
     */
    private void applyJumpBoost(Player player) {
        // 创建一个跳跃提升效果实例，等级为jumpBoostLevel，持续时间-1（无限时间），无粒子效果
        MobEffectInstance jumpBoost = new MobEffectInstance(
            MobEffects.JUMP,  // 跳跃提升效果
            -1,               // 持续时间（tick）-1表示无限时间
            jumpBoostLevel - 1,  // 等级（注意：Minecraft的效果等级是从0开始的）
            false,            // 是否有粒子效果
            false,            // 是否显示在 HUD
            false             // 是否可以被移除
        );
        
        // 应用效果到玩家
        player.addEffect(jumpBoost);
    }
    
    /**
     * 移除跳跃提升效果
     */
    private void removeJumpBoost(Player player) {
        // 移除玩家身上的跳跃提升效果
        player.removeEffect(MobEffects.JUMP);
    }
    
    /**
     * 重写tick间隔，设置为500毫秒确保效果持续存在
     */
    @Override
    protected long getTickInterval() {
        return 500; // 500毫秒检查一次效果是否存在
    }
    
    /**
     * 获取跳跃提升等级
     */
    public int getJumpBoostLevel() {
        return jumpBoostLevel;
    }
    
    /**
     * 设置跳跃提升等级
     */
    public void setJumpBoostLevel(int level) {
        this.jumpBoostLevel = Math.max(1, Math.min(10, level)); // 限制在1-10级之间
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putInt("jumpBoostLevel", jumpBoostLevel);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("jumpBoostLevel")) {
            jumpBoostLevel = nbt.getInt("jumpBoostLevel");
        }
    }
    

}