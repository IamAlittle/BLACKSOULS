package com.iamalittle.black_souls_options.forge.network;

import com.iamalittle.black_souls_options.network.ContractCreatePacket;
import com.iamalittle.black_souls_options.network.ContractDeletePacket;
import com.iamalittle.black_souls_options.network.ContractSyncPacket;
import com.iamalittle.black_souls_options.network.EffectTogglePacket;
import com.iamalittle.black_souls_options.network.FeignDeathSyncPacket;
import com.iamalittle.black_souls_options.network.RandomSoundPacket;
import com.iamalittle.black_souls_options.network.SnowballAttackPacket;
import com.iamalittle.black_souls_options.network.SpitAttackPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Forge版本的契约网络同步
 */
public class ForgeContractNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("black_souls_options", "contract_sync"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * 注册网络处理器
     */
    public static void initialize() {
        INSTANCE.registerMessage(packetId++, ContractSyncPacket.class,
            ContractSyncPacket::encode,
            ContractSyncPacket::new,
            ForgeContractNetwork::handleSyncPacket
        );
        
        INSTANCE.registerMessage(packetId++, ContractCreatePacket.class,
            ContractCreatePacket::encode,
            ContractCreatePacket::new,
            ForgeContractNetwork::handleCreatePacket
        );
        
        INSTANCE.registerMessage(packetId++, ContractDeletePacket.class,
            ContractDeletePacket::encode,
            ContractDeletePacket::new,
            ForgeContractNetwork::handleDeletePacket
        );
        
        INSTANCE.registerMessage(packetId++, FeignDeathSyncPacket.class,
            FeignDeathSyncPacket::encode,
            FeignDeathSyncPacket::new,
            ForgeContractNetwork::handleFeignDeathPacket
        );
        
        INSTANCE.registerMessage(packetId++, EffectTogglePacket.class,
            EffectTogglePacket::encode,
            EffectTogglePacket::new,
            ForgeContractNetwork::handleEffectTogglePacket
        );
        
        INSTANCE.registerMessage(packetId++, SpitAttackPacket.class,
            SpitAttackPacket::encode,
            SpitAttackPacket::new,
            ForgeContractNetwork::handleSpitAttackPacket
        );
        
        INSTANCE.registerMessage(packetId++, RandomSoundPacket.class,
            RandomSoundPacket::encode,
            RandomSoundPacket::new,
            ForgeContractNetwork::handleRandomSoundPacket
        );
        
        INSTANCE.registerMessage(packetId++, SnowballAttackPacket.class,
            SnowballAttackPacket::encode,
            SnowballAttackPacket::new,
            ForgeContractNetwork::handleSnowballAttackPacket
        );
    }
    
    /**
     * 处理接收到的契约同步数据包
     */
    private static void handleSyncPacket(ContractSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            // 客户端处理
            ctx.enqueueWork(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleContractSyncPacket(packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的契约创建请求数据包
     */
    private static void handleCreatePacket(ContractCreatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleContractCreateRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的契约删除请求数据包
     */
    private static void handleDeletePacket(ContractDeletePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleContractDeleteRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的装死状态同步数据包
     */
    private static void handleFeignDeathPacket(FeignDeathSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            // 客户端处理
            ctx.enqueueWork(() -> {
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleFeignDeathSyncPacket(packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的效果开关状态同步数据包
     */
    private static void handleEffectTogglePacket(EffectTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleEffectToggleRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的吐口水攻击请求数据包
     */
    private static void handleSpitAttackPacket(SpitAttackPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleSpitAttackRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的随机音效请求数据包
     */
    private static void handleRandomSoundPacket(RandomSoundPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleRandomSoundRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 处理接收到的雪球攻击请求数据包
     */
    private static void handleSnowballAttackPacket(SnowballAttackPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            // 服务器端处理
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                com.iamalittle.black_souls_options.network.ContractNetworkHandler.handleSnowballAttackRequest(player, packet);
            });
        }
        
        ctx.setPacketHandled(true);
    }
    
    /**
     * 向玩家发送契约同步数据包
     */
    public static void sendToPlayer(ServerPlayer player, ContractSyncPacket packet) {
        if (player == null) return;
        
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 向玩家发送装死状态同步数据包
     */
    public static void sendToPlayer(ServerPlayer player, FeignDeathSyncPacket packet) {
        if (player == null) return;
        
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 客户端发送契约创建请求到服务器
     */
    public static void sendCreateRequest(ContractCreatePacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 客户端发送契约删除请求到服务器
     */
    public static void sendDeleteRequest(ContractDeletePacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 客户端发送效果开关状态请求到服务器
     */
    public static void sendEffectToggleRequest(EffectTogglePacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 客户端发送吐口水攻击请求到服务器
     */
    public static void sendSpitAttackRequest(SpitAttackPacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 客户端发送随机音效请求到服务器
     */
    public static void sendRandomSoundRequest(RandomSoundPacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 客户端发送雪球攻击请求到服务器
     */
    public static void sendSnowballAttackRequest(SnowballAttackPacket packet) {
        INSTANCE.sendToServer(packet);
    }
}