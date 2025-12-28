package com.iamalittle.black_souls_options.contracts;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 契约管理器，负责管理玩家的契约列表
 */
public class ContractManager {
    private final Player owner;                    // 契约所有者
    private final Map<UUID, Contract> contracts;   // 契约映射表
    private final File saveFile;                   // 保存文件路径
    private boolean needsSave = false;             // 是否需要保存
    private long lastSaveTime = 0;                 // 最后保存时间
    private static final long SAVE_INTERVAL_MS = 30000; // 30秒保存一次
    
    public ContractManager(Player player) {
        this.owner = player;
        this.contracts = new HashMap<>();
        
        // 关键修复：检查是否在服务器端执行
        if (player == null || player.level() == null || player.level().isClientSide()) {
            // 客户端创建管理器，使用临时文件路径
            this.saveFile = new File("temp_contracts", player != null ? player.getUUID().toString() + ".dat" : "unknown.dat");
            System.out.println("[BLACKSOULS] Client ContractManager created for: " + (player != null ? player.getScoreboardName() : "null"));
        } else {
            // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下初始化
            if (player.level().getServer() == null) {
                // 玩家无效，无法获取服务器路径，使用临时文件路径
                this.saveFile = new File("temp_contracts", player != null ? player.getUUID().toString() + ".dat" : "unknown.dat");
                System.err.println("[BLACKSOULS] Warning: ContractManager created for invalid player, using temporary file path");
            } else {
                // 构建保存文件路径：存档路径/playerdata/contracts/玩家uuid.dat
                File worldDir = player.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
                File contractsDir = new File(worldDir, "contracts");
                this.saveFile = new File(contractsDir, player.getUUID().toString() + ".dat");
                
                // 确保目录存在
                if (!contractsDir.exists()) {
                    contractsDir.mkdirs();
                }
                
                // 加载现有数据（仅在服务器端）
                loadFromFile();
            }
        }
        
        // 注册实体移动事件监听器，实现实时坐标更新
        Events.EntityMoved.add(this::onEntityMoved);
        
        // 注册实体死亡事件监听器，实现目标死亡时移除契约
        Events.EntityDeath.add(this::onEntityDeath);
    }
    
    /**
     * 创建新契约（仅在服务器端执行）
     */
    public void createContract(UUID entityUuid, String entityType, String entityName, Vec3 position, String dimension) {
        // 检查是否在服务器端执行
        if (owner == null || owner.level() == null || owner.level().isClientSide()) {
            System.out.println("[BLACKSOULS] Warning: Attempted to create contract on client side");
            return;
        }
        
        Contract contract = new Contract(entityUuid, entityType, entityName, position, dimension);
        
        // 为契约添加对应的效果
        addEffectsToContract(contract, entityType);
        
        contracts.put(entityUuid, contract);
        markForSave();
        
        System.out.println("[BLACKSOULS] Contract created on server for entity: " + entityName);
    }
    
    /**
     * 检查是否已有该实体的契约
     */
    public boolean hasContract(UUID entityId) {
        return contracts.containsKey(entityId);
    }
    
    /**
     * 获取契约
     */
    public Contract getContract(UUID entityId) {
        return contracts.get(entityId);
    }
    
    /**
     * 更新契约中的实体位置
     */
    public void updateContractPosition(UUID entityId, Vec3 position) {
        Contract contract = contracts.get(entityId);
        if (contract != null) {
            contract.setEntityPosition(position);
        }
    }
    
    /**
     * 移除契约
     */
    public void removeContract(UUID entityId) {
        Contract contract = contracts.get(entityId);
        if (contract != null) {
            // 停用契约效果
            contract.deactivateEffects(owner);
        }
        contracts.remove(entityId);
        markForSave();
    }
    
    /**
     * 清空所有契约（用于网络同步）
     */
    public void clearContracts() {
        // 停用所有契约效果
        for (Contract contract : contracts.values()) {
            contract.deactivateEffects(owner);
        }
        contracts.clear();
    }
    
