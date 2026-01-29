package com.iamalittle.black_souls_options.network;

import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.effects.mobs.LlamaContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.ParrotContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SnowGolemContract;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 契约网络处理器
 * 处理契约数据的网络同步逻辑
 */
public class ContractNetworkHandler {
    
    /**
     * 服务器端：向指定玩家发送契约数据
     */
    public static void sendContractDataToPlayer(ServerPlayer player, boolean fullSync) {
        if (player == null || player.connection == null) return;
        
        ContractSyncPacket packet = ContractSyncPacket.createForPlayer(player, fullSync);
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendPacketFabric(player, packet);
        } else {
            sendPacketForge(player, packet);
        }
    }
    
    /**
     * 服务器端：向所有在线玩家广播契约数据更新
     */
    public static void broadcastContractUpdate(ServerPlayer sourcePlayer) {
        if (sourcePlayer == null || sourcePlayer.server == null) return;
        
        for (ServerPlayer player : sourcePlayer.server.getPlayerList().getPlayers()) {
            sendContractDataToPlayer(player, false); // 增量同步
        }
    }
    
    /**
     * 客户端：处理接收到的契约数据包
     */
    public static void handleContractSyncPacket(ContractSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        UUID targetPlayerUUID = packet.getPlayerUUID();
        UUID currentPlayerUUID = minecraft.player.getUUID();
        
        // 只处理当前玩家的契约数据
        if (!targetPlayerUUID.equals(currentPlayerUUID)) {
            return;
        }
        
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(minecraft.player);
        if (manager == null) {
            System.out.println("[BLACKSOULS] Contract manager not found for client player");
            return;
        }
        
        if (packet.isFullSync()) {
            // 全量同步：清空现有数据并重新加载
            manager.clearContracts();
        } else {
            // 增量同步：删除服务器端已不存在的契约
            Set<UUID> serverContractIds = new HashSet<>();
            for (ContractSyncPacket.ContractData contractData : packet.getContracts()) {
                serverContractIds.add(contractData.entityId);
            }
            
            // 获取客户端当前所有契约
            Collection<Contract> clientContracts = manager.getAllContracts();
            List<UUID> contractsToRemove = new ArrayList<>();
            
            for (Contract clientContract : clientContracts) {
                if (!serverContractIds.contains(clientContract.getEntityId())) {
                    contractsToRemove.add(clientContract.getEntityId());
                }
            }
            
            // 删除服务器端已不存在的契约
            for (UUID entityId : contractsToRemove) {
                manager.removeContract(entityId);
                System.out.println("[BLACKSOULS] Contract removed on client: " + entityId);
            }
        }
        
        // 添加或更新契约数据
        for (ContractSyncPacket.ContractData contractData : packet.getContracts()) {
            Contract contract = contractData.toContract();
            contract.setTracking(contractData.isTracking);
            
            if (!manager.hasContract(contractData.entityId)) {
                manager.addContractFromNetwork(contract);
            } else {
                manager.updateContractFromNetwork(contract);
            }
        }
        
        System.out.println("[BLACKSOULS] Contract data synchronized for client player: " + packet.getContracts().size() + " contracts");
    }
    
    /**
     * 客户端：发送契约创建请求到服务器
     */
    public static void sendContractCreateRequest(UUID entityId, String entityType, String entityName, Vec3 position, String dimension) {
        ContractCreatePacket packet = new ContractCreatePacket(entityId, entityType, entityName, position, dimension);
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendCreatePacketFabric(packet);
        } else {
            sendCreatePacketForge(packet);
        }
    }
    
    /**
     * 客户端：发送契约删除请求到服务器
     */
    public static void sendContractDeleteRequest(UUID entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        ContractDeletePacket packet = new ContractDeletePacket(entityId, minecraft.player.getUUID());
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendDeletePacketFabric(packet);
        } else {
            sendDeletePacketForge(packet);
        }
    }
    
    /**
     * 服务器端：处理契约删除请求
     */
    public static void handleContractDeleteRequest(ServerPlayer player, ContractDeletePacket packet) {
        if (player == null || player.server == null) return;
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Contract delete request from wrong player");
            return;
        }
        
        ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
        if (manager == null) {
            System.out.println("[BLACKSOULS] Contract manager not found for server player: " + player.getName().getString());
            return;
        }
        
        // 在服务器端删除契约
        manager.removeContract(packet.getEntityId());
        
        // 向所有玩家广播契约更新
        broadcastContractUpdate(player);
        
        System.out.println("[BLACKSOULS] Contract deleted on server for player: " + player.getName().getString() + ", entityId: " + packet.getEntityId());
    }
    
    /**
     * 服务器端：处理契约创建请求
     */
    public static void handleContractCreateRequest(ServerPlayer player, ContractCreatePacket packet) {
        if (player == null || player.server == null) return;
        
        ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
        if (manager == null) {
            System.out.println("[BLACKSOULS] Contract manager not found for server player: " + player.getName().getString());
            return;
        }
        
        // 在服务器端创建契约
        manager.createContract(packet.getEntityId(), packet.getEntityType(), packet.getEntityName(), packet.getPosition(), packet.getDimension());
        
        // 向所有玩家广播契约更新
        broadcastContractUpdate(player);
        
        System.out.println("[BLACKSOULS] Contract created on server for player: " + player.getName().getString() + ", entity: " + packet.getEntityName());
    }
    
    /**
     * 检查是否在Fabric环境中
     */
    private static boolean isFabric() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Fabric版本的数据包发送
     */
    private static void sendPacketFabric(ServerPlayer player, ContractSyncPacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendToPlayer", ServerPlayer.class, ContractSyncPacket.class);
            method.invoke(null, player, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的数据包发送
     */
    private static void sendPacketForge(ServerPlayer player, ContractSyncPacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendToPlayer", ServerPlayer.class, ContractSyncPacket.class);
            method.invoke(null, player, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge packet: " + e.getMessage());
        }
    }
    
    /**
     * Fabric版本的契约创建请求发送
     */
    private static void sendCreatePacketFabric(ContractCreatePacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendCreateRequest", ContractCreatePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric create packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的契约创建请求发送
     */
    private static void sendCreatePacketForge(ContractCreatePacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendCreateRequest", ContractCreatePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge create packet: " + e.getMessage());
        }
    }
    
    /**
     * Fabric版本的契约删除请求发送
     */
    private static void sendDeletePacketFabric(ContractDeletePacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendDeleteRequest", ContractDeletePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric delete packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的契约删除请求发送
     */
    private static void sendDeletePacketForge(ContractDeletePacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendDeleteRequest", ContractDeletePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge delete packet: " + e.getMessage());
        }
    }
    
    /**
     * 服务器端：向所有玩家广播装死状态同步
     */
    public static void broadcastFeignDeathState(FeignDeathSyncPacket packet) {
        // 获取服务器实例
        MinecraftServer server = getServerInstance();
        if (server == null) {
            System.err.println("[BLACKSOULS] Failed to broadcast feign death state: server instance not found");
            return;
        }
        
        // 向所有在线玩家发送装死状态同步
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendFeignDeathPacket(player, packet);
        }
        
        System.out.println("[BLACKSOULS] Feign death state broadcasted to all players");
    }
    
    /**
     * 向特定玩家发送装死状态同步数据包
     */
    private static void sendFeignDeathPacket(ServerPlayer player, FeignDeathSyncPacket packet) {
        if (isFabric()) {
            sendFeignDeathPacketFabric(player, packet);
        } else {
            sendFeignDeathPacketForge(player, packet);
        }
    }
    
    /**
     * Fabric版本的装死状态同步数据包发送
     */
    private static void sendFeignDeathPacketFabric(ServerPlayer player, FeignDeathSyncPacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendFeignDeathPacket", ServerPlayer.class, FeignDeathSyncPacket.class);
            method.invoke(null, player, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric feign death packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的装死状态同步数据包发送
     */
    private static void sendFeignDeathPacketForge(ServerPlayer player, FeignDeathSyncPacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendToPlayer", ServerPlayer.class, FeignDeathSyncPacket.class);
            method.invoke(null, player, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge feign death packet: " + e.getMessage());
        }
    }
    
    /**
     * 获取服务器实例
     */
    private static MinecraftServer getServerInstance() {
        try {
            // 尝试通过Fabric方式获取
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            java.lang.reflect.Method getInstanceMethod = fabricLoaderClass.getMethod("getInstance");
            Object fabricLoader = getInstanceMethod.invoke(null);
            
            java.lang.reflect.Method getGameInstanceMethod = fabricLoaderClass.getMethod("getGameInstance");
            Object gameInstance = getGameInstanceMethod.invoke(fabricLoader);
            
            if (gameInstance instanceof MinecraftServer) {
                return (MinecraftServer) gameInstance;
            }
        } catch (Exception e) {
            // 忽略Fabric获取失败，尝试Forge方式
        }
        
        try {
            // 尝试通过Forge方式获取
            Class<?> serverLifecycleClass = Class.forName("net.minecraftforge.server.ServerLifecycleHooks");
            java.lang.reflect.Method getCurrentServerMethod = serverLifecycleClass.getMethod("getCurrentServer");
            return (MinecraftServer) getCurrentServerMethod.invoke(null);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to get server instance: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 客户端：处理接收到的装死状态同步数据包
     */
    public static void handleFeignDeathSyncPacket(FeignDeathSyncPacket packet) {
        // 委托给AxolotlContract处理
        com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract.handleFeignDeathSync(packet);
    }
    
    /**
     * 客户端：发送效果开关状态请求到服务器
     */
    public static void sendEffectToggleRequest(UUID entityId, boolean isActive) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        EffectTogglePacket packet = new EffectTogglePacket(entityId, minecraft.player.getUUID(), isActive);
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendEffectTogglePacketFabric(packet);
        } else {
            sendEffectTogglePacketForge(packet);
        }
    }
    
    /**
     * 服务器端：处理效果开关状态请求
     */
    public static void handleEffectToggleRequest(ServerPlayer player, EffectTogglePacket packet) {
        if (player == null || player.server == null) return;
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Effect toggle request from wrong player");
            return;
        }
        
        ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
        if (manager == null) {
            System.out.println("[BLACKSOULS] Contract manager not found for server player: " + player.getName().getString());
            return;
        }
        
        // 更新服务器端的效果状态
        Contract contract = manager.getContract(packet.getEntityId());
        if (contract != null) {
            if (packet.isActive()) {
                contract.activateEffects(player, false); // 不发送消息
            } else {
                contract.deactivateEffects(player);
            }
            
            // 向所有玩家广播契约更新
            broadcastContractUpdate(player);
            
            System.out.println("[BLACKSOULS] Effect toggle updated on server for player: " + player.getName().getString() + 
                             ", entityId: " + packet.getEntityId() + ", isActive: " + packet.isActive());
        }
    }
    
    /**
     * 服务器端：处理吐口水攻击请求
     */
    public static void handleSpitAttackRequest(ServerPlayer player, SpitAttackPacket packet) {
        if (player == null || player.server == null) return;
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Spit attack request from wrong player");
            return;
        }
        
        // 检查玩家是否有羊驼契约
        if (LlamaContract.hasLlamaContract(player)) {
            // 执行吐口水攻击
            boolean success = LlamaContract.performSpitAttack(player);
            if (success) {
                System.out.println("[BLACKSOULS] Spit attack performed for player: " + player.getName().getString());
            } else {
                System.out.println("[BLACKSOULS] Spit attack failed for player: " + player.getName().getString());
            }
        } else {
            System.out.println("[BLACKSOULS] Player does not have llama contract: " + player.getName().getString());
        }
    }
    
    /**
     * 服务器端：处理随机音效请求
     */
    public static void handleRandomSoundRequest(ServerPlayer player, RandomSoundPacket packet) {
        if (player == null || player.server == null) return;
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Random sound request from wrong player");
            return;
        }
        
        // 检查玩家是否有鹦鹉契约
        if (ParrotContract.hasParrotContract(player)) {
            // 执行随机音效播放
            boolean success = ParrotContract.performRandomSound(player);
            if (success) {
                System.out.println("[BLACKSOULS] Random sound played for player: " + player.getName().getString());
            } else {
                System.out.println("[BLACKSOULS] Random sound failed for player: " + player.getName().getString());
            }
        } else {
            System.out.println("[BLACKSOULS] Player does not have parrot contract: " + player.getName().getString());
        }
    }
    
    /**
     * 服务器端：处理雪球攻击请求
     */
    public static void handleSnowballAttackRequest(ServerPlayer player, SnowballAttackPacket packet) {
        if (player == null || player.server == null) return;
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Snowball attack request from wrong player");
            return;
        }
        
        // 检查玩家是否有雪傀儡契约
        if (SnowGolemContract.hasSnowGolemContract(player)) {
            // 执行雪球攻击
            boolean success = SnowGolemContract.performSnowballAttack(player);
            if (success) {
                System.out.println("[BLACKSOULS] Snowball attack performed for player: " + player.getName().getString());
            } else {
                System.out.println("[BLACKSOULS] Snowball attack failed for player: " + player.getName().getString());
            }
        } else {
            System.out.println("[BLACKSOULS] Player does not have snow golem contract: " + player.getName().getString());
        }
    }
    
    /**
     * 客户端：发送吐口水攻击请求到服务器
     */
    public static void sendSpitAttackRequest() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        SpitAttackPacket packet = new SpitAttackPacket(minecraft.player.getUUID());
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendSpitAttackPacketFabric(packet);
        } else {
            sendSpitAttackPacketForge(packet);
        }
    }
    
    /**
     * 客户端：发送随机音效请求到服务器
     */
    public static void sendRandomSoundRequest() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        RandomSoundPacket packet = new RandomSoundPacket(minecraft.player.getUUID());
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendRandomSoundPacketFabric(packet);
        } else {
            sendRandomSoundPacketForge(packet);
        }
    }
    
    /**
     * 客户端：发送雪球攻击请求到服务器
     */
    public static void sendSnowballAttackRequest() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        SnowballAttackPacket packet = new SnowballAttackPacket(minecraft.player.getUUID());
        
        // Fabric和Forge分别实现网络发送
        if (isFabric()) {
            sendSnowballAttackPacketFabric(packet);
        } else {
            sendSnowballAttackPacketForge(packet);
        }
    }
    
    /**
     * Fabric版本的吐口水攻击数据包发送
     */
    private static void sendSpitAttackPacketFabric(SpitAttackPacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendSpitAttackRequest", SpitAttackPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric spit attack packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的吐口水攻击数据包发送
     */
    private static void sendSpitAttackPacketForge(SpitAttackPacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendSpitAttackRequest", SpitAttackPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge spit attack packet: " + e.getMessage());
        }
    }
    
    /**
     * Fabric版本的随机音效数据包发送
     */
    private static void sendRandomSoundPacketFabric(RandomSoundPacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendRandomSoundRequest", RandomSoundPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric random sound packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的随机音效数据包发送
     */
    private static void sendRandomSoundPacketForge(RandomSoundPacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendRandomSoundRequest", RandomSoundPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge random sound packet: " + e.getMessage());
        }
    }
    
    /**
     * Fabric版本的雪球攻击数据包发送
     */
    private static void sendSnowballAttackPacketFabric(SnowballAttackPacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendSnowballAttackRequest", SnowballAttackPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric snowball attack packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的雪球攻击数据包发送
     */
    private static void sendSnowballAttackPacketForge(SnowballAttackPacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendSnowballAttackRequest", SnowballAttackPacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge snowball attack packet: " + e.getMessage());
        }
    }
    
    /**
     * Fabric版本的效果开关状态数据包发送
     */
    private static void sendEffectTogglePacketFabric(EffectTogglePacket packet) {
        try {
            Class<?> fabricPacketClass = Class.forName("com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork");
            java.lang.reflect.Method method = fabricPacketClass.getMethod("sendEffectToggleRequest", EffectTogglePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Fabric effect toggle packet: " + e.getMessage());
        }
    }
    
    /**
     * Forge版本的效果开关状态数据包发送
     */
    private static void sendEffectTogglePacketForge(EffectTogglePacket packet) {
        try {
            Class<?> forgePacketClass = Class.forName("com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork");
            java.lang.reflect.Method method = forgePacketClass.getMethod("sendEffectToggleRequest", EffectTogglePacket.class);
            method.invoke(null, packet);
        } catch (Exception e) {
            System.err.println("[BLACKSOULS] Failed to send Forge effect toggle packet: " + e.getMessage());
        }
    }
}