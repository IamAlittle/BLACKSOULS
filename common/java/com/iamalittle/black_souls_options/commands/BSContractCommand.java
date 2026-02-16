package com.iamalittle.black_souls_options.commands;

import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 契约指令系统 - 用于直接给予或删除玩家的契约
 * 格式：/bs contract [give/del] [playerID] [生物命名空间]
 */
public class BSContractCommand {
    
    /**
     * 注册指令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bs")
            .then(Commands.literal("contract")
                .then(Commands.literal("give")
                    .requires(source -> source.hasPermission(2)) // 需要管理员权限（权限等级2）
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("entityType", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                // 提供"all"选项和已注册的生物命名空间建议
                                builder.suggest("all");
                                Set<String> entityTypes = ContractEffectRegistry.getInstance().getAllEntityTypes();
                                for (String entityType : entityTypes) {
                                    builder.suggest(entityType);
                                }
                                return builder.buildFuture();
                            })
                            .executes(BSContractCommand::giveContract)
                        )
                    )
                )
                .then(Commands.literal("del")
                    .requires(source -> source.hasPermission(2)) // 需要管理员权限（权限等级2）
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("entityType", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                // 提供"all"选项和已注册的生物命名空间建议
                                builder.suggest("all");
                                Set<String> entityTypes = ContractEffectRegistry.getInstance().getAllEntityTypes();
                                for (String entityType : entityTypes) {
                                    builder.suggest(entityType);
                                }
                                return builder.buildFuture();
                            })
                            .executes(BSContractCommand::deleteContract)
                        )
                    )
                )
                .then(Commands.literal("list")
                    .executes(BSContractCommand::listContracts)
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> source.hasPermission(2)) // 查看其他玩家契约需要管理员权限
                        .executes(BSContractCommand::listOtherPlayerContracts)
                    )
                )
                .then(Commands.literal("toggle")
                    .requires(source -> source.hasPermission(2)) // 需要管理员权限
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("entityType", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                // 提供已注册的生物命名空间建议
                                Set<String> entityTypes = ContractEffectRegistry.getInstance().getAllEntityTypes();
                                for (String entityType : entityTypes) {
                                    builder.suggest(entityType);
                                }
                                return builder.buildFuture();
                            })
                            .executes(BSContractCommand::toggleContract)
                        )
                    )
                )
            )
        );
    }
    
    /**
     * 给予契约
     */
    private static int giveContract(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String entityType = StringArgumentType.getString(context, "entityType");
        CommandSourceStack source = context.getSource();
        
        // 获取玩家的契约管理器
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
        if (manager == null) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.contract_manager_not_found"));
            return 0;
        }
        
        // 检查是否为"all"选项
        if ("all".equalsIgnoreCase(entityType)) {
            return giveAllContracts(source, player, manager);
        }
        
