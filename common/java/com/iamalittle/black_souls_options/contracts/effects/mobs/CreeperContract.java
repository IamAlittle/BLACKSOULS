package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.List;

/**
 * 苦力怕契约效果 - 死亡自爆
 * 玩家契约苦力怕后，死亡时会产生自爆效果
 * 自爆不会破坏方块，不会着火
 */
public class CreeperContract extends ContractEffect {
    private static final String EFFECT_ID = "creeper_death_explosion";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.creeper.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.creeper.description";
    
    // 自爆威力（默认与苦力怕相同）
    private float explosionPower = 3.0f;
    
    public CreeperContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
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
        // 不需要每tick执行操作，只在玩家死亡时触发
    }
    
    /**
     * 玩家死亡时触发自爆效果
     * 这个方法需要在玩家死亡事件中调用
     */
    public void onPlayerDeath(Player player) {
        // 关键修复：检查玩家状态，避免在玩家无效状态下执行
        if (player == null || player.level() == null) {
            return;
        }
        
        Level level = player.level();
        Vec3 deathPos = player.position();
        
        // 播放苦力怕爆炸音效
        level.playSound(null, deathPos.x, deathPos.y, deathPos.z, 
            SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0f, 1.0f);
        
        // 创建不破坏方块、不着火的爆炸
        Explosion explosion = new Explosion(level, null, null, null, 
            deathPos.x, deathPos.y, deathPos.z, explosionPower, false, Explosion.BlockInteraction.KEEP);
        
        // 执行爆炸效果
        explosion.explode();
        explosion.finalizeExplosion(true);
        
        // 播放爆炸音效
        level.playSound(null, deathPos.x, deathPos.y, deathPos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f, (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.creeper.effect_title").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.creeper.death_effect").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.creeper.explosion_power", explosionPower).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.creeper.safety_info").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#FF5555"))));
        return details;
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putFloat("explosionPower", explosionPower);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("explosionPower")) {
            explosionPower = nbt.getFloat("explosionPower");
        }
    }
}