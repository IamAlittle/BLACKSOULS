package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.effects.DeathTotemEffect;
import com.iamalittle.black_souls_options.effects.DeathTotemDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 唤魔者契约效果 - 不死图腾
 * 玩家契约唤魔者后可以获得不死图腾效果
 */
public class EvokerContract extends ContractEffect {
    private static final String EFFECT_ID = "evoker_death_totem";
    private static final String DISPLAY_NAME = "唤魔者不死图腾";
    private static final String DESCRIPTION = "死亡时触发不死图腾效果";
    
    public EvokerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 激活时获取死亡图腾数据，但不重置冷却时间
            // 注意：这里只是获取数据，不会重置CD状态
            DeathTotemDataManager.getInstance().getPlayerData(player);
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
            // 停用效果时只标记契约状态，不清理死亡图腾数据
            // 这样重新激活契约时CD状态不会丢失
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
        // 死亡图腾效果不需要每tick更新，由DeathTotemEffect.tick()统一处理
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b不死图腾效果："));
        details.add(Component.literal("§7死亡时触发不死图腾效果"));
        return details;
    }
    
    /**
     * 检查是否可以触发不死图腾效果
     */
    public static boolean canTriggerTotem(Player player) {
        return DeathTotemEffect.canTriggerTotem(player);
    }
    
    /**
     * 触发不死图腾效果
     */
    public static void triggerTotemEffect(Player player) {
        DeathTotemEffect.triggerTotemEffect(player);
    }

    
    /**
     * 获取玩家剩余的冷却时间（秒）
     */
    public static float getRemainingCooldownSeconds(Player player) {
        return DeathTotemEffect.getRemainingCooldownSeconds(player);
    }
    
    /**
     * 获取玩家剩余的冷却时间（秒，整数版本）
     */
    public static int getRemainingCooldownSecondsInt(Player player) {
        return DeathTotemEffect.getRemainingCooldownSecondsInt(player);
    }
    
    /**
     * 检查玩家是否在冷却中
     */
    public static boolean isOnCooldown(Player player) {
        return DeathTotemEffect.isOnCooldown(player);
    }
}