        // 检查实体类型是否已注册
        if (!ContractEffectRegistry.getInstance().hasEffectsForEntityType(entityType)) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.entity_type_not_registered", entityType));
            return 0;
        }
        
        // 检查玩家是否已有该契约
        boolean hasContract = manager.getAllContracts().stream()
            .anyMatch(contract -> entityType.equals(contract.getEntityType()));
        
        if (hasContract) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.player_already_has_contract", entityType));
            return 0;
        }
        
        String entityName = getEntityDisplayName(entityType);
        
        // 使用玩家当前位置作为契约位置
        Vec3 playerPos = player.position();
        String dimension = player.level().dimension().location().toString();
        
        // 使用createContractFromCommand方法创建契约（不会被自动解除）
        manager.createContractFromCommand(entityType, entityName, playerPos, dimension);
        
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.give_contract", 
            player.getScoreboardName(), entityType), true);
        
        if (!source.isPlayer() || !source.getPlayer().equals(player)) {
            player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.got_new_contract", entityName));
        }
        
        return 1;
    }
    
    /**
     * 给予所有契约
     */
    private static int giveAllContracts(CommandSourceStack source, ServerPlayer player, ContractManager manager) {
        Set<String> allEntityTypes = ContractEffectRegistry.getInstance().getAllEntityTypes();
        final int[] createdCount = {0}; // 使用数组来绕过final限制
        
        // 使用玩家当前位置作为契约位置
        Vec3 playerPos = player.position();
        String dimension = player.level().dimension().location().toString();
        
        for (String entityType : allEntityTypes) {
            // 检查玩家是否已有该契约
            boolean hasContract = manager.getAllContracts().stream()
                .anyMatch(contract -> entityType.equals(contract.getEntityType()));
            
            if (!hasContract) {
                String entityName = getEntityDisplayName(entityType);
                
                // 使用createContractFromCommand方法创建契约（不会被自动解除）
                manager.createContractFromCommand(entityType, entityName, playerPos, dimension);
                createdCount[0]++;
            }
        }
        
        if (createdCount[0] > 0) {
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.give_all_contracts", 
                player.getScoreboardName(), createdCount[0]), true);
            
            if (!source.isPlayer() || !source.getPlayer().equals(player)) {
                player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.got_all_contracts", createdCount[0]));
            }
        } else {
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.already_has_all_contracts", 
                player.getScoreboardName()), true);
        }
        
        return createdCount[0];
    }
    
    /**
     * 删除契约
     */
    private static int deleteContract(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String entityType = StringArgumentType.getString(context, "entityType");
        CommandSourceStack source = context.getSource();
        
        // 获取玩家的契约管理器
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
        if (manager == null) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.contract_manager_not_found"));
            return 0;
        }
        
        // 检查是否为"all"选项
        if ("all".equalsIgnoreCase(entityType)) {
            return deleteAllContracts(source, player, manager);
        }
        
        // 查找对应的契约
        Optional<Contract> contractToRemove = manager.getAllContracts().stream()
            .filter(contract -> entityType.equals(contract.getEntityType()))
            .findFirst();
        
        if (!contractToRemove.isPresent()) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.player_has_no_contract", entityType));
            return 0;
        }
        
        Contract contract = contractToRemove.get();
        
        // 从管理器中移除契约（服务器端会调用deactivateEffects，避免重复停用）
        manager.removeContract(contract.getEntityId());
        
        // 向所有玩家广播契约更新，确保客户端列表同步
        ContractNetworkHandler.broadcastContractUpdate(player);
        
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.delete_contract", 
            player.getScoreboardName(), entityType), true);
        
        if (!source.isPlayer() || !source.getPlayer().equals(player)) {
            player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.lost_contract", contract.getEntityName()));
        }
        
        return 1;
    }
    
    /**
     * 删除所有契约
     */
    private static int deleteAllContracts(CommandSourceStack source, ServerPlayer player, ContractManager manager) {
        Collection<Contract> allContracts = manager.getAllContracts();
        int removedCount = allContracts.size();
        
        if (removedCount == 0) {
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.player_has_no_contracts", 
                player.getScoreboardName()), true);
            return 0;
        }
        
        // 清空所有契约（服务器端会调用deactivateEffects，避免重复停用）
        manager.clearContracts();
        
        // 向所有玩家广播契约更新，确保客户端列表同步
        ContractNetworkHandler.broadcastContractUpdate(player);
        
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.delete_all_contracts", 
            player.getScoreboardName(), removedCount), true);
        
        if (!source.isPlayer() || !source.getPlayer().equals(player)) {
            player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.lost_all_contracts"));
        }
        
        return removedCount;
    }
    
    /**
     * 列出玩家所有契约（默认查看自己的契约）
     */
    private static int listContracts(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;

        // 如果没有指定玩家，默认使用命令执行者
        player = source.getPlayer();

        // 获取玩家的契约管理器
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
        if (manager == null) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.contract_manager_not_found"));
            return 0;
        }
        
        Collection<Contract> contracts = manager.getAllContracts();
        
        if (contracts.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.list.no_contracts", player.getScoreboardName()), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.list.contracts_header", player.getScoreboardName()), false);
        
        for (Contract contract : contracts) {
            boolean isActive = contract.getEffects().stream().anyMatch(effect -> effect.isActive());
            String toggleCommand = "/bs contract toggle " + player.getScoreboardName() + " " + contract.getEntityType();
            
            // 创建可点击的激活状态文本
            Component statusComponent = Component.literal("[")
                .append(Component.translatable(isActive ? 
                    "black_souls_options.commands.list.status_active" : 
                    "black_souls_options.commands.list.status_inactive"))
                .append(Component.literal("]"))
                .withStyle(style -> style
                    .withColor(net.minecraft.network.chat.TextColor.parseColor(isActive ? "#55FF55" : "#FF5555"))
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, toggleCommand))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, 
                        Component.translatable("black_souls_options.commands.list.click_to_toggle"))));
            
            Component contractInfo = Component.literal("§7- " + contract.getEntityName() + " (" + contract.getEntityType() + ") - ")
                .append(statusComponent);
            
            source.sendSuccess(() -> contractInfo, false);
        }
        
        return contracts.size();
    }
    
    /**
     * 列出其他玩家的契约（管理员功能）
     */
    private static int listOtherPlayerContracts(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

        // 获取目标玩家的契约管理器
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(targetPlayer);
        if (manager == null) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.cannot_get_player_contract_manager", targetPlayer.getScoreboardName()));
            return 0;
        }
        
        Collection<Contract> contracts = manager.getAllContracts();
        
        if (contracts.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.list.no_contracts", targetPlayer.getScoreboardName()), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.list.contracts_header", targetPlayer.getScoreboardName()), false);
        
        for (Contract contract : contracts) {
            boolean isActive = contract.getEffects().stream().anyMatch(effect -> effect.isActive());
            String toggleCommand = "/bs contract toggle " + targetPlayer.getScoreboardName() + " " + contract.getEntityType();
            
            // 创建可点击的激活状态文本
            Component statusComponent = Component.literal("[")
                .append(Component.translatable(isActive ? 
                    "black_souls_options.commands.list.status_active" : 
                    "black_souls_options.commands.list.status_inactive"))
                .append(Component.literal("]"))
                .withStyle(style -> style
                    .withColor(net.minecraft.network.chat.TextColor.parseColor(isActive ? "#55FF55" : "#FF5555"))
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, toggleCommand))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, 
                        Component.translatable("black_souls_options.commands.list.click_to_toggle"))));
            
            String commandCreated = contract.isCommandCreated() ? " " + Component.translatable("black_souls_options.contracts_screen.command_created_text").getString() : "";
            Component contractInfo = Component.literal("§7- " + contract.getEntityName() + " §r(" + contract.getEntityType() + ") - ")
                .append(statusComponent)
                .append(Component.literal(commandCreated));
            
            source.sendSuccess(() -> contractInfo, false);
        }
        
        return contracts.size();
    }
    
    /**
     * 切换契约激活状态
     */
    private static int toggleContract(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String entityType = StringArgumentType.getString(context, "entityType");
        CommandSourceStack source = context.getSource();
        
        // 获取玩家的契约管理器
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
        if (manager == null) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.contract_manager_not_found"));
            return 0;
        }
        
        // 查找对应的契约
        Optional<Contract> contractOptional = manager.getAllContracts().stream()
            .filter(contract -> entityType.equals(contract.getEntityType()))
            .findFirst();
        
        if (!contractOptional.isPresent()) {
            source.sendFailure(Component.translatable("black_souls_options.commands.error.player_has_no_contract", entityType));
            return 0;
        }
        
        Contract contract = contractOptional.get();
        
        // 检查契约是否有激活的效果
        boolean hasActiveEffect = contract.getEffects().stream().anyMatch(effect -> effect.isActive());
        
        if (hasActiveEffect) {
            // 停用所有效果
            contract.deactivateEffects(player);
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.deactivate_contract", 
                player.getScoreboardName(), contract.getEntityName()), true);
            
            if (!source.isPlayer() || !source.getPlayer().equals(player)) {
                player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.contract_deactivated", contract.getEntityName()));
            }
        } else {
            // 激活所有效果
            contract.activateEffects(player, true);
            source.sendSuccess(() -> Component.translatable("black_souls_options.commands.success.activate_contract", 
                player.getScoreboardName(), contract.getEntityName()), true);
            
            if (!source.isPlayer() || !source.getPlayer().equals(player)) {
                player.sendSystemMessage(Component.translatable("black_souls_options.commands.message.contract_activated", contract.getEntityName()));
            }
        }
        
        // 向所有玩家广播契约更新，确保客户端列表同步
        ContractNetworkHandler.broadcastContractUpdate(player);
        
        // 自动重新显示更新后的契约列表，提供更好的用户体验
        source.sendSuccess(() -> Component.translatable("black_souls_options.commands.list.updated_contracts_header").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#FFAA00"))), false);
        
        Collection<Contract> contracts = manager.getAllContracts();
        for (Contract updatedContract : contracts) {
            boolean isActive = updatedContract.getEffects().stream().anyMatch(effect -> effect.isActive());
            String toggleCommand = "/bs contract toggle " + player.getScoreboardName() + " " + updatedContract.getEntityType();
            
            // 创建可点击的激活状态文本
            Component statusComponent = Component.literal("[")
                .append(Component.translatable(isActive ? 
                    "black_souls_options.commands.list.status_active" : 
                    "black_souls_options.commands.list.status_inactive"))
                .append(Component.literal("]"))
                .withStyle(style -> style
                    .withColor(net.minecraft.network.chat.TextColor.parseColor(isActive ? "#55FF55" : "#FF5555"))
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, toggleCommand))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, 
                        Component.translatable("black_souls_options.commands.list.click_to_toggle"))));
            
            Component contractInfo = Component.literal("§7- " + updatedContract.getEntityName() + " §r(" + updatedContract.getEntityType() + ") - ")
                .append(statusComponent);
            
            source.sendSuccess(() -> contractInfo, false);
        }
        
        return 1;
    }
    
    /**
     * 获取实体的显示名称
     */
    private static String getEntityDisplayName(String entityType) {
        // 这里可以添加更复杂的实体名称映射逻辑
        // 目前简单返回实体类型作为名称
        return entityType;
    }
    
    /**
     * 注册指令（重载方法，用于服务器启动时注册）
     */
    public static void register(MinecraftServer server) {
        register(server.getCommands().getDispatcher());
    }
}