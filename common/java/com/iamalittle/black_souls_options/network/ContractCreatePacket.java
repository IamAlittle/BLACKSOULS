package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * 契约创建请求数据包
 * 客户端向服务器发送契约创建请求
 */
public class ContractCreatePacket {
    private final UUID entityId;
    private final String entityType;
    private final String entityName;
    private final Vec3 position;
    private final String dimension;

    public ContractCreatePacket(UUID entityId, String entityType, String entityName, Vec3 position, String dimension) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.entityName = entityName;
        this.position = position;
        this.dimension = dimension;
    }

    public ContractCreatePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readUUID();
        this.entityType = buf.readUtf();
        this.entityName = buf.readUtf();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.dimension = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(entityId);
        buf.writeUtf(entityType);
        buf.writeUtf(entityName);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeUtf(dimension);
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public Vec3 getPosition() {
        return position;
    }

    public String getDimension() {
        return dimension;
    }
}