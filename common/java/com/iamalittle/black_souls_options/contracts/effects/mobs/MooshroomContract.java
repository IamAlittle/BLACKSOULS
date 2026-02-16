package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 哞菇契约效果 - 其他玩家可以手持碗右键点击契约者获得随机迷之炖菜
 * 玩家契约哞菇后，其他玩家可以手持碗右键点击该玩家获得随机效果的迷之炖菜
 */
public class MooshroomContract extends ContractEffect {
    private static final String EFFECT_ID = "mooshroom_stew_giver";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.mooshroom.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.mooshroom.description";
    
    // 随机数生成器
    private final Random random = new Random();
    
    // 可用的迷之炖菜效果列表（包含正面和负面效果）
    private final MobEffect[] stewEffects = {
        MobEffects.NIGHT_VISION,
        MobEffects.INVISIBILITY,
        MobEffects.JUMP,
        MobEffects.FIRE_RESISTANCE,
        MobEffects.MOVEMENT_SPEED,
        MobEffects.SLOW_FALLING,
        MobEffects.WATER_BREATHING,
        MobEffects.HEAL,
        MobEffects.REGENERATION,
        MobEffects.DAMAGE_BOOST,
        MobEffects.ABSORPTION,
        MobEffects.SATURATION,
        MobEffects.BLINDNESS,
        MobEffects.CONFUSION,
        MobEffects.DIG_SLOWDOWN,
        MobEffects.HUNGER,
        MobEffects.POISON,
        MobEffects.WEAKNESS,
        MobEffects.WITHER,
        MobEffects.LEVITATION,
        MobEffects.DARKNESS,
        MobEffects.GLOWING,
        MobEffects.UNLUCK,
        MobEffects.BAD_OMEN
    };
    
    public MooshroomContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null && sendMessage) {
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendActivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            // 停用效果时发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        // 哞菇契约不需要tick更新，只在右键交互时触发
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.mooshroom.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.mooshroom.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.mooshroom.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.mooshroom.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#FFAA00"))));
        details.add(Component.translatable("black_souls_options.contracts.mooshroom.warning")
                .withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        return details;
    }
    
    /**
     * 检查玩家是否拥有哞菇契约
     */
    public static boolean hasMooshroomContract(Player player) {
        if (player == null) return false;
        
        // 检查玩家是否拥有哞菇契约效果
        com.iamalittle.black_souls_options.contracts.ContractManager manager = 
            com.iamalittle.black_souls_options.contracts.ContractManagerHelper.getAppropriateContractManager(player);
        
        if (manager != null) {
            return manager.getAllContracts().stream()
                .anyMatch(contract -> "minecraft:mooshroom".equals(contract.getEntityType()) && 
                    contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
        }
        
        return false;
    }
    
    /**
     * 处理玩家右键点击其他玩家的交互
     * @param player 执行右键点击的玩家
     * @param target 被右键点击的玩家
     * @param hand 交互的手
     * @return 交互结果
     */
    public static InteractionResult handlePlayerRightClick(Player player, Entity target, InteractionHand hand) {
        if (player == null || target == null || !(target instanceof Player)) {
            return InteractionResult.PASS;
        }
        
        // 检查被点击的玩家是否拥有哞菇契约（目标玩家是契约者）
        if (!hasMooshroomContract((Player) target)) {
            return InteractionResult.PASS;
        }
        
        // 检查玩家是否手持碗
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.getItem() != Items.BOWL) {
            return InteractionResult.PASS;
        }
        
        // 检查被点击的玩家是否是自己
        if (player == target) {
            player.displayClientMessage(Component.literal("§c不能对自己使用！"), true);
            return InteractionResult.FAIL;
        }
        
        // 检查玩家是否有足够的碗（至少1个）
        if (itemInHand.getCount() < 1) {
            player.displayClientMessage(Component.literal("§c需要至少一个碗！"), true);
            return InteractionResult.FAIL;
        }
        
        // 生成随机迷之炖菜
        ItemStack suspiciousStew = createRandomSuspiciousStew();
        
        // 消耗一个碗
        itemInHand.shrink(1);
        
        // 给予玩家迷之炖菜
        if (!player.getInventory().add(suspiciousStew)) {
            // 如果背包满了，掉落在地上
            player.drop(suspiciousStew, false);
        }
        
        // 播放音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.MOOSHROOM_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 发送消息给被点击的玩家（契约者）
        ((Player) target).displayClientMessage(Component.literal("§6有人从你身上获取了迷之炖菜！"), true);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 创建随机效果的迷之炖菜
     */
    private static ItemStack createRandomSuspiciousStew() {
        Random random = new Random();
        MobEffect[] stewEffects = {
            MobEffects.NIGHT_VISION,
            MobEffects.INVISIBILITY,
            MobEffects.JUMP,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.SLOW_FALLING,
            MobEffects.WATER_BREATHING,
            MobEffects.HEAL,
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_BOOST,
            MobEffects.ABSORPTION,
            MobEffects.SATURATION,
            MobEffects.BLINDNESS,
            MobEffects.CONFUSION,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.HUNGER,
            MobEffects.POISON,
            MobEffects.WEAKNESS,
            MobEffects.WITHER,
            MobEffects.LEVITATION,
            MobEffects.DARKNESS,
            MobEffects.GLOWING,
            MobEffects.UNLUCK,
            MobEffects.BAD_OMEN
        };
        
        // 随机选择一个效果
        MobEffect randomEffect = stewEffects[random.nextInt(stewEffects.length)];
        
        // 创建迷之炖菜
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        
        // 设置效果（持续时间5-15秒）
        int duration = 100 + random.nextInt(200); // 5-15秒（20 ticks/秒）
        SuspiciousStewItem.saveMobEffect(stew, randomEffect, duration);
        
        return stew;
    }
}