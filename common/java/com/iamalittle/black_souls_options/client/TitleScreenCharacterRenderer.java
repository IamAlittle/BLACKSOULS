package com.iamalittle.black_souls_options.client;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.iamalittle.black_souls_options.sound.ModSounds;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Random;

/**
 * 标题界面像素角色渲染器
 * 在标题界面按照特定行为模式显示像素角色
 */
public class TitleScreenCharacterRenderer {
    
    // 角色精灵表资源位置
    private static final ResourceLocation ALICE_TEXTURE = new ResourceLocation("black_souls_options", "textures/characters/alice.png");
    private static final ResourceLocation ALICE2_TEXTURE = new ResourceLocation("black_souls_options", "textures/characters/alice2.png");
    
    // 精灵表配置
    private static final int SPRITE_WIDTH = 32; // 每个精灵的宽度 (96/3)
    private static final int SPRITE_HEIGHT = 34; // 每个精灵的高度 (136/4)
    private static final int SPRITES_PER_ROW = 3; // 每行的精灵数量
    
    // 动画配置
    private static final int ANIMATION_SPEED = 11; // 普通动画更新速度（帧数）
    private static final int ROTATE_ANIMATION_SPEED = 10; // 转圈动画速度（0.5秒/帧 = 10帧/秒，Minecraft默认20帧/秒）
    private static final int MOVE_SPEED = 1; // 移动速度（像素/帧）
    
    // 行为时间配置（毫秒单位）
    private static final long WAIT_DURATION = 20000; // 等待时间（20秒）
    private static final long PEEK_DURATION = 5000; // 探头观察时间（5秒）
    private static final long JUMP_DURATION = 750; // 跳跃动画时间（约0.75秒）
    private static final long ROTATE_FRAME_DURATION = 500; // 旋转动画帧持续时间（0.5秒/帧）
    private static final long JUMP_OUT_DURATION = 800; // 弧线跳出的时间（毫秒）
    private static final long REACT_DURATION = 3000; // 被点击后的反应时间（3秒）
    private static final long FLEE_DURATION = 1000; // 逃跑动画时间（1秒）
    private static final long FLINCH_DURATION = 500; // 左右张望持续时间（0.5秒）
    private static final long RE_PEEK_DELAY = 5000; // 被点击后再次探头的延迟（5秒）
    
    // 状态枚举
    private enum CharacterState {
        WAITING,     // 等待20秒
        PEEKING,     // 从边缘探头
        JUMPING_OUT, // 跳出
        ROTATING,    // 转圈
        JUMPING,     // 跳跃
        MOVING,      // 正常移动
        REACTING,    // 被点击后的反应
        FLINCHING,   // 左右张望
        FLEEING,     // 逃跑
        HIDE_RECOVERY, // 隐藏恢复中
        SHOW_ALICE2  // 显示Alice2.png
    }
    
    // 探头位置枚举
    private enum PeekEdge {
        LEFT, RIGHT, BOTTOM
    }
    
    // 状态变量
    private static int animationFrame = 0;
    private static int animationTimer = 0;
    private static boolean frameDirectionForward = true; // 动画方向标志，true表示向前（1→2→3），false表示向后（3→2→1）
    private static int characterX = 0;
    private static int characterY = 0;
    private static int targetX = 0;
    private static int targetY = 0;
    private static int deltaX = 0; // X方向差值
    private static int deltaY = 0; // Y方向差值
    private static int directionX = 1; // X方向：1=右，-1=左
    private static int directionY = 1; // Y方向：1=下，-1=上
    private static int primaryDirection = 0; // 主要方向：0:下, 1:左, 2:右, 3:上
    private static boolean isXAxisLocked = false; // X轴是否被锁定
    private static boolean isYAxisLocked = false; // Y轴是否被锁定
    private static boolean isMoving = false;
    private static int stayTimer = 0;
    private static final int STAY_DURATION = 30; // 停留时间（帧数）
    private static Random random = new Random();
    
    // 新状态变量
    private static CharacterState currentState = CharacterState.WAITING;
    private static long stateStartTime = System.currentTimeMillis(); // 状态开始时间
    private static PeekEdge peekEdge = PeekEdge.LEFT;
    private static int jumpHeight = 0;
    private static int rotateFrame = 0;
    private static int jumpCount = 0;
    private static boolean isJumpingUp = true;
    private static long lastRotateFrameTime = 0; // 上次旋转帧更新时间
    private static int jumpStartX; // 跳出动画的起始X位置
    private static int jumpStartY; // 跳出动画的起始Y位置
    private static int jumpEndX; // 跳出动画的结束X位置
    private static int jumpEndY; // 跳出动画的结束Y位置
    
    // 鼠标点击相关变量
    private static boolean clicked = false;
    private static int clickCount = 0;
    private static long lastClickTime = 0;
    private static boolean isShowingAlice2 = false;
    private static int flinchDirection = 1; // 左右张望方向：1=右，-1=左
    private static long flinchStartTime = 0;
    private static boolean fleeDirectionRight = true; // 逃跑方向
    private static int appearanceCount = 1; // 角色出现次数计数器，用于区分第一次和第二次出现，初始值为1
    
    // 文字显示相关变量
    private static boolean showDiscoveryText = false;
    private static long discoveryTextStartTime = 0;
    private static final int DISCOVERY_TEXT_COUNT = 50;
    private static final long DISCOVERY_TEXT_DURATION = 2000; // 2秒内显示所有文字
    private static final String DISCOVERY_TEXT = "被发现了！";
    private static final DiscoveryText[] discoveryTexts = new DiscoveryText[DISCOVERY_TEXT_COUNT];
    // 窗口大小跟踪变量
    private static int lastWindowWidth = 0;
    private static int lastWindowHeight = 0;
    
