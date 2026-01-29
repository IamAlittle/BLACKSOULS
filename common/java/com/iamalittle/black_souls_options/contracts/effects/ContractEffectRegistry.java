package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.*;

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
        // 豹猫（使用与猫相同的契约）
        registerEffectAndEntityType("cat_scare_creeper", new CatContract(), "minecraft:ocelot");
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
        // 僵尸马
        registerEffectAndEntityType("horse_auto_step", new HorseContract(), "minecraft:zombie_horse");
        // 骷髅马
        registerEffectAndEntityType("horse_auto_step", new HorseContract(), "minecraft:skeleton_horse");
        
        // 僵尸
        registerEffectAndEntityType("zombie_infect_villager", new ZombieContract(), "minecraft:zombie");
        // 僵尸村民
        registerEffectAndEntityType("zombie_infect_villager", new ZombieContract(), "minecraft:zombie_villager");
        // 溺尸
        registerEffectAndEntityType("drowned_trident_slowness", new DrownedContract(), "minecraft:drowned");
        // 尸壳
        registerEffectAndEntityType("husk_hunger_effect", new HuskContract(), "minecraft:husk");

        // 远古守卫者
        registerEffectAndEntityType("guardian_fluid_clear", new GuardianContract(), "minecraft:elder_guardian");
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
        
        // 鱿鱼
        registerEffectAndEntityType("squid_blindness_defense", new SquidContract(), "minecraft:squid");
        
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
        
        // 哞菇
        registerEffectAndEntityType("mooshroom_stew_giver", new MooshroomContract(), "minecraft:mooshroom");
        
        // 熊猫
        registerEffectAndEntityType("panda_bamboo_eat", new PandaContract(), "minecraft:panda");
        
        // 鹦鹉
        registerEffectAndEntityType("parrot_random_sound", new ParrotContract(), "minecraft:parrot");
        
        // 幻翼
        registerEffectAndEntityType("phantom_elytra_repair", new PhantomContract(), "minecraft:phantom");
        
        // 猪
        registerEffectAndEntityType("pig_basic", new PigContract(), "minecraft:pig");
        
        // 骷髅
        registerEffectAndEntityType("skeleton_infinite_arrows", new SkeletonContract(), "minecraft:skeleton");
        
        // 猪灵
        registerEffectAndEntityType("piglin_loved_vision", new PiglinContract(), "minecraft:piglin");
        // 猪灵蛮兵
        registerEffectAndEntityType("piglin_loved_vision", new PiglinContract(), "minecraft:piglin_brute");
        
        // 掠夺者
        registerEffectAndEntityType("illager_bad_omen", new IllagerContract(), "minecraft:pillager");
        // 卫道士
        registerEffectAndEntityType("illager_bad_omen", new IllagerContract(), "minecraft:vindicator");
        // 幻术师
        registerEffectAndEntityType("illager_bad_omen", new IllagerContract(), "minecraft:illusioner");
        // 劫掠兽
        registerEffectAndEntityType("ravager_destroy_plants", new RavagerContract(), "minecraft:ravager");
        
        // 北极熊
        registerEffectAndEntityType("polar_bear_freeze_immunity", new PolarBearContract(), "minecraft:polar_bear");
        
        // 河豚
        registerEffectAndEntityType("pufferfish_poison_aura", new PufferfishContract(), "minecraft:pufferfish");
        
        // 岩浆怪
        registerEffectAndEntityType("magma_cube_fire_immunity", new MagmaCubeContract(), "minecraft:magma_cube");
        
        // 绵羊
        registerEffectAndEntityType("sheep_rainbow_color", new SheepContract(), "minecraft:sheep");
        
        // 潜影贝
        registerEffectAndEntityType("shulker_levitation", new ShulkerContract(), "minecraft:shulker");
        
        // 蠹虫
        registerEffectAndEntityType("silverfish_stone_breaker", new SilverfishContract(), "minecraft:silverfish");
        
        // 嗅探兽
        registerEffectAndEntityType("sniffer_find_treasure", new SnifferContract(), "minecraft:sniffer");
        
        // 雪傀儡
        registerEffectAndEntityType("snow_golem_snowball_attack", new SnowGolemContract(), "minecraft:snow_golem");

        // 蜘蛛
        registerEffectAndEntityType("spider_climbing", new SpiderContract(), "minecraft:spider");

        // 流浪者
        registerEffectAndEntityType("stray_slowness_arrows", new StrayContract(), "minecraft:stray");
        
        // 赤足兽
        registerEffectAndEntityType("strider_lava_walking", new StriderContract(), "minecraft:strider");
        
        // 蝌蚪
        registerEffectAndEntityType("tadpole_mom_question", new TadpoleContract(), "minecraft:tadpole");
        
        // 海龟
        registerEffectAndEntityType("turtle_attract_hostile", new TurtleContract(), "minecraft:turtle");
        
        // 恼鬼
        registerEffectAndEntityType("vex_immune", new VexContract(), "minecraft:vex");
        
        // 村民
        registerEffectAndEntityType("villager_hero_of_the_village", new VillagerContract(), "minecraft:villager");
        
        // 流浪商人
        registerEffectAndEntityType("villager_hero_of_the_village", new VillagerContract(), "minecraft:wandering_trader");
        
        // 监守者
        registerEffectAndEntityType("warden_vibration_glow", new WardenContract(), "minecraft:warden");
        
        // 女巫
        registerEffectAndEntityType("witch_magic_resistance", new WitchContract(), "minecraft:witch");
        
        // 狼
        registerEffectAndEntityType("wolf_tame_wolves", new WolfContract(), "minecraft:wolf");
        
        // 凋零骷髅
        registerEffectAndEntityType("wither_skeleton_wither", new WitherSkeletonContract(), "minecraft:wither_skeleton");
    }
}