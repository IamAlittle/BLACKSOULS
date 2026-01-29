package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.tags.FluidTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 鱼类契约效果 - 水下速度提升
 * 玩家与鱼类（鲑鱼、鳕鱼、热带鱼）建立契约后获得的能力：
 * 1. 在水中游泳速度提升，三种鱼的契约可以叠加
 */
public class FishContract extends ContractEffect {
    private static final String EFFECT_ID = "fish_swim_boost";
    private static final String DISPLAY_NAME = "我是一条鱼";
    private static final String DESCRIPTION = "水中游泳速度提升，契约鳕鱼、鲑鱼、热带鱼可叠加";
    
    // 玩家契约鱼类数量映射表
    private static final Map<UUID, Integer> fishContractCountMap = new HashMap<>();
    
    // 当前玩家引用，用于显示详细信息
    private Player currentPlayer;
    
    public FishContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            UUID playerUUID = player.getUUID();
            
            // 增加玩家的鱼类契约计数
            int count = fishContractCountMap.getOrDefault(playerUUID, 0);
            fishContractCountMap.put(playerUUID, count + 1);
            
            // 设置当前玩家引用
            this.currentPlayer = player;
            
            // 游泳速度提升现在由Mixin处理
            
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
            UUID playerUUID = player.getUUID();
            
            // 减少玩家的鱼类契约计数
            int count = fishContractCountMap.getOrDefault(playerUUID, 0);
            if (count > 0) {
                count--;
                if (count > 0) {
                    fishContractCountMap.put(playerUUID, count);
                } else {
                    fishContractCountMap.remove(playerUUID);
                }
                
                // 游泳速度提升现在由Mixin处理
            }
            
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
    }
    
    /**
     * 检查玩家是否拥有鱼类契约效果
     */
    public static boolean hasFishContract(Player player) {
        if (player == null) {
            return false;
        }
        
        // 在客户端，需要检查是否有对应的契约数据
        if (player.level() != null && player.level().isClientSide()) {
            // 客户端检查：通过契约管理器来验证是否有鱼类契约
            ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
            if (manager != null) {
                return manager.getAllContracts().stream()
                    .anyMatch(contract -> {
                        String entityType = contract.getEntityType();
                        return "minecraft:salmon".equals(entityType) || 
                               "minecraft:cod".equals(entityType) || 
                               "minecraft:tropical_fish".equals(entityType);
                    });
            }
            return false;
        }
        
        // 服务器端：使用本地计数
        return fishContractCountMap.containsKey(player.getUUID());
    }
    
    /**
     * 获取玩家的鱼类契约层数
     */
    public static int getFishContractLevel(Player player) {
        if (player == null) {
            return 0;
        }
        
        // 在客户端，需要从契约管理器获取实际的鱼类契约数量
        if (player.level() != null && player.level().isClientSide()) {
            ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
            if (manager != null) {
                return (int) manager.getAllContracts().stream()
                    .filter(contract -> {
                        String entityType = contract.getEntityType();
                        return "minecraft:salmon".equals(entityType) || 
                               "minecraft:cod".equals(entityType) || 
                               "minecraft:tropical_fish".equals(entityType);
                    })
                    .count();
            }
            return 0;
        }
        
        // 服务器端：使用本地计数
        return fishContractCountMap.getOrDefault(player.getUUID(), 0);
    }
    
    /**
     * 重置玩家的鱼类契约计数（用于网络同步）
     */
    public static void resetPlayerFishContractCount(Player player) {
        if (player != null) {
            fishContractCountMap.remove(player.getUUID());
        }
    }

    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        
        details.add(Component.literal("§7水中游泳速度提升"));
        details.add(Component.literal("§7鱼类契约效果可叠加3层"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        return super.saveToNBT();
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
    }
}