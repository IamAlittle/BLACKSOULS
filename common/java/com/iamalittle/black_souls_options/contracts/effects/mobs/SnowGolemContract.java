package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.contracts.ContractDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 雪傀儡契约效果 - 发射雪球能力
 * 玩家契约雪傀儡后获得的能力：
 * 1. 可以像雪傀儡一样发射雪球攻击敌人
 * 2. 雪球攻击会造成伤害和击退效果
 */
public class SnowGolemContract extends ContractEffect {
    private static final String EFFECT_ID = "snow_golem_snowball_attack";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.snow_golem.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.snow_golem.description";
    
    // 雪傀儡契约玩家集合
    private static final Set<UUID> snowGolemContractPlayers = new HashSet<>();
    
    public SnowGolemContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            snowGolemContractPlayers.add(player.getUUID());
            
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
            snowGolemContractPlayers.remove(player.getUUID());
            
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
        // 雪傀儡契约玩家在移动时在脚底生成雪片
        if (player != null && hasSnowGolemContract(player)) {
            // 只在主世界中生成雪片
            if (!player.level().isClientSide() && player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                // 参考原版雪傀儡的雪片生成逻辑
                BlockState blockstate = Blocks.SNOW.defaultBlockState();
                
                // 在玩家周围4个位置尝试放置雪片
                for(int i = 0; i < 4; ++i) {
                    // 计算雪片位置（玩家周围0.25格距离）
                    int j = Mth.floor(player.getX() + (double)((float)(i % 2 * 2 - 1) * 0.25F));
                    int k = Mth.floor(player.getY());
                    int l = Mth.floor(player.getZ() + (double)((float)(i / 2 % 2 * 2 - 1) * 0.25F));
                    BlockPos blockpos = new BlockPos(j, k, l);
                    
                    // 检查位置是否可以放置雪片
                    if (player.level().isEmptyBlock(blockpos) && blockstate.canSurvive(player.level(), blockpos)) {
                        // 设置雪片方块并触发游戏事件
                        player.level().setBlockAndUpdate(blockpos, blockstate);
                        player.level().gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate));
                    }
                }
            }
        }
    }
    
    /**
     * 执行雪球攻击
     * @param player 玩家
     * @return 是否成功发射雪球
     */
    public static boolean performSnowballAttack(Player player) {
        if (player == null || !hasSnowGolemContract(player)) {
            return false;
        }
        
        // 检查玩家是否在生存模式
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        
        // 执行雪球攻击
        if (throwSnowballAtTarget(player)) {
            // 播放雪球发射音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 向目标方向发射雪球
     * @param player 玩家
     * @return 是否成功发射雪球
     */
    private static boolean throwSnowballAtTarget(Player player) {
        // 获取玩家视线方向
        Vec3 lookVec = player.getLookAngle();
        
        // 计算雪球的起始位置（玩家头部位置，稍微向前偏移）
        double startX = player.getX() + lookVec.x * 0.3;
        double startY = player.getY() + player.getEyeHeight() - 0.1; // 稍微降低高度
        double startZ = player.getZ() + lookVec.z * 0.3;
        
        // 创建雪球实体
        Snowball snowball = new Snowball(EntityType.SNOWBALL, player.level());
        snowball.setOwner(player);
        snowball.setPos(startX, startY, startZ);
        
        // 设置雪球速度和方向
        double speed = 1.5; // 雪球速度
        snowball.shoot(lookVec.x, lookVec.y, lookVec.z, (float) speed, 1.0f);
        
        // 添加到世界
        player.level().addFreshEntity(snowball);
        
        return true;
    }
    
    /**
     * 检查玩家是否拥有雪傀儡契约效果
     */
    public static boolean hasSnowGolemContract(Player player) {
        return player != null && snowGolemContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 100; // 增加检测频率以实现更流畅的雪片生成
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.snow_golem.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.snow_golem.effect1").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.snow_golem.effect2").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.snow_golem.effect3").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}