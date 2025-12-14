package com.iamalittle.black_souls_options.contracts;

import net.minecraft.core.BlockPos;
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
        
        // 构建保存文件路径：存档路径/playerdata/contracts/玩家uuid.dat
        File worldDir = player.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
        File contractsDir = new File(worldDir, "contracts");
        this.saveFile = new File(contractsDir, player.getUUID().toString() + ".dat");
        
        // 确保目录存在
        if (!contractsDir.exists()) {
            contractsDir.mkdirs();
        }
        
        // 加载现有数据
        loadFromFile();
        
        // 注册实体移动事件监听器，实现实时坐标更新
        Events.EntityMoved.add(this::onEntityMoved);
        
        // 注册实体死亡事件监听器，实现目标死亡时移除契约
        Events.EntityDeath.add(this::onEntityDeath);
    }
    
    /**
     * 创建新契约
     */
    public void createContract(UUID entityUuid, String entityType, String entityName, Vec3 position, String dimension) {
        Contract contract = new Contract(entityUuid, entityType, entityName, position, dimension);
        
        // 为契约添加对应的效果
        addEffectsToContract(contract, entityType);
        
        contracts.put(entityUuid, contract);
        markForSave();
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
     * 为契约添加对应的效果
     */
    private void addEffectsToContract(Contract contract, String entityType) {
        // 从注册表获取实体类型对应的效果
        List<ContractEffect> effects = ContractEffectRegistry.getInstance().getEffectsForEntityType(entityType);
        
        for (ContractEffect effect : effects) {
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
        for (Contract contract : contracts.values()) {
            contract.tickEffects(owner);
        }
    }
    
    /**
     * 从文件加载契约数据
     */
    private void loadFromFile() {
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
                                } else {
                                    // 客户端：使用更通用的方法获取实体
                                    // 注意：在客户端，可能需要使用其他方式获取实体
                                    // 这里简化处理，主要依赖实体移动事件来实时更新
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
            
            // 向玩家发送聊天消息通知目标死亡
            owner.sendSystemMessage(Component.literal("您位于 ").append(Component.literal(coordinates).withStyle(style -> style.withBold(true)))
                .append(" 的契约对象【").append(Component.literal(event.getEntity().getName().getString()).withStyle(style -> style.withBold(true)))
                .append("】已死亡，契约消失"));
            
            // 标记需要保存
            markForSave();
        }
    }
}