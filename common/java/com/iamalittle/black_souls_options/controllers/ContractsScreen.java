package com.iamalittle.black_souls_options.controllers;

import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import com.iamalittle.black_souls_options.network.ContractSyncPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 契约界面，用于显示玩家的契约列表和实体实时位置
 */
public class ContractsScreen extends Screen {
    private ContractManager contractManager;
    private List<Contract> sortedContracts;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL_MS = 500; // 500毫秒更新一次位置
    private Contract selectedContract;
    private Button trackButton;
    private Button deleteButton;
    private EditBox searchBox;
    private int scrollOffset;
    private int maxVisibleItems;
    private int effectDetailsScrollOffset; // 效果详情区域滚动偏移量
    private String searchFilter = ""; // 搜索过滤条件
    private long lastScrollTime = 0; // 文本滚动时间记录
    private boolean needsRefresh = false; // 标记是否需要刷新界面
    private long lastContractStateCheckTime = 0; // 上次检查契约状态的时间
    private static final long CONTRACT_STATE_CHECK_INTERVAL_MS = 100; // 100毫秒检查一次契约状态
    
    public ContractsScreen() {
        super(Component.translatable("black_souls_options.contracts_screen.title"));
        this.contractManager = ContractManagerHelper.getAppropriateContractManager(Minecraft.getInstance().player);
        this.sortedContracts = new ArrayList<>();
        this.lastUpdateTime = System.currentTimeMillis();
        this.selectedContract = null;
        this.scrollOffset = 0;
        this.maxVisibleItems = 0; // 初始化为0，在render方法中动态计算
        this.effectDetailsScrollOffset = 0;
        this.lastContractStateCheckTime = System.currentTimeMillis();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 更新契约列表
        updateContractList();
        
        // 添加搜索框（在左下角，与关闭按钮齐平）
        int searchBoxWidth = 100;
        int searchBoxHeight = 16;
        int searchBoxX = 20; // 左侧对齐
        int searchBoxY = this.height - 30; // 与关闭按钮齐平

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, 
            Component.literal(""));
        this.searchBox.setHint(Component.translatable("black_souls_options.contracts_screen.search_hint"));
        this.searchBox.setResponder(text -> {
            this.searchFilter = text.trim();
            updateContractList(); // 更新列表以应用过滤
        });
        this.addRenderableWidget(this.searchBox);
        
        // 添加关闭按钮
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - 30;
        
        Button closeButton = Button.builder(
            Component.translatable("black_souls_options.contracts_screen.close_button"),
            button -> this.onClose()
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
        
        this.addRenderableWidget(closeButton);
        
        // 添加追踪按钮（初始位置设为(0,0)，在render方法中动态定位）
        this.trackButton = Button.builder(
            Component.translatable("black_souls_options.contracts_screen.track_button"),
            button -> {
                if (selectedContract != null) {
                    selectedContract.setTracking(!selectedContract.isTracking());
                    updateButtonText(button);
                }
            }
        ).bounds(0, 0, 80, 14).build(); // 使用与文本区域更匹配的高度
        this.addRenderableWidget(this.trackButton);
        
        // 添加删除按钮（初始位置设为(0,0)，在render方法中动态定位）
        this.deleteButton = Button.builder(
            Component.translatable("black_souls_options.contracts_screen.delete_button"),
            button -> {
                if (selectedContract != null && contractManager != null) {
                    contractManager.removeContract(selectedContract.getEntityId());
                    updateContractList();
                    selectedContract = null;
                }
            }
        ).bounds(0, 0, 100, 20).build();
        this.addRenderableWidget(this.deleteButton);
    }
    
    /**
     * 更新追踪按钮的文本
     */
    private void updateButtonText(Button button) {
        if (selectedContract != null) {
            String translationKey = selectedContract.isTracking() ? 
                "black_souls_options.contracts_screen.untrack_button" : 
                "black_souls_options.contracts_screen.track_button";
            button.setMessage(Component.translatable(translationKey));
        }
    }
    
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景（半透明黑色）
        this.renderBackground(guiGraphics);
        
        // 检查并处理契约状态更新
        checkAndHandleContractUpdates();
        