    /**
     * 从网络添加契约（用于客户端同步）
     */
    public void addContractFromNetwork(Contract contract) {
        // 确保契约有对应的效果
        if (contract.getEffects().isEmpty()) {
            // 如果契约没有效果，从注册表获取对应实体类型的效果
            addEffectsToContract(contract, contract.getEntityType());
        }
        
        contracts.put(contract.getEntityId(), contract);
        // 不标记保存，因为这是从服务器同步的数据
    }
    
    /**
     * 从网络更新契约（用于客户端同步）
     */
    public void updateContractFromNetwork(Contract contract) {
        Contract existing = contracts.get(contract.getEntityId());
        if (existing != null) {
            // 更新位置信息
            existing.setEntityPosition(contract.getEntityPosition());
            existing.setTracking(contract.isTracking());
        }
    }
    
    /**
     * 为契约添加对应的效果
     */
    private void addEffectsToContract(Contract contract, String entityType) {
        // 从注册表获取实体类型对应的效果
        List<ContractEffect> effects = ContractEffectRegistry.getInstance().getEffectsForEntityType(entityType);
        
        for (ContractEffect effect : effects) {
            // 在添加效果前设置契约目标名称
            effect.getEffectData().putString("contractEntityName", contract.getEntityName());
            contract.addEffect(effect);
        }
        
        // 激活契约效果
        contract.activateEffects(owner);
    }
    
    /**
     * 标记需要保存
     */
    public void markForSave() {
        this.needsSave = true;
    }
    
    /**
     * 定期保存检查
     */
    public void tick() {
        // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下执行
        if (owner == null || !owner.isAlive() || owner.level() == null) {
            // 玩家死亡或无效，跳过tick执行
            return;
        }
        
        if (needsSave && System.currentTimeMillis() - lastSaveTime > SAVE_INTERVAL_MS) {
            saveToFile();
        }
        
        // 更新所有契约效果
        updateContractEffects();
    }
    
    /**
     * 更新所有契约效果
     */
    private void updateContractEffects() {
        // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下执行
        if (owner == null || !owner.isAlive() || owner.level() == null) {
            return;
        }
        
        for (Contract contract : contracts.values()) {
            contract.tickEffects(owner);
        }
    }
    
