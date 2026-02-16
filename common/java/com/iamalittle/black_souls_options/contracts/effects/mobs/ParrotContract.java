package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.TextColor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鹦鹉契约效果 - 随机模仿音效
 * 玩家契约鹦鹉后获得的能力：
 * 1. 按下通用按键（R键）时随机播放已注册的任意音效
 * 2. 可以模仿原版和模组的各种音效
 */
public class ParrotContract extends ContractEffect {
    private static final String EFFECT_ID = "parrot_random_sound";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.parrot.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.parrot.description";
    
    // 鹦鹉契约玩家集合
    private static final Set<UUID> parrotContractPlayers = new HashSet<>();
    
    // 音效缓存，避免频繁获取注册表
    private static List<SoundEvent> cachedSounds = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存
    
    public ParrotContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            parrotContractPlayers.add(player.getUUID());
            
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
            parrotContractPlayers.remove(player.getUUID());
            
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
        // 鹦鹉契约不需要每tick更新，音效播放由按键触发
    }
    
    /**
     * 获取所有已注册的音效（原生+模组），排除音乐类音效
     */
    public static List<SoundEvent> getAllSoundsFromRegistry() {
        // 检查缓存是否有效
        long currentTime = System.currentTimeMillis();
        if (cachedSounds != null && currentTime - lastCacheTime < CACHE_DURATION) {
            return new ArrayList<>(cachedSounds);
        }
        
        try {
            // 使用BuiltInRegistries获取音效注册表
            Registry<SoundEvent> soundRegistry = BuiltInRegistries.SOUND_EVENT;
            
            // 转为List集合，方便遍历，并排除音乐类音效
            List<SoundEvent> sounds = soundRegistry.stream()
                .filter(Objects::nonNull)
                .filter(sound -> sound != null && sound.getLocation() != null)
                .filter(sound -> !isMusicSound(sound)) // 排除音乐类音效
                .collect(Collectors.toList());
            
            // 更新缓存
            cachedSounds = new ArrayList<>(sounds);
            lastCacheTime = currentTime;
            
            return sounds;
        } catch (Exception e) {
            BlackSoulsConfig.error("获取音效注册表时出错：" + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 检查音效是否为音乐类音效
     */
    private static boolean isMusicSound(SoundEvent sound) {
        if (sound == null || sound.getLocation() == null) {
            return false;
        }
        
        String soundPath = sound.getLocation().getPath();
        
        // 排除音乐相关的音效路径
        return soundPath.contains("music") || 
               soundPath.contains("record") ||
               soundPath.contains("disc") ||
               soundPath.contains("menu") ||
               soundPath.contains("creative") ||
               soundPath.contains("credits") ||
               soundPath.contains("end") ||
               soundPath.contains("game") ||
               soundPath.contains("under_water") ||
               soundPath.contains("dragon");
    }
    
    /**
     * 执行随机音效播放
     * @param player 玩家
     * @return 是否成功播放音效
     */
    public static boolean performRandomSound(Player player) {
        if (player == null || !hasParrotContract(player)) {
            return false;
        }
        
        // 检查玩家是否在生存模式
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        
        // 获取所有音效
        List<SoundEvent> allSounds = getAllSoundsFromRegistry();
        if (allSounds.isEmpty()) {
            return false;
        }
        
        // 随机选择一个音效
        Random random = new Random();
        SoundEvent randomSound = allSounds.get(random.nextInt(allSounds.size()));
        
        // 播放随机音效
        if (randomSound != null) {
            // 使用PLAYERS音源，音量0.5，随机音调
            float pitch = 0.8f + random.nextFloat() * 0.4f; // 0.8-1.2随机音调
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                randomSound, SoundSource.PLAYERS, 0.5f, pitch);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查玩家是否拥有鹦鹉契约效果
     */
    public static boolean hasParrotContract(Player player) {
        return player != null && parrotContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 1000; // 减少检测频率，音效播放由按键触发
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.parrot.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.parrot.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.parrot.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.parrot.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}