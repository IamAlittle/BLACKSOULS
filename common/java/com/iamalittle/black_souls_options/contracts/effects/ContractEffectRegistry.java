package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.RabbitContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AllayContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;

import java.util.*;

/**
 * 契约效果注册管理器，用于管理和注册契约效果
 * 方便附属mod添加其他模组生物的契约效果
 */
public class ContractEffectRegistry {
    private static final ContractEffectRegistry INSTANCE = new ContractEffectRegistry();
    private final Map<String, ContractEffect> effectRegistry; // 效果注册表
    private final Map<String, List<String>> entityTypeEffects; // 实体类型对应的效果列表
    
    private ContractEffectRegistry() {
        this.effectRegistry = new HashMap<>();
        this.entityTypeEffects = new HashMap<>();
        registerDefaultEffects();
    }
    
    /**
     * 获取注册管理器实例
     */
    public static ContractEffectRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册契约效果
     * @param effectId 效果唯一标识符
     * @param effect 效果实例
     */
    public void registerEffect(String effectId, ContractEffect effect) {
        effectRegistry.put(effectId, effect);
    }
    
    /**
     * 为实体类型注册效果
     * @param entityType 实体类型（可以是原版实体类型或模组实体类型）
     * @param effectId 效果ID
     */
    public void registerEffectForEntityType(String entityType, String effectId) {
        entityTypeEffects.computeIfAbsent(entityType, k -> new ArrayList<>()).add(effectId);
    }
    
    /**
     * 注册效果并同时关联到实体类型（简化版）
     * @param effectId 效果唯一标识符
     * @param effect 效果实例
     * @param entityType 实体类型（可以是原版实体类型或模组实体类型）
     */
    public void registerEffectAndEntityType(String effectId, ContractEffect effect, String entityType) {
        registerEffect(effectId, effect);
        registerEffectForEntityType(entityType, effectId);
    }
    
    /**
     * 获取效果实例
     */
    public ContractEffect getEffect(String effectId) {
        ContractEffect effect = effectRegistry.get(effectId);
        if (effect != null) {
            try {
                // 尝试使用默认构造函数创建新的实例，避免共享状态
                return effect.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // 如果默认构造函数失败，尝试使用参数化构造函数（向后兼容）
                try {
                    return effect.getClass().getDeclaredConstructor(String.class, String.class, String.class)
                        .newInstance(effect.getEffectId(), effect.getDisplayName(), effect.getDescription());
                } catch (Exception ex) {
                    System.err.println("Failed to create effect instance for: " + effectId);
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取实体类型对应的效果列表
     */
    public List<ContractEffect> getEffectsForEntityType(String entityType) {
        List<ContractEffect> effects = new ArrayList<>();
        List<String> effectIds = entityTypeEffects.get(entityType);
        
        if (effectIds != null) {
            for (String effectId : effectIds) {
                ContractEffect effect = getEffect(effectId);
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        
        return effects;
    }
    
    /**
     * 获取所有已注册的效果ID
     */
    public Set<String> getAllEffectIds() {
        return new HashSet<>(effectRegistry.keySet());
    }
    
    /**
     * 获取所有已注册的实体类型
     */
    public Set<String> getAllEntityTypes() {
        return new HashSet<>(entityTypeEffects.keySet());
    }
    
    /**
     * 检查效果是否存在
     */
    public boolean hasEffect(String effectId) {
        return effectRegistry.containsKey(effectId);
    }
    
    /**
     * 检查实体类型是否有注册的效果
     */
    public boolean hasEffectsForEntityType(String entityType) {
        return entityTypeEffects.containsKey(entityType) && !entityTypeEffects.get(entityType).isEmpty();
    }
    
    /**
     * 注册默认效果和实体类型关联
     */
    private void registerDefaultEffects() {
        
        // 兔子
        registerEffectAndEntityType("rabbit_jump_boost", new RabbitContract(), "minecraft:rabbit");
        // 悦灵
        registerEffectAndEntityType("allay_life_boost", new AllayContract(), "minecraft:allay");
        // 美西螈
        registerEffectAndEntityType("axolotl_feign_death", new AxolotlContract(), "minecraft:axolotl");
    }
}