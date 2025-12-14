package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.RabbitJumpBoostEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Constructor;
import java.util.*;

public class ContractEffectRegistry {
    
    private static final Map<String, Class<? extends ContractEffect>> effectClasses = new HashMap<>();
    private static final Map<EntityType<?>, List<String>> entityTypeEffects = new HashMap<>();
    
    static {
        // 注册效果类
        registerEffectClass("rabbit_jump_boost", RabbitJumpBoostEffect.class);
        
        // 注册实体类型对应的效果
        try {
            EntityType<?> rabbit = EntityType.RABBIT;
            registerEffectsForEntityType(rabbit, Arrays.asList("rabbit_jump_boost"));
        } catch (Exception e) {
            System.err.println("Failed to register rabbit entity type effects: " + e.getMessage());
        }
    }
    
    public static void registerEffectClass(String effectId, Class<? extends ContractEffect> effectClass) {
        effectClasses.put(effectId, effectClass);
        System.out.println("Registered effect class: " + effectId + " -> " + effectClass.getName());
    }
    
    public static void registerEffectsForEntityType(EntityType<?> entityType, List<String> effectIds) {
        entityTypeEffects.put(entityType, new ArrayList<>(effectIds));
        System.out.println("Registered effects for entity type " + getEntityTypeKey(entityType) + ": " + effectIds);
    }
    
    public static ContractEffect getEffect(String effectId, String name, String description) {
        try {
            Class<? extends ContractEffect> effectClass = effectClasses.get(effectId);
            if (effectClass != null) {
                Constructor<? extends ContractEffect> constructor = effectClass.getDeclaredConstructor(String.class, String.class, String.class);
                return constructor.newInstance(effectId, name, description);
            }
        } catch (Exception e) {
            System.err.println("Failed to create effect instance for: " + effectId);
            e.printStackTrace();
        }
        return null;
    }
    
    public static List<ContractEffect> getEffectsForEntityType(EntityType<?> entityType) {
        List<ContractEffect> effects = new ArrayList<>();
        List<String> effectIds = entityTypeEffects.get(entityType);
        
        if (effectIds != null) {
            for (String effectId : effectIds) {
                ContractEffect effect = getEffect(effectId, getEffectDisplayName(effectId), getEffectDescription(effectId));
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        
        return effects;
    }
    
    private static String getEntityTypeKey(EntityType<?> entityType) {
        try {
            ResourceLocation key = EntityType.getKey(entityType);
            return key != null ? key.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getEffectDisplayName(String effectId) {
        switch (effectId) {
            case "rabbit_jump_boost":
                return "Rabbit Jump Boost";
            default:
                return effectId;
        }
    }
    
    private static String getEffectDescription(String effectId) {
        switch (effectId) {
            case "rabbit_jump_boost":
                return "Grants enhanced jumping ability like a rabbit";
            default:
                return "Unknown effect";
        }
    }
    
    public static boolean hasEffectsForEntityType(EntityType<?> entityType) {
        return entityTypeEffects.containsKey(entityType) && !entityTypeEffects.get(entityType).isEmpty();
    }
}