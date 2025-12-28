package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.RabbitContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AllayContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.BatContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.BeeContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.BlazeContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CamelContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CatContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CaveSpiderContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.ChickenContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EndermiteContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.FishContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CowContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CreeperContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.DolphinContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.HorseContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.ZombieContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.DrownedContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.HuskContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EnderManContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EvokerContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.FoxContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.FrogContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GhastContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GlowSquidContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GoatContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianThornsContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.HoglinContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.IronGolemContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.LlamaContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SlimeContract;

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
        // 蝙蝠
        registerEffectAndEntityType("bat_night_vision", new BatContract(), "minecraft:bat");
        // 蜜蜂
        registerEffectAndEntityType("bee_pollen_spreader", new BeeContract(), "minecraft:bee");
        // 烈焰人
        registerEffectAndEntityType("blaze_fire_bender", new BlazeContract(), "minecraft:blaze");
        // 骆驼
        registerEffectAndEntityType("camel_cactus_eat", new CamelContract(), "minecraft:camel");
        // 猫
        registerEffectAndEntityType("cat_scare_creeper", new CatContract(), "minecraft:cat");
        // 洞穴蜘蛛
        registerEffectAndEntityType("cave_spider_poison", new CaveSpiderContract(), "minecraft:cave_spider");

        // 鱼类（鲑鱼）
        registerEffectAndEntityType("fish_swim_boost", new FishContract(), "minecraft:salmon");
        // 鱼类（鳕鱼）
        registerEffectAndEntityType("fish_swim_boost", new FishContract(), "minecraft:cod");
        // 鱼类（热带鱼）
        registerEffectAndEntityType("fish_swim_boost", new FishContract(), "minecraft:tropical_fish");

        // 牛
        registerEffectAndEntityType("cow_clear_buffs", new CowContract(), "minecraft:cow");
        // 苦力怕
        registerEffectAndEntityType("creeper_death_explosion", new CreeperContract(), "minecraft:creeper");
        // 海豚
        registerEffectAndEntityType("dolphin_chest_vision", new DolphinContract(), "minecraft:dolphin");

        // 马
        registerEffectAndEntityType("horse_auto_step", new HorseContract(), "minecraft:horse");
        // 驴
        registerEffectAndEntityType("horse_auto_step", new HorseContract(), "minecraft:donkey");
        // 骡
        registerEffectAndEntityType("horse_auto_step", new HorseContract(), "minecraft:mule");
        
        // 僵尸
        registerEffectAndEntityType("zombie_infect_villager", new ZombieContract(), "minecraft:zombie");
        // 僵尸村民
        registerEffectAndEntityType("zombie_infect_villager", new ZombieContract(), "minecraft:zombie_villager");
        // 溺尸
        registerEffectAndEntityType("drowned_trident_slowness", new DrownedContract(), "minecraft:drowned");
        // 尸壳
        registerEffectAndEntityType("husk_hunger_effect", new HuskContract(), "minecraft:husk");

        // 远古守卫者
        registerEffectAndEntityType("guardian_water_clear", new GuardianContract(), "minecraft:elder_guardian");
        // 末影人
        registerEffectAndEntityType("enderman_projectile_immunity", new EnderManContract(), "minecraft:enderman");
        // 鸡
        registerEffectAndEntityType("chicken_slow_falling", new ChickenContract(), "minecraft:chicken");
        
        // 末影螨
        registerEffectAndEntityType("endermite_anger_enderman", new EndermiteContract(), "minecraft:endermite");
        
        // 唤魔者
        registerEffectAndEntityType("evoker_death_totem", new EvokerContract(), "minecraft:evoker");
        
        // 狐狸
        registerEffectAndEntityType("fox_berry_immunity", new FoxContract(), "minecraft:fox");
        
        // 青蛙
        registerEffectAndEntityType("frog_magma_cube_drops", new FrogContract(), "minecraft:frog");
        
        // 恶魂
        registerEffectAndEntityType("ghast_fireball_deflection", new GhastContract(), "minecraft:ghast");
        
        // 发光鱿鱼
        registerEffectAndEntityType("glow_squid_glowing", new GlowSquidContract(), "minecraft:glow_squid");
        
        // 山羊
        registerEffectAndEntityType("goat_floor_charge", new GoatContract(), "minecraft:goat");
        
        // 守卫者
        registerEffectAndEntityType("guardian_thorns", new GuardianThornsContract(), "minecraft:guardian");
        
        // 疣猪兽
        registerEffectAndEntityType("hoglin_knockback_resistance", new HoglinContract(), "minecraft:hoglin");
        // 僵尸疣猪兽
        registerEffectAndEntityType("hoglin_knockback_resistance", new HoglinContract(), "minecraft:zoglin");
        
        // 铁傀儡
        registerEffectAndEntityType("iron_golem_knockback_immunity", new IronGolemContract(), "minecraft:iron_golem");
        
        // 羊驼
        registerEffectAndEntityType("llama_spit_attack", new LlamaContract(), "minecraft:llama");
        // 行商羊驼
        registerEffectAndEntityType("llama_spit_attack", new LlamaContract(), "minecraft:trader_llama");
        
        // 史莱姆
        registerEffectAndEntityType("slime_split_rebirth", new SlimeContract(), "minecraft:slime");
        // 岩浆怪
        registerEffectAndEntityType("slime_split_rebirth", new SlimeContract(), "minecraft:magma_cube");
    }
}