        // 检查是否需要更新契约位置
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL_MS) {
            updateContractList();
            lastUpdateTime = currentTime;
        }
        
        // 绘制契约列表标题（合并为一个标题，删除红色主标题）
        String titleText;
        if (contractManager != null) {
            titleText = "契约列表 (" + sortedContracts.size() + "/" + contractManager.getContractCount() + ")";
        } else {
            titleText = "契约列表 (" + sortedContracts.size() + "/0)";
        }
        guiGraphics.drawString(
            this.font,
            titleText,
            (this.width - this.font.width(titleText)) / 2,
            15, // 标题位置上移到距离顶部15像素
            0xFFFFFF
        );
        
        // 绘制选中契约的详细信息
        int detailY = 30; // 详细信息位置上移，距离标题10像素
        int detailX = 20;
        
        // 绘制详细信息背景（缩小高度）
        guiGraphics.fill(detailX, detailY, this.width - 20, detailY + 16, 0xCC333333);
        guiGraphics.fill(detailX, detailY, this.width - 20, detailY + 1, 0xFFFFFF); // 上边框
        guiGraphics.fill(detailX, detailY + 15, this.width - 20, detailY + 16, 0xFFFFFF); // 下边框
        
        if (selectedContract != null) {
            Vec3 pos = selectedContract.getEntityPosition();
            BlockPos blockPos = BlockPos.containing(pos);
            
            // 解析实体名称（处理JSON格式）
            Component nameComponent;
            try {
                // 尝试将实体名称解析为JSON格式的Component（保留所有样式）
                Component baseComponent = Component.Serializer.fromJson(selectedContract.getEntityName());
                nameComponent = Component.empty().append(baseComponent);
            } catch (Exception e) {
                // 如果不是JSON格式，使用普通文本处理
                nameComponent = Component.literal(selectedContract.getEntityName());
            }
            
            // 构建位置和维度文本
            Component detailText;
            if (selectedContract.isCommandCreated()) {
                // 如果是指令创建的契约，添加特殊标识
                detailText = Component.translatable("black_souls_options.contracts_screen.target_info_command",
                    nameComponent, 
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(), 
                    selectedContract.getDimension());
            } else {
                // 普通契约
                detailText = Component.translatable("black_souls_options.contracts_screen.target_info",
                    nameComponent, 
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(), 
                    selectedContract.getDimension());
            }
            
            // 计算文本垂直居中的Y坐标
            int textHeight = this.font.lineHeight;
            int backgroundHeight = 16;
            int centeredY = detailY + (backgroundHeight - textHeight) / 2;
            
            // 定位追踪按钮在详细信息区域右侧（垂直居中）
            int buttonWidth = 80;
            // 获取按钮的实际高度（不要硬编码，使用按钮的实际尺寸）
            int buttonHeight = trackButton.getHeight();
            int trackButtonX = this.width - 20 - buttonWidth - 5;
            int trackButtonY = detailY + (16 - buttonHeight) / 2; // 按钮垂直居中
            
            // 计算文本的最大可用宽度（避免与追踪按钮重叠）
            int maxTextWidth = trackButtonX - detailX - 10; // 留出5像素的边距
            
            // 检查文本是否过长，如果过长则启用滚动显示
            String detailString = detailText.getString();
            int textWidth = this.font.width(detailString);
            
            if (textWidth > maxTextWidth) {
                // 文本过长，启用滚动显示
                if (lastScrollTime == 0) {
                    lastScrollTime = currentTime;
                }
                
                // 计算滚动偏移量（平滑滚动）
                long scrollDuration = 5000; // 5秒完成一个完整滚动周期
                int totalScrollDistance = textWidth + maxTextWidth; // 滚动总距离
                int scrollOffset = (int)((currentTime - lastScrollTime) % scrollDuration * totalScrollDistance / scrollDuration);
                
                // 绘制滚动文本
                guiGraphics.enableScissor(detailX + 5, detailY, trackButtonX - 5, detailY + backgroundHeight);
                guiGraphics.drawString(
                    this.font,
                    detailText,
                    detailX + 5 - scrollOffset,
                    centeredY,
                    0xFFFFFF
                );
                guiGraphics.disableScissor();
            } else {
                // 文本不长，正常显示
                guiGraphics.drawString(
                    this.font,
                    detailText,
                    detailX + 5,
                    centeredY,
                    0xFFFFFF
                );
            }
            
            // 更新追踪按钮位置和状态
            if (this.trackButton != null) {
                this.trackButton.setX(trackButtonX);
                this.trackButton.setY(trackButtonY);
                this.trackButton.setWidth(buttonWidth);
                this.trackButton.visible = true;
                updateButtonText(this.trackButton);
            }
            
            // 隐藏详细信息区域的删除按钮
            if (this.deleteButton != null) {
                this.deleteButton.visible = false;
            }
        } else {
            // 显示提示信息（垂直居中）
            int textHeight = this.font.lineHeight;
            int backgroundHeight = 16;
            int centeredY = detailY + (backgroundHeight - textHeight) / 2;
            
            guiGraphics.drawString(
                this.font,
                Component.translatable("black_souls_options.contracts_screen.select_prompt").getString(),
                detailX + 5,
                centeredY,
                0xAAAAAA
            );
            
            // 隐藏追踪和删除按钮
            if (this.trackButton != null) {
                this.trackButton.visible = false;
            }
            if (this.deleteButton != null) {
                this.deleteButton.visible = false;
            }
        }
        
        // 绘制选中契约的效果详细信息
        if (selectedContract != null) {
            drawEffectDetails(guiGraphics);
        }
        
        // 绘制契约列表区域（固定位置，不再随选择契约而自动下移）
        int listX = 20;
        int listY = 121; // 效果详情区域下方（51+65+5=121），与上方保持5像素间隔
        int listWidth = this.width - 40;
        int listHeight = Math.max(100, this.height - listY - 30); // 确保列表高度至少100像素，距离底部30像素
        
        // 绘制列表背景
        guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0xCC000000);
        
        // 绘制边框
        guiGraphics.fill(listX, listY, listX + listWidth, listY + 1, 0xFFFFFF); // 上边框
        guiGraphics.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFFFFFF); // 下边框
        guiGraphics.fill(listX, listY, listX + 1, listY + listHeight, 0xFFFFFF); // 左边框
        guiGraphics.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFFFFFF); // 右边框
        
        // 绘制契约列表
        int itemHeight = font.lineHeight + 8;
        int currentY = listY + 5;
        
        // 动态计算最大可见项目数量，使用调整后的列表项高度
        maxVisibleItems = listHeight / Math.max(itemHeight, font.lineHeight * 2 + 12);
        
        if (sortedContracts.isEmpty()) {
            // 显示没有契约的提示
            String noContractsText = "暂无契约";
            guiGraphics.drawString(
                this.font,
                noContractsText,
                (this.width - this.font.width(noContractsText)) / 2,
                listY + listHeight / 2 - font.lineHeight / 2,
                0xAAAAAA
            );
        } else {
            // 计算可见项目范围
            int startIndex = Math.max(0, scrollOffset);
            int endIndex = Math.min(sortedContracts.size(), startIndex + maxVisibleItems);
            
            // 绘制滚动条背景
            int scrollBarWidth = 6;
            int scrollBarX = listX + listWidth - scrollBarWidth - 2;
            int scrollBarY = listY + 2;
            int scrollBarHeight = listHeight - 4;
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight, 0xCC333333);
            
            // 绘制滚动条滑块
            if (sortedContracts.size() > maxVisibleItems) {
                int scrollBarThumbHeight = Math.max(10, scrollBarHeight * maxVisibleItems / sortedContracts.size());
                int scrollBarThumbY = scrollBarY + (scrollBarHeight - scrollBarThumbHeight) * scrollOffset / Math.max(1, sortedContracts.size() - maxVisibleItems);
                guiGraphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + scrollBarWidth, scrollBarThumbY + scrollBarThumbHeight, 0xCC888888);
            }
            
            // 绘制契约项目
            for (int i = startIndex; i < endIndex; i++) {
                Contract contract = sortedContracts.get(i);
                Vec3 posVec = contract.getEntityPosition();
                BlockPos pos = BlockPos.containing(posVec);
                
                // 调整列表项高度以容纳所有内容（名称、位置、两个按钮）
                int adjustedItemHeight = Math.max(itemHeight, font.lineHeight * 2 + 12); // 确保足够高度容纳两行文本
                
                // 检查鼠标是否悬停
                boolean isMouseOver = mouseX >= listX && mouseX <= listX + listWidth - scrollBarWidth - 5 && 
                                    mouseY >= currentY && mouseY <= currentY + adjustedItemHeight;
                
                // 绘制背景（选中状态）
                if (contract == selectedContract || isMouseOver) {
                    guiGraphics.fill(listX + 2, currentY, listX + listWidth - scrollBarWidth - 7, currentY + adjustedItemHeight, 0xCC555555);
                }
                
                // 绘制实体名称（保留原有颜色，添加加粗效果）
                Component nameComponent;
                try {
                    // 尝试将实体名称解析为JSON格式的Component（保留所有样式）
                    Component baseComponent = Component.Serializer.fromJson(contract.getEntityName());
                    // 使用新的Component包装并添加加粗效果
                    nameComponent = Component.empty().append(baseComponent).withStyle(style -> style.withBold(true));
                } catch (Exception e) {
                    // 如果不是JSON格式，使用普通文本处理
                    nameComponent = Component.literal(contract.getEntityName()).withStyle(style -> style.withBold(true));
                }
                
                // 绘制实体名称
                guiGraphics.drawString(font, nameComponent, listX + 10, currentY + 3, 0xFFFFFF);
                
                // 如果是指令创建的契约，在名称旁边添加特殊标识
                if (contract.isCommandCreated()) {
                    Component commandCreatedText = Component.translatable("black_souls_options.contracts_screen.command_created_text");
                    guiGraphics.drawString(
                        font,
                        commandCreatedText,
                        listX + 10 + font.width(nameComponent) + 5, // 名称右侧5像素
                        currentY + 3,
                        0xFFAA00 // 橙色标识
                    );
                }
                
                // 绘制实体位置（在名称下方）
                String positionText = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
                
                // 绘制实体位置
                guiGraphics.drawString(
                    font,
                    positionText,
                    listX + 10, // 与名称左对齐
                    currentY + 3 + font.lineHeight, // 名称下方
                    0xAAAAAA
                );
                
                // 绘制开关按钮（在删除按钮左侧）
                int toggleBtnWidth = 40;
                int toggleBtnHeight = 14;
                int toggleBtnX = listX + listWidth - scrollBarWidth - toggleBtnWidth - 5 - 45; // 删除按钮左侧留5像素间距
                int toggleBtnY = currentY + (adjustedItemHeight - toggleBtnHeight) / 2;
                
                // 检查鼠标是否悬停在开关按钮上
                boolean isToggleBtnHovered = mouseX >= toggleBtnX && mouseX <= toggleBtnX + toggleBtnWidth && 
                                           mouseY >= toggleBtnY && mouseY <= toggleBtnY + toggleBtnHeight;
                
                // 获取契约效果状态
                boolean isEffectActive = !contract.getEffects().isEmpty() && contract.getEffects().get(0).isActive();
                
                // 修复逻辑：激活状态显示红色"关闭"，未激活状态显示绿色"开启"
                int toggleBtnColor = isEffectActive ? 
                    (isToggleBtnHovered ? 0xCCAA0000 : 0xCC550000) :   // 激活状态：红色
                    (isToggleBtnHovered ? 0xCC00AA00 : 0xCC005500);     // 未激活状态：绿色
                
                // 绘制开关按钮背景
                guiGraphics.fill(toggleBtnX, toggleBtnY, toggleBtnX + toggleBtnWidth, toggleBtnY + toggleBtnHeight, 
                               toggleBtnColor);
                
                // 绘制开关按钮文字
                Component toggleText = isEffectActive ? 
                    Component.translatable("black_souls_options.contracts_screen.toggle_button_off") : 
                    Component.translatable("black_souls_options.contracts_screen.toggle_button_on");
                guiGraphics.drawString(
                    font,
                    toggleText,
                    toggleBtnX + (toggleBtnWidth - font.width(toggleText)) / 2,
                    toggleBtnY + (toggleBtnHeight - font.lineHeight) / 2,
                    0xFFFFFF
                );
                
                // 绘制删除按钮
                int deleteBtnWidth = 40;
                int deleteBtnHeight = 14;
                int deleteBtnX = listX + listWidth - scrollBarWidth - deleteBtnWidth - 5;
                int deleteBtnY = currentY + (adjustedItemHeight - deleteBtnHeight) / 2;
                
                // 检查鼠标是否悬停在删除按钮上
                boolean isDeleteBtnHovered = mouseX >= deleteBtnX && mouseX <= deleteBtnX + deleteBtnWidth && 
                                           mouseY >= deleteBtnY && mouseY <= deleteBtnY + deleteBtnHeight;
                
                // 绘制删除按钮背景
                guiGraphics.fill(deleteBtnX, deleteBtnY, deleteBtnX + deleteBtnWidth, deleteBtnY + deleteBtnHeight, 
                               isDeleteBtnHovered ? 0xCCAA0000 : 0xCC550000);
                
                // 绘制删除按钮文字
                Component deleteText = Component.translatable("black_souls_options.contracts_screen.delete_button_text");
                guiGraphics.drawString(
                    font,
                    deleteText,
                    deleteBtnX + (deleteBtnWidth - font.width(deleteText)) / 2,
                    deleteBtnY + (deleteBtnHeight - font.lineHeight) / 2,
                    0xFFFFFF
                );
                
                // 如果是当前选中的契约，绘制指示标记
                if (contract == selectedContract) {
                    guiGraphics.drawString(
                        font,
                        ">",
                        listX + 2,
                        currentY + (adjustedItemHeight - font.lineHeight) / 2,
                        0x55FF55
                    );
                }
                
                currentY += adjustedItemHeight;
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理契约列表点击
        int listX = 20;
        int listY = 121; // 与render方法中的listY保持一致（固定位置）
        int listWidth = this.width - 40;
        int listHeight = this.height - 141; // 与render方法中的listHeight保持一致（固定高度）
        int scrollBarWidth = 6;
        
        if (mouseX >= listX && mouseX <= listX + listWidth && 
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int baseItemHeight = font.lineHeight + 8;
            int adjustedItemHeight = Math.max(baseItemHeight, font.lineHeight * 2 + 12); // 与render方法中保持一致
            int relativeY = (int) (mouseY - listY);
            int clickedIndex = scrollOffset + relativeY / adjustedItemHeight;
            
            if (clickedIndex >= 0 && clickedIndex < sortedContracts.size()) {
                // 计算当前项的Y坐标
                int currentY = listY + (clickedIndex - scrollOffset) * adjustedItemHeight;
                
                // 检查是否点击了开关按钮
                int toggleBtnWidth = 40;
                int toggleBtnHeight = 14;
                int toggleBtnX = listX + listWidth - scrollBarWidth - toggleBtnWidth - 5 - 45;
                int toggleBtnY = currentY + (adjustedItemHeight - toggleBtnHeight) / 2;
                
                if (mouseX >= toggleBtnX && mouseX <= toggleBtnX + toggleBtnWidth && 
                    mouseY >= toggleBtnY && mouseY <= toggleBtnY + toggleBtnHeight) {
                    // 点击了开关按钮
                    Contract contract = sortedContracts.get(clickedIndex);
                    List<ContractEffect> effects = contract.getEffects();
                    
                    if (!effects.isEmpty()) {
                        ContractEffect effect = effects.get(0);
                        if (effect.isActive()) {
                            // 停用效果
                            effect.deactivate(Minecraft.getInstance().player);
                            // 关键修复：向服务器发送状态同步请求
                            com.iamalittle.black_souls_options.network.ContractNetworkHandler.sendEffectToggleRequest(contract.getEntityId(), false);
                        } else {
                            // 激活效果（玩家点击开关时发送消息）
                            effect.activate(Minecraft.getInstance().player, true);
                            // 关键修复：向服务器发送状态同步请求
                            com.iamalittle.black_souls_options.network.ContractNetworkHandler.sendEffectToggleRequest(contract.getEntityId(), true);
                        }
                        
                        // 关键修复：立即更新界面显示，确保按钮状态实时变化
                        // 强制重新渲染界面，让按钮颜色立即更新
                        this.updateContractList();
                    }
                    return true;
                }
                
                // 检查是否点击了删除按钮
                int deleteBtnWidth = 40;
                int deleteBtnHeight = 14;
                int deleteBtnX = listX + listWidth - scrollBarWidth - deleteBtnWidth - 5;
                int deleteBtnY = currentY + (adjustedItemHeight - deleteBtnHeight) / 2;
                
                if (mouseX >= deleteBtnX && mouseX <= deleteBtnX + deleteBtnWidth && 
                    mouseY >= deleteBtnY && mouseY <= deleteBtnY + deleteBtnHeight) {
                    // 点击了删除按钮
                    Contract contract = sortedContracts.get(clickedIndex);
                    
                    // 在客户端静默停用效果（不发送停用消息，避免重复）
                    contract.deactivateEffectsSilently(Minecraft.getInstance().player);
                    
                    // 在客户端删除契约（静默删除，不发送消息）
                    contractManager.removeContract(contract.getEntityId());
                    
                    // 向服务器发送删除请求（服务器端会发送停用消息）
                    com.iamalittle.black_souls_options.network.ContractNetworkHandler.sendContractDeleteRequest(contract.getEntityId());
                    
                    // 如果删除的是当前选中的契约，清除选中状态
                    if (contract == selectedContract) {
                        selectedContract = null;
                    }
                    
                    // 更新契约列表
                    updateContractList();
                    return true;
                } else {
                    // 点击了契约项，选择契约
                    selectedContract = sortedContracts.get(clickedIndex);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        // 处理契约列表鼠标滚轮滚动
        int listX = 20;
        int listY = 121; // 与render方法中的listY保持一致（固定位置）
        int listWidth = this.width - 40;
        int listHeight = this.height - 141; // 与render方法中的listHeight保持一致（固定高度）
        
        if (mouseX >= listX && mouseX <= listX + listWidth && 
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            if (scrollDelta > 0) {
                // 向上滚动
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (scrollDelta < 0) {
                // 向下滚动
                scrollOffset = Math.min(Math.max(0, sortedContracts.size() - maxVisibleItems), scrollOffset + 1);
            }
            return true;
        }
        
        // 处理效果详情区域鼠标滚轮滚动
        int detailX = 20;
        int detailY = 55;
        int detailWidth = this.width - 40;
        int effectDetailsHeight = 70; // 固定高度70像素
        
        if (mouseX >= detailX && mouseX <= detailX + detailWidth && 
            mouseY >= detailY && mouseY <= detailY + effectDetailsHeight) {
            
            // 计算内容总高度
            int contentHeight = calculateEffectDetailsHeight();
            int maxVisibleHeight = effectDetailsHeight - 10;
            
            if (scrollDelta > 0) {
                // 向上滚动
                effectDetailsScrollOffset = Math.max(0, effectDetailsScrollOffset - font.lineHeight);
            } else if (scrollDelta < 0) {
                // 向下滚动
                int maxScroll = Math.max(0, contentHeight - maxVisibleHeight);
                effectDetailsScrollOffset = Math.min(maxScroll, effectDetailsScrollOffset + font.lineHeight);
            }
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }
    
    /**
     * 更新契约列表
     */
    private void updateContractList() {
        if (contractManager != null) {
            // 保存当前选中的契约（如果有）
            Contract previouslySelected = selectedContract;
            
            sortedContracts.clear();
            
            // 获取所有契约
            Collection<Contract> allContracts = contractManager.getAllContracts();
            
            // 应用搜索过滤
            if (searchFilter != null && !searchFilter.isEmpty()) {
                for (Contract contract : allContracts) {
                    // 检查契约名称是否匹配
                    boolean nameMatches = contract.getEntityName().toLowerCase().contains(searchFilter.toLowerCase());
                    
                    // 检查契约效果详细信息是否匹配
                    boolean effectMatches = false;
                    List<ContractEffect> effects = contract.getEffects();
                    for (ContractEffect effect : effects) {
                        // 检查效果名称
                        if (effect.getDisplayName().toLowerCase().contains(searchFilter.toLowerCase())) {
                            effectMatches = true;
                            break;
                        }
                        
                        // 检查效果描述
                        if (effect.getDescription().toLowerCase().contains(searchFilter.toLowerCase())) {
                            effectMatches = true;
                            break;
                        }
                        
                        // 检查效果详细信息
                        List<Component> effectDetails = effect.getEffectDetails();
                        for (Component detail : effectDetails) {
                            if (detail.getString().toLowerCase().contains(searchFilter.toLowerCase())) {
                                effectMatches = true;
                                break;
                            }
                        }
                        
                        if (effectMatches) break;
                    }
                    
                    // 如果名称或效果匹配，则添加到列表中
                    if (nameMatches || effectMatches) {
                        sortedContracts.add(contract);
                    }
                }
            } else {
                // 没有搜索过滤，添加所有契约
                sortedContracts.addAll(allContracts);
            }
            
            // 按创建时间排序
            sortedContracts.sort((c1, c2) -> Long.compare(c2.getCreationTime(), c1.getCreationTime()));
            
            // 恢复选中的契约（如果仍然存在）
            if (previouslySelected != null) {
                for (Contract contract : sortedContracts) {
                    if (contract.getEntityId().equals(previouslySelected.getEntityId())) {
                        selectedContract = contract;
                        break;
                    }
                }
            }
            
            // 确保滚动位置有效
            scrollOffset = Math.min(scrollOffset, Math.max(0, sortedContracts.size() - maxVisibleItems));
        }
    }
    
    /**
     * 计算效果详情内容的总高度
     */
    private int calculateEffectDetailsHeight() {
        if (selectedContract == null) return 0;
        
        List<ContractEffect> effects = selectedContract.getEffects();
        int totalHeight = font.lineHeight + 20; // 标题高度和间距
        
        if (effects.isEmpty()) {
            totalHeight += font.lineHeight;
        } else {
            for (ContractEffect effect : effects) {
                totalHeight += font.lineHeight * 2 + 10; // 名称和描述高度
                
                // 详细信息高度
                List<Component> effectDetails = effect.getEffectDetails();
                totalHeight += (font.lineHeight + 2) * effectDetails.size();
                
                totalHeight += 5; // 效果之间的间距
            }
        }
        
        return totalHeight;
    }
    
    /**
     * 绘制契约效果详细信息
     */
    private void drawEffectDetails(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        if (selectedContract == null) return;
        
        int detailX = 20;
        int detailY = 50; // 契约详细信息下方（30+16+5=51），与上方保持5像素间隔
        int detailWidth = this.width - 40;
        int effectDetailsHeight = 70; // 固定高度70像素
        
        // 绘制效果详情区域背景
        guiGraphics.fill(detailX, detailY, detailX + detailWidth, detailY + effectDetailsHeight, 0xCC222222);
        
        // 绘制边框
        guiGraphics.fill(detailX, detailY, detailX + detailWidth, detailY + 1, 0xFFFFFF); // 上边框
        guiGraphics.fill(detailX, detailY + effectDetailsHeight - 1, detailX + detailWidth, detailY + effectDetailsHeight, 0xFFFFFF); // 下边框
        
        // 计算内容总高度
        int contentHeight = calculateEffectDetailsHeight();
        int maxVisibleHeight = effectDetailsHeight - 10;
        boolean needScrollBar = contentHeight > maxVisibleHeight;
        
        // 限制滚动偏移，防止内容超出顶部边界
        if (contentHeight > maxVisibleHeight) {
            int maxScroll = Math.max(0, contentHeight - maxVisibleHeight);
            effectDetailsScrollOffset = Math.min(effectDetailsScrollOffset, maxScroll);
        } else {
            effectDetailsScrollOffset = 0;
        }
        
        // 绘制契约效果列表
        List<ContractEffect> effects = selectedContract.getEffects();
        int currentY = detailY + 5 - effectDetailsScrollOffset; // 应用滚动偏移，包括标题
        
        // 绘制效果详情标题
        Component effectTitle = Component.translatable("black_souls_options.contracts_screen.effect_details_title");
        if (currentY + font.lineHeight >= detailY+10 && currentY < detailY + effectDetailsHeight - 10) {
            guiGraphics.drawString(
                font,
                effectTitle.getString(),
                detailX + 10,
                currentY,
                0x55FF55
            );
        }
        
        currentY += font.lineHeight + 20; // 标题与内容之间的间距
        
        if (effects.isEmpty()) {
            // 没有效果
            String noEffectsText = "该契约暂无效果";
            if (currentY + font.lineHeight >= detailY+10 && currentY < detailY + effectDetailsHeight - 10) {
                guiGraphics.drawString(
                    font,
                    noEffectsText,
                    detailX + 10,
                    currentY,
                    0xAAAAAA
                );
            }
        } else {
            // 显示效果列表
            for (ContractEffect effect : effects) {
                // 效果名称和状态
                Component statusText = effect.isActive() ? 
                    Component.translatable("black_souls_options.contracts_screen.effect_active").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))) : 
                    Component.translatable("black_souls_options.contracts_screen.effect_inactive").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#FF5555")));
                
                Component effectInfo = Component.translatable("black_souls_options.contracts_screen.effect_info", 
                    effect.getDisplayName(), 
                    statusText
                ).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#FFFFFF")));
                
                if (currentY + font.lineHeight >= detailY+10 && currentY < detailY + effectDetailsHeight - 10) {
                    guiGraphics.drawString(
                        font,
                        effectInfo,
                        detailX + 10,
                        currentY,
                        0xFFFFFF
                    );
                }
                
                // 效果描述
                String description = effect.getDescription();
                if (description.length() > 40) {
                    description = description.substring(0, 37) + "...";
                }
                if (currentY + font.lineHeight * 2 + 2 >= detailY+10 && currentY + font.lineHeight < detailY + effectDetailsHeight - 10) {
                    guiGraphics.drawString(
                        font,
                        description,
                        detailX + 10,
                        currentY + font.lineHeight + 2,
                        0xAAAAAA
                    );
                }
                
                currentY += font.lineHeight * 2 + 10;
                
                // 显示效果详细信息
                List<Component> effectDetails = effect.getEffectDetails();
                for (Component detail : effectDetails) {
                    if (currentY + font.lineHeight >= detailY+10 && currentY < detailY + effectDetailsHeight - 10) {
                        guiGraphics.drawString(
                            font,
                            detail,
                            detailX + 20, // 缩进显示详细信息
                            currentY,
                            0xDDDDDD
                        );
                    }
                    currentY += font.lineHeight + 2;
                }
                
                currentY += 5; // 效果之间的间距
            }
        }
        
        // 绘制滚动条（如果需要）
        if (needScrollBar) {
            int scrollBarWidth = 6;
            int scrollBarX = detailX + detailWidth - scrollBarWidth - 5;
            int scrollBarTrackHeight = effectDetailsHeight - 20;
            int scrollBarTrackY = detailY + 10;
            
            // 绘制滚动条轨道
            guiGraphics.fill(scrollBarX, scrollBarTrackY, scrollBarX + scrollBarWidth, scrollBarTrackY + scrollBarTrackHeight, 0x444444);
            
            // 计算滚动条滑块高度
            int sliderHeight = Math.max(10, scrollBarTrackHeight * maxVisibleHeight / contentHeight);
            int sliderY = scrollBarTrackY + (effectDetailsScrollOffset * (scrollBarTrackHeight - sliderHeight)) / Math.max(1, contentHeight - maxVisibleHeight);
            
            // 绘制滚动条滑块
            guiGraphics.fill(scrollBarX, sliderY, scrollBarX + scrollBarWidth, sliderY + sliderHeight, 0x888888);
        }
        
        // 如果内容超出可见区域，在底部中间绘制倒三角符号提示下方有更多内容
        if (needScrollBar && effectDetailsScrollOffset < contentHeight - maxVisibleHeight) {
            int triangleX = detailX + detailWidth / 2 - 3; // 中间位置
            int triangleY = detailY + effectDetailsHeight - 10; // 底部上方10像素
            
            // 绘制倒三角符号 ▼
            guiGraphics.drawString(
                font,
                "▼",
                triangleX,
                triangleY,
                0xAAAAAA
            );
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
    
    // 删除复杂的网络监听器注册代码，使用简单的状态检查机制
    
    /**
     * 检查并处理契约状态更新
     */
    private void checkAndHandleContractUpdates() {
        long currentTime = System.currentTimeMillis();
        
        // 定期检查契约状态是否发生变化
        if (currentTime - lastContractStateCheckTime > CONTRACT_STATE_CHECK_INTERVAL_MS) {
            checkContractStateChanges();
            lastContractStateCheckTime = currentTime;
        }
        
        if (needsRefresh) {
            // 强制刷新契约列表
            updateContractList();
            needsRefresh = false;
        }
    }
    
    /**
     * 检查契约状态是否发生变化
     */
    private void checkContractStateChanges() {
        if (contractManager == null || sortedContracts.isEmpty()) {
            return;
        }
        
        // 检查每个契约的状态是否与界面显示的状态一致
        for (Contract contract : sortedContracts) {
            Contract currentContract = contractManager.getContract(contract.getEntityId());
            if (currentContract != null) {
                // 检查效果数量是否一致
                if (currentContract.getEffects().size() != contract.getEffects().size()) {
                    needsRefresh = true;
                    break;
                }
                
                // 检查每个效果的状态是否发生变化
                for (int i = 0; i < currentContract.getEffects().size(); i++) {
                    ContractEffect currentEffect = currentContract.getEffects().get(i);
                    ContractEffect displayedEffect = contract.getEffects().get(i);
                    
                    if (currentEffect.isActive() != displayedEffect.isActive()) {
                        // 状态发生变化，标记需要刷新
                        needsRefresh = true;
                        break;
                    }
                }
                
                if (needsRefresh) {
                    break; // 发现一个变化就足够触发刷新
                }
            }
        }
    }
}