    // 背景音乐相关变量
    private static boolean bgm1Played = false; // 第一首背景音乐是否已播放
    private static boolean bgm2Played = false; // 第二首背景音乐是否已播放
    private static SimpleSoundInstance bgm1Instance = null; // 第一首背景音乐实例
    private static SimpleSoundInstance bgm2Instance = null; // 第二首背景音乐实例
    
    // 文字显示类
    private static class DiscoveryText {
        int x;
        int y;
        float rotation;
        long startTime;
        boolean active;
        
        DiscoveryText(int x, int y, float rotation, long startTime) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.startTime = startTime;
            this.active = true;
        }
    }
    
    /**
     * 渲染标题界面的像素角色
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 只有在标题界面才渲染
        if (!(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        
        // 播放第一首背景音乐（进入标题界面时）
        if (!bgm1Played) {
            playBGM1(minecraft);
            bgm1Played = true;
        }
        
        // 初始化位置（如果需要）
        if (characterX == 0 && characterY == 0) {
            initCharacterPosition(minecraft);
        }
        
        // 更新状态
        updateState(minecraft);
        
        // 更新动画
        updateAnimation();
        
        // 更新位置
        updatePosition(minecraft);
        
        // 如果GuiGraphics为null，直接返回
        if (guiGraphics == null) {
            return;
        }
        
        // 渲染角色
        renderCharacter(guiGraphics, minecraft);
        
        // 渲染文字显示
        renderDiscoveryTexts(guiGraphics, minecraft);
        
        // 当显示Alice2时，添加更亮的红橙色透明滤镜
        if (isShowingAlice2) {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            // 使用更亮的红橙色(R:255, G:180, B:100)，透明度约为40% (ARGB颜色值)
            guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x66FF7F47);
        }
        
        // 渲染坐标显示
        renderCoordinates(guiGraphics, minecraft);
    }
    
    /**
     * 初始化角色位置（不在屏幕内，准备探头）
     */
    private static void initCharacterPosition(Minecraft minecraft) {
        // 随机选择探头边缘
        peekEdge = PeekEdge.values()[random.nextInt(PeekEdge.values().length)];
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 初始位置设置在屏幕外，准备探头
        switch (peekEdge) {
            case LEFT:
                characterX = -SPRITE_WIDTH / 2;
                characterY = random.nextInt(screenHeight / 2 - SPRITE_HEIGHT / 2) + screenHeight / 4;
                break;
            case RIGHT:
                characterX = screenWidth;
                characterY = random.nextInt(screenHeight / 2 - SPRITE_HEIGHT / 2) + screenHeight / 4;
                break;
            case BOTTOM:
                characterX = random.nextInt(screenWidth - SPRITE_WIDTH / 2);
                characterY = screenHeight;
                break;
        }
        
        currentState = CharacterState.WAITING;
        stateStartTime = System.currentTimeMillis();
    }
    
    /**
     * 生成随机目标位置
     */
    private static void generateTargetPosition(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 随机目标位置，确保角色在屏幕内（缩小一半）
        targetX = random.nextInt(screenWidth - SPRITE_WIDTH / 2);
        targetY = random.nextInt(screenHeight - SPRITE_HEIGHT / 2);
    }
    
    /**
     * 初始化文字显示
     */
    private static void initDiscoveryTexts(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        showDiscoveryText = true;
        discoveryTextStartTime = System.currentTimeMillis();
        
        // 获取字体大小，计算文字宽度和高度
        int textWidth = minecraft.font.width(DISCOVERY_TEXT);
        int textHeight = minecraft.font.lineHeight;
        
        // 考虑放大3倍后的实际尺寸
        int scaledTextWidth = (int)(textWidth * 3f);
        int scaledTextHeight = (int)(textHeight * 3f);
        
        // 计算网格大小，确保文字均匀分布
        int gridCols = 6; // 6列
        int gridRows = (int)Math.ceil((double)DISCOVERY_TEXT_COUNT / gridCols); // 自动计算行数
        
        // 计算每个网格的大小，考虑文字间距
        int gridCellWidth = (screenWidth - scaledTextWidth) / (gridCols - 1);
        int gridCellHeight = (screenHeight - scaledTextHeight) / (gridRows - 1);
        
        // 初始化所有文字
        for (int i = 0; i < DISCOVERY_TEXT_COUNT; i++) {
            // 基于网格的位置，添加随机偏移以避免完全规则排列
            int col = i % gridCols;
            int row = i / gridCols;
            
            // 计算基础位置
            int baseX = col * gridCellWidth;
            int baseY = row * gridCellHeight;
            
            // 添加随机偏移（±20%网格大小）
            int offsetX = (int)(random.nextFloat() * gridCellWidth * 0.4f - gridCellWidth * 0.2f);
            int offsetY = (int)(random.nextFloat() * gridCellHeight * 0.4f - gridCellHeight * 0.2f);
            
            // 确保文字在屏幕范围内
            int x = Math.max(0, Math.min(screenWidth - scaledTextWidth, baseX + offsetX));
            int y = Math.max(0, Math.min(screenHeight - scaledTextHeight, baseY + offsetY));
            
            // 随机旋转角度（-45度到45度）
            float rotation = random.nextFloat() * 90 - 45;
            // 计算文字开始时间（2秒内均匀分布）
            long startTime = discoveryTextStartTime + (long)(i * (DISCOVERY_TEXT_DURATION / (float)DISCOVERY_TEXT_COUNT));
            
            discoveryTexts[i] = new DiscoveryText(x, y, rotation, startTime);
        }
        
        // 更新窗口大小记录
        lastWindowWidth = screenWidth;
        lastWindowHeight = screenHeight;
    }
    
    /**
     * 处理鼠标点击事件
     */
    public static void handleMouseClick(Minecraft minecraft, double mouseX, double mouseY) {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否点击到角色
        int renderX = characterX;
        int renderY = characterY - jumpHeight;
        int spriteSizeX = (currentState == CharacterState.PEEKING) ? SPRITE_WIDTH / 2 : SPRITE_WIDTH / 2;
        int spriteSizeY = (currentState == CharacterState.PEEKING) ? SPRITE_HEIGHT / 2 : SPRITE_HEIGHT / 2;
        
        if (mouseX >= renderX && mouseX <= renderX + spriteSizeX && 
            mouseY >= renderY && mouseY <= renderY + spriteSizeY) {
            
            clicked = true;
            clickCount++;
            lastClickTime = currentTime;
            
            switch (currentState) {
                case PEEKING:
                    // 探头期间被点击，缩回窗口外
                    startFleeing(minecraft);
                    break;
                    
                case MOVING:
                    // 移动时的点击处理
                    if (isMoving) {
                        // 根据出现次数决定点击行为
                        if (appearanceCount == 1) {
                            // 第一次出现，点击后先原地跳一下左右看看再逃跑
                            currentState = CharacterState.REACTING;
                            stateStartTime = currentTime;
                            jumpCount = 0;
                            jumpHeight = 0;
                            isJumpingUp = true;
                        } else {
                            // 第二次及以后出现，检查点击次数
                            if (clickCount < 2) {
                                // 第一次点击，进入跳跃状态
                                currentState = CharacterState.JUMPING;
                                stateStartTime = currentTime;
                                jumpCount = 0;
                                jumpHeight = 0;
                                isJumpingUp = true;
                            } else {
                            // 第二次点击，显示Alice2.png
                            currentState = CharacterState.SHOW_ALICE2;
                            isShowingAlice2 = true; // 设置显示Alice2贴图
                            stateStartTime = currentTime;
                            // 初始化文字显示
                            initDiscoveryTexts(minecraft);
                            // 播放第二首背景音乐（第二次点击和文字出现时）
                            if (!bgm2Played) {
                                playBGM2(minecraft);
                                bgm2Played = true;
                            }
                        }
                        }
                    } else {
                        // 停留时被点击
                        if (appearanceCount == 1) {
                            // 第一次出现，点击后先原地跳一下左右看看再逃跑
                            currentState = CharacterState.REACTING;
                            stateStartTime = currentTime;
                            jumpCount = 0;
                            jumpHeight = 0;
                            isJumpingUp = true;
                        } else {
                            // 第二次及以后出现，检查点击次数
                            if (clickCount < 2) {
                                // 第一次点击，进入跳跃状态
                                currentState = CharacterState.JUMPING;
                                stateStartTime = currentTime;
                                jumpCount = 0;
                                jumpHeight = 0;
                                isJumpingUp = true;
                            } else {
                                // 第二次点击，显示Alice2.png
                                currentState = CharacterState.SHOW_ALICE2;
                                isShowingAlice2 = true; // 设置显示Alice2贴图
                                stateStartTime = currentTime;
                                // 初始化文字显示
                                initDiscoveryTexts(minecraft);
                                // 播放第二首背景音乐（第二次点击和文字出现时）
                                if (!bgm2Played) {
                                    playBGM2(minecraft);
                                    bgm2Played = true;
                                }
                            }
                        }
                        isMoving = false;
                    }
                    break;
                    
                case HIDE_RECOVERY:
                case JUMPING_OUT:
                case ROTATING:
                case JUMPING:
                    // 这些状态下无法互动
                    break;
                    
                default:
                    break;
            }
        }
    }
    
    /**
     * 开始逃跑
     */
    private static void startFleeing(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        
        // 选择距离更近的窗口边缘逃跑
        fleeDirectionRight = (screenWidth - characterX) < characterX;
        
        currentState = CharacterState.FLEEING;
        stateStartTime = System.currentTimeMillis();
    }
    
    /**
     * 更新角色状态机
     */
    private static void updateState(Minecraft minecraft) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - stateStartTime;
        
        switch (currentState) {
            case WAITING:
                if (elapsedTime >= WAIT_DURATION) {
                    // 等待20秒后开始探头
                    currentState = CharacterState.PEEKING;
                    stateStartTime = currentTime;
                }
                break;
            
            case PEEKING:
                if (elapsedTime >= PEEK_DURATION) {
                    // 探头5秒后开始弧线跳出动画
                    int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                    int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                    
                    // 记录跳跃起始位置和结束位置
                    jumpStartX = characterX;
                    jumpStartY = characterY;
                    
                    switch (peekEdge) {
                        case LEFT:
                            jumpEndX = SPRITE_WIDTH / 2;
                            jumpEndY = characterY;
                            break;
                        case RIGHT:
                            jumpEndX = screenWidth - SPRITE_WIDTH;
                            jumpEndY = characterY;
                            break;
                        case BOTTOM:
                            jumpEndX = characterX;
                            jumpEndY = screenHeight - SPRITE_HEIGHT;
                            break;
                    }
                    
                    // 进入弧线跳出状态
                    currentState = CharacterState.JUMPING_OUT;
                    stateStartTime = currentTime;
                    isMoving = false;
                }
                break;
            
            case JUMPING_OUT:
                if (elapsedTime >= JUMP_OUT_DURATION) {
                    // 弧线跳出完成，进入转圈状态
                    currentState = CharacterState.ROTATING;
                    stateStartTime = currentTime;
                    rotateFrame = 0;
                    animationTimer = 0;
                    lastRotateFrameTime = currentTime;
                }
                break;
            
            case ROTATING:
                if (elapsedTime >= ROTATE_FRAME_DURATION * 4) { // 4个旋转帧
                    // 转圈完成后开始跳跃
                    currentState = CharacterState.JUMPING;
                    stateStartTime = currentTime;
                    jumpCount = 0;
                    jumpHeight = 0;
                    isJumpingUp = true;
                }
                break;
            
            case JUMPING:
                if (elapsedTime >= JUMP_DURATION) {
                    jumpCount++;
                    if (jumpCount >= 2) {
                        // 跳跃两次完成后开始正常移动（第一次出现时的行为）
                        currentState = CharacterState.MOVING;
                        stateStartTime = currentTime;
                        generateTargetPosition(minecraft);
                        isMoving = true;
                        
                        // 计算初始方向并确定主要方向
                        deltaX = targetX - characterX;
                        deltaY = targetY - characterY;
                        
                        // 20%概率直接斜着走
                        boolean diagonalMove = random.nextInt(100) < 20;
                        
                        if (!diagonalMove) {
                            // 优先走直线：锁定一个轴，先移动距离最长的轴
                            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                // X方向距离更远，先移动X轴（水平移动）
                                directionX = deltaX > 0 ? 1 : -1;
                                directionY = 0; // 锁定Y轴
                                primaryDirection = directionX > 0 ? 2 : 1; // 右或左
                            } else {
                                // Y方向距离更远，先移动Y轴（垂直移动）
                                directionY = deltaY > 0 ? 1 : -1;
                                directionX = 0; // 锁定X轴
                                primaryDirection = directionY > 0 ? 0 : 3; // 下或上
                            }
                        } else {
                            // 斜着走时，同时移动两个轴
                            directionX = deltaX > 0 ? 1 : -1;
                            directionY = deltaY > 0 ? 1 : -1;
                            // 根据X和Y的步数决定主要方向
                            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                primaryDirection = directionX > 0 ? 2 : 1; // 右或左
                            } else {
                                primaryDirection = directionY > 0 ? 0 : 3; // 下或上
                            }
                        }
                    } else if (appearanceCount > 1) {
                        // 第二次及以后出现时，跳跃一次后继续移动
                        currentState = CharacterState.MOVING;
                        stateStartTime = currentTime;
                        generateTargetPosition(minecraft); // 生成新的目标位置
                        isMoving = true;
                    } else {
                        // 第一次跳跃完成，准备第二次跳跃
                        stateStartTime = currentTime;
                        jumpHeight = 0;
                        isJumpingUp = true;
                    }
                } else {
                    // 更新跳跃高度
                    if (isJumpingUp) {
                        jumpHeight += 2;
                        if (jumpHeight >= 15) {
                            isJumpingUp = false;
                        }
                    } else {
                        jumpHeight -= 2;
                        if (jumpHeight <= 0) {
                            jumpHeight = 0;
                        }
                    }
                }
                break;
            
            case MOVING:
                // 正常移动逻辑已在updatePosition中实现
                break;
                
            case REACTING:
                if (elapsedTime >= JUMP_DURATION) {
                    jumpCount++;
                    if (jumpCount >= 2) {
                        // 跳跃两次后开始左右张望
                        currentState = CharacterState.FLINCHING;
                        stateStartTime = currentTime;
                        flinchDirection = 1;
                        flinchStartTime = currentTime;
                    } else {
                        // 第一次跳跃完成，准备第二次跳跃
                        stateStartTime = currentTime;
                        jumpHeight = 0;
                        isJumpingUp = true;
                    }
                } else {
                    // 更新跳跃高度
                    if (isJumpingUp) {
                        jumpHeight += 2;
                        if (jumpHeight >= 15) {
                            isJumpingUp = false;
                        }
                    } else {
                        jumpHeight -= 2;
                        if (jumpHeight <= 0) {
                            jumpHeight = 0;
                        }
                    }
                }
                break;
                
            case FLINCHING:
                if (elapsedTime >= FLINCH_DURATION * 2) {
                    // 左右张望两下后逃跑
                    startFleeing(minecraft);
                } else {
                    // 每隔FLINCH_DURATION毫秒切换张望方向
                    if (currentTime - flinchStartTime >= FLINCH_DURATION) {
                        flinchDirection *= -1;
                        flinchStartTime = currentTime;
                    }
                }
                break;
                
            case FLEEING:
                if (elapsedTime >= FLEE_DURATION) {
                    // 逃跑完成后隐藏恢复
                    currentState = CharacterState.HIDE_RECOVERY;
                    stateStartTime = currentTime;
                    
                    // 重置位置到屏幕外
                    int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                    int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                    
                    if (fleeDirectionRight) {
                        characterX = screenWidth;
                    } else {
                        characterX = -SPRITE_WIDTH / 2;
                    }
                    characterY = random.nextInt(screenHeight / 2 - SPRITE_HEIGHT / 2) + screenHeight / 4;
                }
                break;
                
            case HIDE_RECOVERY:
                if (elapsedTime >= RE_PEEK_DELAY) {
                    // 恢复完成后重新开始探头流程
                    currentState = CharacterState.WAITING;
                    stateStartTime = currentTime;
                    clickCount = 0; // 重置点击计数
                    appearanceCount++; // 增加出现次数
                }
                break;
                
            case SHOW_ALICE2:
                // 保持显示Alice2.png
                break;
        }
    }
    
    /**
     * 更新动画帧
     */
    private static void updateAnimation() {
        switch (currentState) {
            case WAITING:
                animationFrame = 1; // 等待时显示正脸
                break;
                
            case PEEKING:
            case JUMPING_OUT:
                animationFrame = 1; // 探头和跳出时显示正脸
                break;
                
            case ROTATING:
                // 转圈动画：2、5、8、11帧（索引1、4、7、10）
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRotateFrameTime >= ROTATE_FRAME_DURATION) {
                    rotateFrame++;
                    if (rotateFrame >= 4) {
                        rotateFrame = 0;
                    }
                    lastRotateFrameTime = currentTime;
                }
                break;
                
            case JUMPING:
                animationFrame = 1; // 跳跃时显示正脸
                break;
                
            case MOVING:
                // 如果在停留状态，显示正脸（第2帧，索引为1）
                if (!isMoving) {
                    animationFrame = 1;
                    return;
                }
                
                // 移动时更新动画帧
                animationTimer++;
                if (animationTimer >= ANIMATION_SPEED) {
                    // 修复动画播放顺序：1、2、3、2、1、2、3、2...
                    // 使用方向标志来控制帧的循环方向
                    if (frameDirectionForward) {
                        // 向前移动：1→2→3
                        if (animationFrame < 2) {
                            animationFrame++;
                        } else {
                            // 到达第3帧，改变方向
                            animationFrame--;
                            frameDirectionForward = false;
                        }
                    } else {
                        // 向后移动：3→2→1
                        if (animationFrame > 0) {
                            animationFrame--;
                        } else {
                            // 到达第1帧，改变方向
                            animationFrame++;
                            frameDirectionForward = true;
                        }
                    }
                    animationTimer = 0;
                }
                break;
        }
    }
    
    /**
     * 更新角色位置
     */
    private static void updatePosition(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        switch (currentState) {
            case WAITING:
                // 等待时保持在屏幕外，但要根据当前窗口大小调整位置
                switch (peekEdge) {
                    case LEFT:
                        characterX = -SPRITE_WIDTH / 2;
                        break;
                    case RIGHT:
                        characterX = screenWidth;
                        break;
                    case BOTTOM:
                        characterY = screenHeight;
                        break;
                }
                break;
                
            case PEEKING:
                // 探头时的位置调整
                switch (peekEdge) {
                    case LEFT:
                        characterX = -SPRITE_WIDTH / 4;
                        break;
                    case RIGHT:
                        characterX = screenWidth - SPRITE_WIDTH / 4;
                        break;
                    case BOTTOM:
                        characterY = screenHeight - SPRITE_HEIGHT / 4; // 只露出一部分（1/4高度）
                        break;
                }
                break;
            
            case JUMPING_OUT:
                // 弧线跳出动画
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - stateStartTime;
                float progress = Math.min(elapsedTime / (float)JUMP_OUT_DURATION, 1.0f);
                
                // 计算抛物线轨迹
                float jumpProgress = (float)(1 - Math.pow(1 - progress, 2));
                float arcHeight = 20.0f; // 跳跃高度
                
                switch (peekEdge) {
                    case LEFT:
                        // 从左边缘向右弧线跳出
                        characterX = (int) (jumpStartX + (jumpEndX - jumpStartX) * jumpProgress);
                        // 抛物线Y位置
                        characterY = (int) (jumpStartY - Math.sin(jumpProgress * Math.PI) * arcHeight);
                        break;
                    case RIGHT:
                        // 从右边缘向左弧线跳出
                        characterX = (int) (jumpStartX - (jumpStartX - jumpEndX) * jumpProgress);
                        // 抛物线Y位置
                        characterY = (int) (jumpStartY - Math.sin(jumpProgress * Math.PI) * arcHeight);
                        break;
                    case BOTTOM:
                        // 从底部向上弧线跳出
                        characterY = (int) (jumpStartY - (jumpStartY - jumpEndY) * jumpProgress);
                        // 抛物线X位置（左右小幅度摆动）
                        characterX = (int) (jumpStartX + Math.sin(jumpProgress * Math.PI) * 10);
                        break;
                }
                break;
                
            case MOVING:
                // 移动逻辑
                if (isMoving) {
                    // 计算与目标位置的差值
                    float deltaX = targetX - characterX;
                    float deltaY = targetY - characterY;
                    
                    // 检查是否到达目标位置
                    if (Math.abs(deltaX) <= MOVE_SPEED && Math.abs(deltaY) <= MOVE_SPEED) {
                        // 到达目标位置
                        characterX = targetX;
                        characterY = targetY;
                        isMoving = false;
                        stayTimer = 0;
                        isXAxisLocked = false; // 解锁所有轴
                        isYAxisLocked = false;
                        
                        if (currentState == CharacterState.MOVING) {
                            // 如果是正常移动状态，到达目标后停留
                            stayTimer = 0;
                        }
                        
                        return;
                    }
                    
                    // 智能轴锁定移动逻辑
                    if (!isXAxisLocked && !isYAxisLocked) {
                        // 20%概率直接斜着走
                        boolean diagonalMove = random.nextInt(100) < 20;
                        
                        if (!diagonalMove) {
                            // 80%概率：选择距离最长的轴进行锁定
                            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                // X方向距离更远，锁定Y轴，先移动X轴
                                isYAxisLocked = true;
                                directionX = deltaX > 0 ? 1 : -1;
                                directionY = 0;
                                primaryDirection = directionX > 0 ? 2 : 1; // 右或左
                            } else {
                                // Y方向距离更远，锁定X轴，先移动Y轴
                                isXAxisLocked = true;
                                directionY = deltaY > 0 ? 1 : -1;
                                directionX = 0;
                                primaryDirection = directionY > 0 ? 0 : 3; // 下或上
                            }
                        } else {
                            // 20%概率：斜着走，同时移动两个轴
                            directionX = deltaX > 0 ? 1 : -1;
                            directionY = deltaY > 0 ? 1 : -1;
                            // 根据X和Y的步数决定主要方向
                            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                primaryDirection = directionX > 0 ? 2 : 1; // 右或左
                            } else {
                                primaryDirection = directionY > 0 ? 0 : 3; // 下或上
                            }
                            // 斜向移动时，锁定两个轴，避免进入轴切换逻辑
                            isXAxisLocked = true;
                            isYAxisLocked = true;
                        }
                    } else if (isXAxisLocked && !isYAxisLocked) {
                        // X轴被锁定，只移动Y轴
                        directionY = deltaY > 0 ? 1 : -1;
                        directionX = 0;
                        primaryDirection = directionY > 0 ? 0 : 3; // 下或上
                     } else if (!isXAxisLocked && isYAxisLocked) {
                        // Y轴被锁定，只移动X轴
                        directionX = deltaX > 0 ? 1 : -1;
                        directionY = 0;
                        primaryDirection = directionX > 0 ? 2 : 1; // 右或左
                    }
                    
                    // 检查是否需要切换锁定的轴
                    if (isXAxisLocked && Math.abs(deltaY) <= MOVE_SPEED) {
                        // Y轴已到达目标，解锁X轴，锁定Y轴
                        isXAxisLocked = false;
                        isYAxisLocked = true;
                    } else if (isYAxisLocked && Math.abs(deltaX) <= MOVE_SPEED) {
                        // X轴已到达目标，解锁Y轴，锁定X轴
                        isYAxisLocked = false;
                        isXAxisLocked = true;
                    }
                    
                    // 更新位置
                    characterX += directionX * MOVE_SPEED;
                    characterY += directionY * MOVE_SPEED;
                    
                    // 确保不超出屏幕边界
                    characterX = Math.max(0, Math.min(characterX, screenWidth - SPRITE_WIDTH / 2));
                    characterY = Math.max(0, Math.min(characterY, screenHeight - SPRITE_HEIGHT / 2));
                    
                    // 如果角色到达边界，重新计算主要方向，避免卡在边界
                    if (characterY >= screenHeight - SPRITE_HEIGHT / 2 && primaryDirection == 0) {
                        // 到达底部边界，如果主要方向是向下，重新选择方向
                        primaryDirection = 3; // 改为向上
                    } else if (characterY <= 0 && primaryDirection == 3) {
                        // 到达顶部边界，如果主要方向是向上，重新选择方向
                        primaryDirection = 0; // 改为向下
                    }
                } else if (currentState == CharacterState.MOVING) {
                    // 正常移动状态下的停留逻辑
                    stayTimer++;
                    if (stayTimer >= STAY_DURATION) {
                        // 停留时间结束，生成新目标位置并开始移动
                        generateTargetPosition(minecraft);
                        isMoving = true;
                        stayTimer = 0;
                        isXAxisLocked = false; // 重置轴锁定状态
                        isYAxisLocked = false;
                    }
                }
                break;
                
            case ROTATING:
            case JUMPING:
            case REACTING:
            case FLINCHING:
            case SHOW_ALICE2:
                // 这些状态下不移动
                break;
                
            case FLEEING:
                // 逃跑时的位置更新
                long fleeCurrentTime = System.currentTimeMillis();
                long fleeElapsedTime = fleeCurrentTime - stateStartTime;
                float fleeProgress = Math.min(fleeElapsedTime / (float)FLEE_DURATION, 1.0f);
                
                // 线性逃跑，确保方向正确
                if (fleeDirectionRight) {
                    characterX += (int)(MOVE_SPEED * 3); // 快速向右跑
                } else {
                    characterX -= (int)(MOVE_SPEED * 3); // 快速向左跑
                }
                break;
                
            case HIDE_RECOVERY:
                // 隐藏恢复时不移动
                break;
        }
    }
    
    /**
     * 渲染角色精灵
     */
    private static void renderCharacter(GuiGraphics guiGraphics, Minecraft minecraft) {
        int spriteRow = 0;
        int spriteCol = 1; // 默认显示正脸
        ResourceLocation texture = ALICE_TEXTURE;
        
        // 检查是否显示Alice2.png
        if (isShowingAlice2) {
            texture = ALICE2_TEXTURE;
        }
        
        // 根据状态选择不同的精灵帧
        switch (currentState) {
            case WAITING:
            case PEEKING:
            case JUMPING_OUT:
            case JUMPING:
                spriteRow = 0;
                spriteCol = 1; // 正脸
                break;
                
            case ROTATING:
                // 转圈动画：2、5、8、11帧对应行0-3，列1
                spriteRow = rotateFrame;
                spriteCol = 1;
                break;
                
            case MOVING:
                if (!isMoving) {
                    // 停留时显示正脸
                    spriteRow = 0;
                    spriteCol = 1;
                } else {
                    // 根据主要方向选择动画帧行
                    switch (primaryDirection) {
                        case 0: // 下
                            spriteRow = 0;
                            break;
                        case 1: // 左
                            spriteRow = 1;
                            break;
                        case 2: // 右
                            spriteRow = 2;
                            break;
                        case 3: // 上
                            spriteRow = 3;
                            break;
                        default:
                            spriteRow = 0;
                            break;
                    }
                    spriteCol = animationFrame;
                }
                break;
                
            case REACTING:
            case HIDE_RECOVERY:
                spriteRow = 0;
                spriteCol = 1; // 正脸
                break;
                
            case FLINCHING:
                // 根据张望方向显示左右朝向
                if (flinchDirection > 0) {
                    spriteRow = 2; // 右侧朝向
                } else {
                    spriteRow = 1; // 左侧朝向
                }
                spriteCol = 1; // 显示正脸帧
                break;
                
            case FLEEING:
                // 根据逃跑方向选择朝向
                if (fleeDirectionRight) {
                    spriteRow = 2; // 右侧朝向
                } else {
                    spriteRow = 1; // 左侧朝向
                }
                spriteCol = animationFrame; // 使用动画帧
                break;
                
            case SHOW_ALICE2:
                // 显示Alice2.png
                spriteRow = 0;
                spriteCol = 0;
                break;
        }
        
        int u = spriteCol * SPRITE_WIDTH;
        int v = spriteRow * SPRITE_HEIGHT;
        
        // 设置渲染状态
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 获取渲染的实际位置（考虑跳跃高度）
        int renderX = characterX;
        int renderY = characterY - jumpHeight;
        
        // 根据探头位置应用旋转
        if (currentState == CharacterState.PEEKING) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            
            // 设置旋转中心
            float centerX = renderX + SPRITE_WIDTH / 4f;
            float centerY = renderY + SPRITE_HEIGHT / 4f;
            
            poseStack.translate(centerX, centerY, 0);
            
            // 应用旋转
            switch (peekEdge) {
                case LEFT:
                    poseStack.mulPose(Axis.ZP.rotationDegrees(45)); // 左边顺时针旋转45度
                    break;
                case RIGHT:
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-45)); // 右边逆时针旋转45度
                    break;
                case BOTTOM:
                    // 底部不旋转
                    break;
            }
            
            poseStack.translate(-centerX, -centerY, 0);
            
            // 渲染精灵（缩小一半）
            guiGraphics.blit(texture, 
                    renderX, renderY, 
                    SPRITE_WIDTH / 2, SPRITE_HEIGHT / 2, 
                    u, v, 
                    SPRITE_WIDTH, SPRITE_HEIGHT, 
                    SPRITE_WIDTH * SPRITES_PER_ROW, SPRITE_HEIGHT * 4);
            
            poseStack.popPose();
        } else {
            // 正常渲染（不旋转）
            // 渲染精灵（缩小一半）
            guiGraphics.blit(texture, 
                    renderX, renderY, 
                    SPRITE_WIDTH / 2, SPRITE_HEIGHT / 2, 
                    u, v, 
                    SPRITE_WIDTH, SPRITE_HEIGHT, 
                    SPRITE_WIDTH * SPRITES_PER_ROW, SPRITE_HEIGHT * 4);
        }
        
        // 恢复渲染状态
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染"被发现了！"文字
     */
    private static void renderDiscoveryTexts(GuiGraphics guiGraphics, Minecraft minecraft) {
        if (!showDiscoveryText || discoveryTexts == null) {
            return;
        }
        
        // 检查窗口大小是否变化
        int currentWidth = minecraft.getWindow().getGuiScaledWidth();
        int currentHeight = minecraft.getWindow().getGuiScaledHeight();
        
        if (currentWidth != lastWindowWidth || currentHeight != lastWindowHeight) {
            // 窗口大小变化，重新初始化文字位置
            initDiscoveryTexts(minecraft);
        }
        
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < discoveryTexts.length; i++) {
            DiscoveryText text = discoveryTexts[i];
            if (text != null && text.active && currentTime >= text.startTime) {
                // 渲染单个文字
                renderDiscoveryText(guiGraphics, minecraft, text);
            }
        }
    }
    
    /**
     * 渲染单个"被发现了！"文字
     */
    private static void renderDiscoveryText(GuiGraphics guiGraphics, Minecraft minecraft, DiscoveryText text) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 设置旋转中心
        float centerX = text.x;
        float centerY = text.y;
        
        poseStack.translate(centerX, centerY, 0);
        
        // 应用随机旋转角度
        poseStack.mulPose(Axis.ZP.rotationDegrees(text.rotation));
        
        poseStack.translate(-centerX, -centerY, 0);
        
        // 设置文字颜色为红色
        int textColor = 0xFF0000 | (0xFF << 24); // 红色，不透明
        
        // 渲染文字
        poseStack.pushPose();
        poseStack.scale(3f, 3f, 3f); // 放大
        guiGraphics.drawString(minecraft.font, "被发现了！", (int)(text.x / 3f), (int)(text.y / 3f), textColor);
        poseStack.popPose();
        
        poseStack.popPose();
    }
    
    /**
     * 渲染角色坐标信息
     */
    private static void renderCoordinates(GuiGraphics guiGraphics, Minecraft minecraft) {
        // 只有在debug模式下才显示坐标信息
        if (!BlackSoulsConfig.getInstance().isEnableDebugMode()) {
            return;
        }
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 坐标显示在右上角，距离边缘10像素
        int coordX = screenWidth - 120; // 留出足够空间显示坐标
        int coordY = 10;
        
        // 构建坐标文本
        String coordText = String.format("坐标: (%d, %d)", characterX, characterY);
        String stateText = "状态: " + getStateName(currentState);
        String edgeText = "边缘: " + getEdgeName(peekEdge);
        String axisLockText = String.format("轴锁定: X=%s Y=%s", 
                isXAxisLocked ? "锁定" : "解锁", 
                isYAxisLocked ? "锁定" : "解锁");
        
        // 设置文本颜色（白色半透明）
        int textColor = 0xFFFFFF | (0x80 << 24); // 白色，50%透明度
        
        // 绘制背景框（黑色半透明）
        int padding = 4;
        int textHeight = minecraft.font.lineHeight;
        int boxWidth = 110;
        int boxHeight = textHeight * 4 + padding * 5;
        
        guiGraphics.fill(coordX - padding, coordY - padding, 
                        coordX + boxWidth, coordY + boxHeight, 0x80000000);
        
        // 绘制坐标文本
        guiGraphics.drawString(minecraft.font, coordText, coordX, coordY, textColor);
        
        // 绘制状态文本
        guiGraphics.drawString(minecraft.font, stateText, coordX, coordY + textHeight + padding, textColor);
        
        // 绘制边缘信息文本
        guiGraphics.drawString(minecraft.font, edgeText, coordX, coordY + (textHeight + padding) * 2, textColor);
        
        // 绘制轴锁定信息文本
        guiGraphics.drawString(minecraft.font, axisLockText, coordX, coordY + (textHeight + padding) * 3, textColor);
    }
    
    /**
     * 获取状态名称
     */
    private static String getStateName(CharacterState state) {
        switch (state) {
            case WAITING: return "等待";
            case PEEKING: return "探头";
            case JUMPING_OUT: return "跳出";
            case ROTATING: return "转圈";
            case JUMPING: return "跳跃";
            case MOVING: return "移动";
            case REACTING: return "反应";
            case FLINCHING: return "张望";
            case FLEEING: return "逃跑";
            case HIDE_RECOVERY: return "恢复";
            case SHOW_ALICE2: return "Alice2";
            default: return "未知";
        }
    }
    
    /**
     * 获取边缘名称
     */
    private static String getEdgeName(PeekEdge edge) {
        switch (edge) {
            case LEFT: return "左侧";
            case RIGHT: return "右侧";
            case BOTTOM: return "底部";
            default: return "未知";
        }
    }
    
    /**
     * 播放第一首背景音乐（进入标题界面时播放）
     */
    private static void playBGM1(Minecraft minecraft) {
        if (bgm1Played || ModSounds.TITLE_SCREEN_BGM1 == null) {
            return;
        }
        
        SoundManager soundManager = minecraft.getSoundManager();
        if (soundManager != null) {
            // 创建背景音乐实例，使用环境音源（不受音乐音量滑块影响），循环播放
            bgm1Instance = new SimpleSoundInstance(
                ModSounds.TITLE_SCREEN_BGM1.getLocation(),
                SoundSource.AMBIENT, // 使用环境音源，不受音乐音量控制
                0.5F, // 音量
                1.0F, // 音调
                RandomSource.create(), // 随机
                true, // 不重复播放（但我们设置循环）
                0, // 衰减距离
                SoundInstance.Attenuation.NONE, // 无衰减
                0.0D, 0.0D, 0.0D, // 位置（全局音效）
                true // 循环播放
            );
            soundManager.play(bgm1Instance);
            bgm1Played = true;
        }
    }
    
    /**
     * 播放第二首背景音乐（第二次点击和文字出现时播放）
     */
    private static void playBGM2(Minecraft minecraft) {
        if (bgm2Played || ModSounds.TITLE_SCREEN_BGM2 == null) {
            return;
        }
        
        SoundManager soundManager = minecraft.getSoundManager();
        if (soundManager != null) {
            // 停止第一首背景音乐
            if (bgm1Instance != null) {
                soundManager.stop(bgm1Instance);
            }
            
            // 创建第二首背景音乐实例，使用环境音源（不受音乐音量滑块影响），循环播放
            bgm2Instance = new SimpleSoundInstance(
                ModSounds.TITLE_SCREEN_BGM2.getLocation(),
                SoundSource.AMBIENT, // 使用环境音源，不受音乐音量控制
                0.7F, // 音量
                1.0F, // 音调
                RandomSource.create(), // 随机
                true, // 不重复播放（但我们设置循环）
                0, // 衰减距离
                SoundInstance.Attenuation.NONE, // 无衰减
                0.0D, 0.0D, 0.0D, // 位置（全局音效）
                true // 循环播放
            );
            soundManager.play(bgm2Instance);
            bgm2Played = true;
        }
    }
    
    /**
     * 停止所有背景音乐
     */
    private static void stopAllBGM(Minecraft minecraft) {
        SoundManager soundManager = minecraft.getSoundManager();
        if (soundManager != null) {
            if (bgm1Instance != null) {
                soundManager.stop(bgm1Instance);
                bgm1Instance = null;
            }
            if (bgm2Instance != null) {
                soundManager.stop(bgm2Instance);
                bgm2Instance = null;
            }
        }
        bgm1Played = false;
        bgm2Played = false;
    }
}