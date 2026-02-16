package com.iamalittle.black_souls_options.fabric.network;

import com.iamalittle.black_souls_options.network.ContractCreatePacket;
import com.iamalittle.black_souls_options.network.ContractSyncPacket;
import com.iamalittle.black_souls_options.network.EffectTogglePacket;
import com.iamalittle.black_souls_options.network.FeignDeathSyncPacket;
import com.iamalittle.black_souls_options.network.RandomSoundPacket;
import com.iamalittle.black_souls_options.network.SnowballAttackPacket;
import com.iamalittle.black_souls_options.network.KillAttackPacket;
import com.iamalittle.black_souls_options.network.SpitAttackPacket;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric版本的契约网络同步
 */
public class FabricContractNetwork {
    public static final ResourceLocation CONTRACT_SYNC_PACKET_ID = new ResourceLocation("black_souls_options", "contract_sync");
    public static final ResourceLocation CONTRACT_CREATE_PACKET_ID = new ResourceLocation("black_souls_options", "contract_create");
    public static final ResourceLocation FEIGN_DEATH_SYNC_PACKET_ID = new ResourceLocation("black_souls_options", "feign_death_sync");
    public static final ResourceLocation EFFECT_TOGGLE_PACKET_ID = new ResourceLocation("black_souls_options", "effect_toggle");
    public static final ResourceLocation SPIT_ATTACK_PACKET_ID = new ResourceLocation("black_souls_options", "spit_attack");
    public static final ResourceLocation RANDOM_SOUND_PACKET_ID = new ResourceLocation("black_souls_options", "random_sound");
    public static final ResourceLocation SNOWBALL_ATTACK_PACKET_ID = new ResourceLocation("black_souls_options", "snowball_attack");
    public static final ResourceLocation KILL_ATTACK_PACKET_ID = new ResourceLocation("black_souls_options", "kill_attack");
    
    /**
     * 注册网络处理器
     */
    public static void initialize() {
        // 服务器端注册
        ServerPlayNetworking.registerGlobalReceiver(CONTRACT_SYNC_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            // 服务器端不需要处理客户端发送的契约同步包
        });
        
        // 服务器端注册契约创建请求处理器
        ServerPlayNetworking.registerGlobalReceiver(CONTRACT_CREATE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            ContractCreatePacket packet = new ContractCreatePacket(buf);
            server.execute(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleContractCreateRequest(player, packet);
            });
        });
        
        // 服务器端注册吐口水攻击请求处理器
        ServerPlayNetworking.registerGlobalReceiver(SPIT_ATTACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            SpitAttackPacket packet = new SpitAttackPacket(buf);
            server.execute(() -> {
                SpitAttackPacket.handle(packet, player);
            });
        });
        
        // 服务器端注册随机音效请求处理器
        ServerPlayNetworking.registerGlobalReceiver(RANDOM_SOUND_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            RandomSoundPacket packet = new RandomSoundPacket(buf);
            server.execute(() -> {
                RandomSoundPacket.handle(packet, player);
            });
        });
        
        // 服务器端注册雪球攻击请求处理器
        ServerPlayNetworking.registerGlobalReceiver(SNOWBALL_ATTACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            SnowballAttackPacket packet = new SnowballAttackPacket(buf);
            server.execute(() -> {
                SnowballAttackPacket.handle(packet, player);
            });
        });
        
        // 注册杀害攻击请求处理器
        ServerPlayNetworking.registerGlobalReceiver(KILL_ATTACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            KillAttackPacket packet = new KillAttackPacket(buf);
            server.execute(() -> {
                ContractNetworkHandler.handleKillAttackRequest(player, packet);
            });
        });
        
        // 客户端注册
        ClientPlayNetworking.registerGlobalReceiver(CONTRACT_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            ContractSyncPacket packet = new ContractSyncPacket(buf);
            client.execute(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleContractSyncPacket(packet);
            });
        });
        
        // 客户端注册装死状态同步处理器
        ClientPlayNetworking.registerGlobalReceiver(FEIGN_DEATH_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            FeignDeathSyncPacket packet = new FeignDeathSyncPacket(buf);
            client.execute(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleFeignDeathSyncPacket(packet);
            });
        });
        
        // 服务器端注册效果开关状态请求处理器
        ServerPlayNetworking.registerGlobalReceiver(EFFECT_TOGGLE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            EffectTogglePacket packet = new EffectTogglePacket(buf);
            server.execute(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleEffectToggleRequest(player, packet);
            });
        });
    }
    
    /**
     * 向玩家发送契约同步数据包
     */
    public static void sendToPlayer(ServerPlayer player, ContractSyncPacket packet) {
        if (player == null || player.connection == null) return;
        
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ServerPlayNetworking.send(player, CONTRACT_SYNC_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送契约创建请求到服务器
     */
    public static void sendCreateRequest(ContractCreatePacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(CONTRACT_CREATE_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送吐口水攻击请求到服务器
     */
    public static void sendSpitAttackRequest(SpitAttackPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(SPIT_ATTACK_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送随机音效请求到服务器
     */
    public static void sendRandomSoundRequest(RandomSoundPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(RANDOM_SOUND_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送雪球攻击请求到服务器
     */
    public static void sendSnowballAttackRequest(SnowballAttackPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(SNOWBALL_ATTACK_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送杀害攻击请求到服务器
     */
    public static void sendKillAttackRequest(KillAttackPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(KILL_ATTACK_PACKET_ID, buf);
    }
    
    /**
     * 向玩家发送装死状态同步数据包
     */
    public static void sendFeignDeathPacket(ServerPlayer player, FeignDeathSyncPacket packet) {
        if (player == null || player.connection == null) return;
        
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ServerPlayNetworking.send(player, FEIGN_DEATH_SYNC_PACKET_ID, buf);
    }
    
    /**
     * 客户端发送效果开关状态请求到服务器
     */
    public static void sendEffectToggleRequest(EffectTogglePacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        
        ClientPlayNetworking.send(EFFECT_TOGGLE_PACKET_ID, buf);
    }
}