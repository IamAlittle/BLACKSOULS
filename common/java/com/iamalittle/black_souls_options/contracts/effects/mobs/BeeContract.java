package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;

import java.util.*;

/**
 * 蜜蜂契约效果 - 花粉传播者
 * 玩家契约蜜蜂后，携带花朵时产生花粉粒子，并为周围作物授粉
 */
public class BeeContract extends ContractEffect {
    private static final String EFFECT_ID = "bee_pollen_spreader";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.bee.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.bee.description";
    
    // 花朵物品标签
    private static final TagKey<Item> FLOWER_ITEMS = ItemTags.FLOWERS;
    
    // 可授粉的作物列表（使用作物标签）
    private static final TagKey<Block> POLLINATABLE_CROPS = BlockTags.CROPS;
    
    // 花粉粒子效果冷却时间（毫秒）
    private static final long POLLEN_PARTICLE_COOLDOWN = 200L;
    // 授粉冷却时间（毫秒）
    private static final long POLLINATION_COOLDOWN = 3000L;
    
    private long lastPollenParticleTime = 0;
    private long lastPollinationTime = 0;
    private static final Set<UUID> beeContractPlayers = new HashSet<>();
    
    public BeeContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            beeContractPlayers.add(player.getUUID());
            
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
            beeContractPlayers.remove(player.getUUID());
            
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
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // 检查玩家是否携带花朵
        if (hasFlowerInInventory(player)) {
            // 产生花粉粒子效果
            if (currentTime - lastPollenParticleTime >= POLLEN_PARTICLE_COOLDOWN) {
                spawnPollenParticles(player);
                lastPollenParticleTime = currentTime;
            }
            
            // 为周围作物授粉（冷却时间控制）
            if (currentTime - lastPollinationTime >= POLLINATION_COOLDOWN) {
                pollinateNearbyCrops(player);
                lastPollinationTime = currentTime;
            }
        }
    }
    
    /**
     * 检查玩家物品栏中是否有花朵（使用物品标签）
     */
    private boolean hasFlowerInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var itemStack = player.getInventory().getItem(i);
            if (!itemStack.isEmpty() && itemStack.is(FLOWER_ITEMS)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 在玩家周围生成花粉粒子
     */
    private void spawnPollenParticles(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 在玩家周围随机位置生成花粉粒子
            for (int i = 0; i < 3; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetY = player.getRandom().nextDouble() * 1.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 2.0;
                
                double x = player.getX() + offsetX;
                double y = player.getY() + offsetY;
                double z = player.getZ() + offsetZ;
                
                serverLevel.sendParticles(ParticleTypes.FALLING_NECTAR,
                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * 为玩家周围的作物授粉
     */
    private void pollinateNearbyCrops(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        
        BlockPos playerPos = player.blockPosition();
        int radius = 1; // 授粉范围半径（缩小到1格）
        int pollinatedCount = 0;
        
        // 搜索玩家周围的作物
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos cropPos = playerPos.offset(x, y, z);
                    BlockState blockState = serverLevel.getBlockState(cropPos);
                    
                    // 检查是否为可授粉的作物
                    if (isPollinatableCrop(blockState) && player.getRandom().nextFloat() < 0.3f) {
                        // 30% 几率授粉成功
                        if (accelerateCropGrowth(serverLevel, cropPos, blockState)) {
                            pollinatedCount++;
                            
                            // 显示授粉粒子效果
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5,
                                3, 0.3, 0.3, 0.3, 0.1);
                        }
                        
                        // 限制每次最多授粉3个作物
                        if (pollinatedCount >= 3) {
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 检查方块是否为可授粉的作物（使用花朵标签）
     */
    private boolean isPollinatableCrop(BlockState blockState) {
        return blockState.is(POLLINATABLE_CROPS);
    }
    
    /**
     * 加速作物生长（类似骨粉效果）
     */
    private boolean accelerateCropGrowth(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        
        if (block instanceof CropBlock crop) {
            // 对于农作物（小麦、胡萝卜、马铃薯、甜菜根）
            if (crop.isMaxAge(state)) {
                return false; // 已经成熟，不需要授粉
            }
            
            // 随机生长1-2个阶段
            int currentAge = state.getValue(CropBlock.AGE);
            int growthAmount = level.random.nextInt(2) + 1;
            int newAge = Math.min(crop.getMaxAge(), currentAge + growthAmount);
            
            if (newAge > currentAge) {
                BlockState newState = state.setValue(CropBlock.AGE, newAge);
                level.setBlock(pos, newState, 3);
                return true;
            }
        } else if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
            // 对于瓜类和南瓜茎
            int currentAge = state.getValue(net.minecraft.world.level.block.StemBlock.AGE);
            
            if (currentAge < 7) { // 最大年龄为7
                int growthAmount = level.random.nextInt(2) + 1;
                int newAge = Math.min(7, currentAge + growthAmount);
                
                if (newAge > currentAge) {
                    BlockState newState = state.setValue(net.minecraft.world.level.block.StemBlock.AGE, newAge);
                    level.setBlock(pos, newState, 3);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查玩家是否拥有蜜蜂契约效果
     */
    public static boolean hasBeeContract(Player player) {
        return beeContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.bee.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.bee.pollen_spreader").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.bee.pollen_particles").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.bee.pollination_range").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.bee.bone_meal_effect").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.bee.requires_flowers").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putLong("lastPollenParticleTime", lastPollenParticleTime);
        nbt.putLong("lastPollinationTime", lastPollinationTime);
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("lastPollenParticleTime")) {
            lastPollenParticleTime = nbt.getLong("lastPollenParticleTime");
        }
        if (nbt.contains("lastPollinationTime")) {
            lastPollinationTime = nbt.getLong("lastPollinationTime");
        }
    }
}