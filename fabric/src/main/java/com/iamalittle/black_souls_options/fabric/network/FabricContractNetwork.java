package com.iamalittle.black_souls_options.fabric.network;

import com.iamalittle.black_souls_options.network.ContractCreatePacket;
import com.iamalittle.black_souls_options.network.ContractSyncPacket;
import com.iamalittle.black_souls_options.network.EffectTogglePacket;
import com.iamalittle.black_souls_options.network.FeignDeathSyncPacket;
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