package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.common.Events;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import java.util.*;

/**
 * 青蛙契约效果
 */
public class FrogContract extends ContractEffect {
    private static final String EFFECT_ID = "frog_magma_cube_drops";
    private static final String DISPLAY_NAME = "呱";
    private static final String DESCRIPTION = "击杀小岩浆怪会随机掉落蛙鸣灯";
    
    // 青蛙契约玩家集合
    private static final Set<UUID> frogContractPlayers = new HashSet<>();
    
    // 掉落概率（100%概率）
    private static final float DROP_CHANCE = 1.0f;
    
    // 是否已注册事件监听器
    private static boolean eventListenerRegistered = false;
    
    public FrogContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
        
        // 注册实体死亡事件监听器（只注册一次）
        if (!eventListenerRegistered) {
            Events.EntityDeath.add(this::onEntityDeath);
            eventListenerRegistered = true;
        }
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            frogContractPlayers.add(player.getUUID());
            
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
            frogContractPlayers.remove(player.getUUID());
            
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
        // 青蛙契约不需要每tick处理
    }
    
    /**
     * 处理实体死亡事件
     */
    private void onEntityDeath(Events.EntityDeathEvent event) {
        Entity entity = event.getEntity();
        net.minecraft.world.damagesource.DamageSource damageSource = event.getDamageSource();
        
        // 检查是否为小岩浆怪（尺寸为1）
        if (entity instanceof MagmaCube && ((MagmaCube) entity).getSize() == 1) {
            // 获取击杀者（通过伤害来源）
            Entity killer = damageSource != null ? damageSource.getEntity() : null;
            
            if (killer instanceof Player) {
                Player player = (Player) killer;
                
                // 检查玩家是否拥有青蛙契约效果
                if (hasFrogContract(player)) {
                    // 随机决定是否掉落蛙鸣灯
                    if (player.getRandom().nextFloat() < DROP_CHANCE) {
                        dropFroglight(player, entity);
                    }
                }
            }
        }
    }
    
    /**
     * 掉落蛙鸣灯
     */
    private void dropFroglight(Player player, Entity magmaCube) {
        Level level = magmaCube.level();
        BlockPos pos = magmaCube.blockPosition();
        
        // 随机选择一种蛙鸣灯
        net.minecraft.world.item.Item froglightItem;
        
        int randomType = player.getRandom().nextInt(3);
        switch (randomType) {
            case 0:
                froglightItem = Items.OCHRE_FROGLIGHT;
                break;
            case 1:
                froglightItem = Items.PEARLESCENT_FROGLIGHT;
                break;
            case 2:
            default:
                froglightItem = Items.VERDANT_FROGLIGHT;
                break;
        }
        
        // 创建随机蛙鸣灯物品堆栈（1个）
        ItemStack froglightStack = new ItemStack(froglightItem, 1);
        
        // 在岩浆怪位置掉落蛙鸣灯
        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, froglightStack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        

    }
    
    /**
     * 检查玩家是否拥有青蛙契约效果
     */
    public static boolean hasFrogContract(Player player) {
        return player != null && frogContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 1000;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6青蛙契约效果："));
        details.add(Component.literal("§7- 击杀小岩浆怪必定掉落随机蛙鸣灯"));
        return details;
    }
}