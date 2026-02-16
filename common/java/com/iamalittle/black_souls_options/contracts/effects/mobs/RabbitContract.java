package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import java.util.ArrayList;
import java.util.List;

/**
 * 兔子契约效果 - 跳跃提升
 * 玩家契约兔子后可以获得跳跃提升效果
 */
public class RabbitContract extends ContractEffect {
    private static final String EFFECT_ID = "rabbit_jump_boost";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.rabbit.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.rabbit.description";
    
    // 跳跃提升等级
    private int jumpBoostLevel = 2;
    
    public RabbitContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 激活时立即应用跳跃提升效果
            applyJumpBoost(player);
            // 使用契约目标名称发送消息（仅在需要时发送）
            if (sendMessage) {
                String entityName = effectData.getString("contractEntityName");
                if (entityName.isEmpty()) {
                    entityName = displayName; // 回退到效果名称
                }
                sendActivationMessage(player, entityName);
            }
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            // 停用效果时移除跳跃提升
            removeJumpBoost(player);
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下执行
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }

            applyJumpBoost(player);

    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.rabbit.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.rabbit.effect1", jumpBoostLevel)
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.rabbit.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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