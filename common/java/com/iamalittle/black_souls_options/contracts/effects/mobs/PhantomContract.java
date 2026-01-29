package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * 幻翼契约效果 - 自动恢复鞘翅耐久
 * 玩家契约幻翼后获得的能力：
 * 1. 自动恢复身上鞘翅的耐久度
 * 2. 在夜间或黑暗环境中恢复速度更快
 */
public class PhantomContract extends ContractEffect {
    private static final String EFFECT_ID = "phantom_elytra_repair";
    private static final String DISPLAY_NAME = "幻翼";
    private static final String DESCRIPTION = "自动恢复鞘翅耐久度";
    
    // 幻翼契约玩家集合
    private static final Set<UUID> phantomContractPlayers = new HashSet<>();
    
    // 恢复间隔（毫秒）
    private static final long REPAIR_INTERVAL = 10000; // 10秒

    
    // 恢复量
    private static final int REPAIR_AMOUNT = 10;
    
    public PhantomContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            phantomContractPlayers.add(player.getUUID());
            
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
            phantomContractPlayers.remove(player.getUUID());
            
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
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 检查玩家是否拥有幻翼契约
        if (!hasPhantomContract(player)) {
            return;
        }
        
        // 执行鞘翅耐久恢复
        repairElytra(player);
    }

    
    /**
     * 恢复鞘翅耐久
     * @param player 玩家
     * @return 是否成功恢复耐久
     */
    private boolean repairElytra(Player player) {
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        
        // 检查是否为鞘翅
        if (chestItem.getItem() != Items.ELYTRA) {
            return false;
        }
        
        // 检查鞘翅是否已损坏
        if (chestItem.getDamageValue() <= 0) {
            return false;
        }
        
        // 检查玩家经验值是否足够（每点耐久消耗1点经验）
        if (player.totalExperience < REPAIR_AMOUNT) {
            return false;
        }
        
        // 消耗经验值
        player.giveExperiencePoints(-REPAIR_AMOUNT);
        
        // 恢复耐久（固定恢复量，不考虑耐久附魔）
        int newDamage = Math.max(0, chestItem.getDamageValue() - REPAIR_AMOUNT);
        chestItem.setDamageValue(newDamage);
        
        // 更新物品栏
        player.getInventory().setChanged();
        
        return true;
    }
    
    /**
     * 检查玩家是否拥有幻翼契约效果
     */
    public static boolean hasPhantomContract(Player player) {
        return player != null && phantomContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        // 默认返回10秒间隔（10000毫秒）
        return REPAIR_INTERVAL;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6幻翼契约效果："));
        details.add(Component.literal("§7- 自动恢复身上鞘翅的耐久度"));
        details.add(Component.literal("§7- 每10秒恢复1点耐久"));
        details.add(Component.literal("§7- 每次恢复消耗10点经验值"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putLong("lastRepairTime", effectData.getLong("lastRepairTime"));
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        effectData.putLong("lastRepairTime", nbt.getLong("lastRepairTime"));
    }
}