    /**
     * 从文件加载契约数据
     */
    private void loadFromFile() {
        // 检查是否在服务器端执行
        if (owner == null || owner.level() == null || owner.level().isClientSide()) {
            System.out.println("[BLACKSOULS] Warning: Attempted to load contract data on client side");
            return;
        }
        
        if (!saveFile.exists()) {
            return;
        }
        
        try {
            CompoundTag rootTag = NbtIo.read(saveFile);
            if (rootTag != null && rootTag.contains("contracts")) {
                ListTag contractsList = rootTag.getList("contracts", 10); // 10 = CompoundTag
                
                for (int i = 0; i < contractsList.size(); i++) {
                    CompoundTag contractTag = contractsList.getCompound(i);
                    Contract contract = Contract.fromNBT(contractTag);
                    if (contract != null) {
                        contracts.put(contract.getEntityId(), contract);
                    }
                }
                
                // 关键修复：加载契约数据后，重新激活之前已激活的契约效果
                // 这解决了进入游戏时需要重新开关契约的问题
                if (owner != null && owner.isAlive()) {
                    reactivateActiveEffects(owner);
                    System.out.println("[BLACKSOULS] Previously active contract effects reactivated after loading: " + owner.getScoreboardName());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load contracts from file: " + saveFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存契约数据到文件
     */
    public void saveToFile() {
        // 检查是否在服务器端执行
        if (owner == null || owner.level() == null || owner.level().isClientSide()) {
            System.out.println("[BLACKSOULS] Warning: Attempted to save contract data on client side");
            return;
        }
        
        try {
            CompoundTag rootTag = new CompoundTag();
            ListTag contractsList = new ListTag();
            
            for (Contract contract : contracts.values()) {
                contractsList.add(contract.toNBT());
            }
            
            rootTag.put("contracts", contractsList);
            NbtIo.write(rootTag, saveFile);
            
            this.needsSave = false;
            this.lastSaveTime = System.currentTimeMillis();
            System.out.println("[BLACKSOULS] Contract data saved on server for player: " + owner.getScoreboardName());
        } catch (IOException e) {
            System.err.println("Failed to save contracts to file: " + saveFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    /**
     * 强制保存（用于服务器关闭等场景）
     */
    public void forceSave() {
        saveToFile();
    }
    
    /**
     * 强制重新加载契约数据
     */
    public void forceReload() {
        // 重新加载数据文件
        loadFromFile();
        System.out.println("[BLACKSOULS] Contract data reloaded for player: " + owner.getScoreboardName());
    }
    
    /**
     * 重新激活所有契约效果
     */
    public void activateAllEffects(Player player) {
        for (Contract contract : contracts.values()) {
            // 重新激活所有契约效果
            contract.activateEffects(player);
        }
        System.out.println("[BLACKSOULS] All contract effects reactivated for player: " + player.getScoreboardName());
    }
    
    /**
     * 重新激活之前已激活的契约效果（不激活未激活的契约）
     */
    public void reactivateActiveEffects(Player player) {
        for (Contract contract : contracts.values()) {
            // 只重新激活之前已激活的契约效果
            contract.reactivateActiveEffects(player);
        }
        System.out.println("[BLACKSOULS] Previously active contract effects reactivated for player: " + player.getScoreboardName());
    }
    
    /**
     * 获取所有契约
     */
    public Collection<Contract> getAllContracts() {
        return Collections.unmodifiableCollection(contracts.values());
    }
    
    /**
     * 获取契约数量
     */
    public int getContractCount() {
        return contracts.size();
    }
    
    /**
     * 获取契约所有者（玩家）
     */
    public Player getOwner() {
        return owner;
    }
    
    /**
     * 更新所有实体的位置（如果实体仍在世界中）
     * 区块加载时实时更新目标位置，区块卸载时保存最后位置
     */
    public void updateAllEntityPositions() {
        if (owner.level() != null) {
            // 遍历所有契约
            for (Contract contract : contracts.values()) {
                // 检查契约实体所在维度是否与玩家相同
                if (contract.getDimension().equals(owner.level().dimension().location().toString())) {
                    // 获取契约实体的区块坐标
                    Vec3 posVec = contract.getEntityPosition();
                    BlockPos entityPos = BlockPos.containing(posVec);
                    if (entityPos != null) {
                        int chunkX = entityPos.getX() >> 4; // 转换为区块坐标 X
                        int chunkZ = entityPos.getZ() >> 4; // 转换为区块坐标 Z
                        
                        // 检查实体所在区块是否已加载
                        if (owner.level().hasChunk(chunkX, chunkZ)) {
                            // 区块已加载，标记位置可更新
                            contract.setCanUpdatePosition(true);
                            
                            // 尝试通过UUID获取实体并更新位置
                            try {
                                // 使用正确的方式通过UUID获取实体
                                net.minecraft.world.entity.Entity entity = null;
                                if (owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                    // 服务器端：通过UUID获取实体
                                    entity = serverLevel.getEntity(contract.getEntityId());
                                }
                                
                                if (entity != null) {
                                    // 找到匹配的实体，更新契约中的位置信息（使用精确坐标）
                                    Vec3 newPos = entity.position();
                                    contract.setEntityPosition(newPos);
                                    markForSave(); // 标记需要保存
                                }
                            } catch (Exception e) {
                                // 忽略任何异常，保持原位置信息
                                // 这是为了避免在访问受限或出现问题时导致崩溃
                            }
                        } else {
                            // 区块未加载，保持最后保存的位置
                            contract.setCanUpdatePosition(false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 实体移动事件处理 - 实时更新契约坐标
     */
    private void onEntityMoved(Events.EntityMoveEvent event) {
        if (owner.level() != null) {
            // 检查移动的实体是否在玩家的契约列表中
            UUID movedEntityId = event.getEntity().getUUID();
            Contract contract = contracts.get(movedEntityId);
            
            if (contract != null) {
                // 检查实体是否在相同维度
                String currentDimension = owner.level().dimension().location().toString();
                if (contract.getDimension().equals(currentDimension)) {
                    // 实时更新契约中的实体位置
                    contract.setEntityPosition(event.getNewPos());
                    contract.setCanUpdatePosition(true);
                    
                    // 标记需要保存
                    markForSave();

                }
            }
        }
    }
    
    /**
     * 实体死亡事件处理方法，移除死亡实体的契约
     */
    private void onEntityDeath(Events.EntityDeathEvent event) {
        UUID entityUuid = event.getEntity().getUUID();
        
        // 检查是否有该实体的契约
        if (contracts.containsKey(entityUuid)) {
            // 获取契约信息
            Contract contract = contracts.get(entityUuid);
            
            // 在移除契约前，先停用所有契约效果
            contract.deactivateEffects(owner);
            
            // 移除契约
            contracts.remove(entityUuid);
            
            // 获取死亡坐标
            net.minecraft.world.phys.Vec3 pos = event.getEntity().position();
            String coordinates = String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z);
            
            // 解析实体名称（可能为JSON格式）
            String entityName = contract.getEntityName();
            Component displayName;
            try {
                // 尝试解析为JSON格式的Component
                displayName = Component.Serializer.fromJson(entityName);
            } catch (Exception e) {
                // 如果不是JSON格式，直接使用字符串
                displayName = Component.literal(entityName);
            }
            
            // 向玩家发送聊天消息通知目标死亡
            owner.sendSystemMessage(Component.literal("§c您位于 ").append(Component.literal(coordinates).withStyle(ChatFormatting.BOLD))
                .append("§c 的契约对象【").append(displayName.copy().withStyle(ChatFormatting.BOLD))
                .append("§c】已死亡，契约消失"));
            
            // 标记需要保存
            markForSave();
        }
    }
    
    /**
     * 检查并移除实体已消失的契约
     * 当契约对象被捕捉类模组变成物品或其他方式消失时调用
     * 注意：区块卸载或跨维度时实体只是暂时不可访问，不会移除契约
     */
    public void checkAndRemoveVanishedEntityContracts() {
        if (owner.level() == null) {
            return;
        }
        
        List<UUID> contractsToRemove = new ArrayList<>();
        
        for (Contract contract : contracts.values()) {
            UUID entityId = contract.getEntityId();
            
            // 检查实体是否仍然存在
            boolean entityExists = false;
            
            // 尝试在当前维度中通过UUID查找实体
            try {
                // 使用正确的方式通过UUID获取实体
                Entity entity = null;
                if (owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // 服务器端：通过UUID获取实体
                    entity = serverLevel.getEntity(entityId);
                }
                
                if (entity != null) {
                    entityExists = true;
                    // 如果实体存在，重置跨维度检测状态
                    contract.setLastCrossDimensionCheckTime(0);
                } else {
                    // 实体在当前维度不存在，检查是否是跨维度或区块卸载导致的暂时不可访问
                    
                    // 首先检查实体是否在其他维度存在
                    boolean entityExistsInOtherDimension = false;
                    net.minecraft.server.level.ServerLevel entityLevel = null;
                    
                    if (owner.level() instanceof net.minecraft.server.level.ServerLevel currentServerLevel) {
                        net.minecraft.server.MinecraftServer server = currentServerLevel.getServer();
                        if (server != null) {
                            // 检查所有维度中是否存在该实体
                            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                                Entity foundEntity = level.getEntity(entityId);
                                if (foundEntity != null) {
                                    entityExistsInOtherDimension = true;
                                    entityLevel = level;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (entityExistsInOtherDimension) {
                        // 实体在其他维度存在，只是跨维度了
                        long currentTime = System.currentTimeMillis();
                        long lastCheckTime = contract.getLastCrossDimensionCheckTime();
                        
                        // 如果是第一次检测到跨维度，或者距离上次检测超过30秒，才记录日志
                        if (lastCheckTime == 0 || currentTime - lastCheckTime > 30000) {
                            //System.out.println("契约对象在其他维度存在（跨维度），保持契约: " + contract.getEntityName());
                            contract.setLastCrossDimensionCheckTime(currentTime);
                        }
                        
                        // 更新实体位置信息（如果实体在其他维度）
                        if (entityLevel != null) {
                            Entity foundEntity = entityLevel.getEntity(entityId);
                            if (foundEntity != null) {
                                contract.setEntityPosition(foundEntity.position());
                                markForSave();
                            }
                        }
                    } else {
                        // 实体在所有维度都不存在，检查是否是区块卸载导致的暂时不可访问
                        // 获取契约实体的区块坐标
                        Vec3 posVec = contract.getEntityPosition();
                        BlockPos entityPos = BlockPos.containing(posVec);
                        if (entityPos != null) {
                            int chunkX = entityPos.getX() >> 4; // 转换为区块坐标 X
                            int chunkZ = entityPos.getZ() >> 4; // 转换为区块坐标 Z
                            
                            // 检查实体所在区块是否已加载（在实体原始维度中检查）
                            String contractDimension = contract.getDimension();
                            boolean chunkLoaded = false;
                            
                            if (owner.level() instanceof net.minecraft.server.level.ServerLevel currentServerLevel) {
                                net.minecraft.server.MinecraftServer server = currentServerLevel.getServer();
                                if (server != null) {
                                    // 在实体原始维度中检查区块加载状态
                                    for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                                        if (level.dimension().location().toString().equals(contractDimension)) {
                                            chunkLoaded = level.hasChunk(chunkX, chunkZ);
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            if (chunkLoaded) {
                                // 区块已加载但实体在所有维度都不存在，说明实体真正消失了
                                // 这可能是被捕捉类模组变成物品或其他方式移除
                                //System.out.println("检测到契约对象真正消失（区块已加载但实体不存在），移除契约: " + contract.getEntityName());
                                contractsToRemove.add(entityId);
                            } else {
                                // 区块未加载，实体只是暂时不可访问，不移除契约
                                //System.out.println("契约对象所在区块未加载，保持契约: " + contract.getEntityName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略任何异常，保持原位置信息
                // 这是为了避免在访问受限或出现问题时导致崩溃
                System.out.println("检查实体存在性时发生异常，保持契约: " + contract.getEntityName());
            }
        }
        
        // 移除已消失实体的契约
        for (UUID entityId : contractsToRemove) {
            Contract contract = contracts.get(entityId);
            if (contract != null) {
                // 停用契约效果
                contract.deactivateEffects(owner);
                
                // 移除契约
                contracts.remove(entityId);
                
                // 获取最后的坐标
                Vec3 lastPos = contract.getEntityPosition();
                String coordinates = String.format("%.1f, %.1f, %.1f", lastPos.x, lastPos.y, lastPos.z);
                
                // 解析实体名称（可能为JSON格式）
                String entityName = contract.getEntityName();
                Component displayName;
                try {
                    // 尝试解析为JSON格式的Component
                    displayName = Component.Serializer.fromJson(entityName);
                } catch (Exception e) {
                    // 如果不是JSON格式，直接使用字符串
                    displayName = Component.literal(entityName);
                }
                
                // 向玩家发送通知，附带最后的坐标
                owner.sendSystemMessage(Component.literal("§c您位于 ").append(Component.literal(coordinates).withStyle(ChatFormatting.BOLD))
                    .append("§c 的契约对象【").append(displayName.copy().withStyle(ChatFormatting.BOLD))
                    .append("§c】已消失或跨纬度传送发生意外，契约自动解除"));
                
                // 向客户端发送契约删除通知（仅在服务器端执行）
                if (owner.level() != null && !owner.level().isClientSide() && owner instanceof net.minecraft.server.level.ServerPlayer) {
                    ContractNetworkHandler.broadcastContractUpdate((net.minecraft.server.level.ServerPlayer) owner);
                }
                
                //System.out.println("已移除消失实体的契约: " + contract.getEntityName());
            }
        }
        
        // 如果有契约被移除，标记需要保存
        if (!contractsToRemove.isEmpty()) {
            markForSave();
        }
    }
}