package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 蜘蛛契约效果 - 爬墙能力
 * 玩家契约蜘蛛后获得的能力：
 * 1. 可以像蜘蛛一样在墙壁上攀爬
 */
public class SpiderContract extends ContractEffect {
    private static final String EFFECT_ID = "spider_climbing";
    private static final String DISPLAY_NAME = "爬墙";
    private static final String DESCRIPTION = "可以像蜘蛛一样在墙壁上攀爬";
    
    // 蜘蛛契约玩家集合
    private static final Set<UUID> spiderContractPlayers = new HashSet<>();
    
    public SpiderContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            spiderContractPlayers.add(player.getUUID());
            
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
            spiderContractPlayers.remove(player.getUUID());
            
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
        // 空实现，因为我们使用客户端的playerTick方法
    }
    
    @Override
    public void playerTick(Minecraft MC, Player player) {
        if(player==null) return;
        if(!player.horizontalCollision) return;
        Vec3 vec3 = player.getViewVector(0);
        if(vec3.y>=0.2) return;
        player.setDeltaMovement(0,0.2,0);
    }
    
    /**
     * 检查玩家是否拥有蜘蛛契约效果
     */
    public static boolean hasSpiderContract(Player player) {
        return player != null && spiderContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 20; // 基础检测间隔
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6蜘蛛契约效果："));
        details.add(Component.literal("§7- 可以在墙壁上攀爬"));
        details.add(Component.literal("§7- 当贴在墙壁上且视线向下时会可向上攀爬"));
        return details;
    }
}