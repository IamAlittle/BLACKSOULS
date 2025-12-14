package com.iamalittle.black_souls_options.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 打字机效果文本显示组件
 * 支持逐字显示效果
 */
public class TypewriterText {
    private final Font font;
    private String fullText;
    private String currentText;
    private int currentIndex;
    private long lastUpdateTime;
    private final long charDelay; // 每个字符的显示延迟（毫秒）
    private boolean isComplete;
    private boolean isPlaying;
    private float fontSizeScale; // 字体大小缩放比例
    
    /**
     * 构造函数
     * @param font 字体渲染器
     * @param charDelay 字符显示延迟（毫秒）
     */
    public TypewriterText(Font font, long charDelay) {
        this.font = font;
        this.charDelay = charDelay;
        this.fontSizeScale = 1.0f; // 默认字体大小缩放比例为1.0
        this.fullText = "";
        this.currentText = "";
        this.currentIndex = 0;
        this.lastUpdateTime = 0;
        this.isComplete = false;
        this.isPlaying = false;
    }
    
    /**
     * 构造函数（带字体大小缩放）
     * @param font 字体渲染器
     * @param charDelay 字符显示延迟（毫秒）
     * @param fontSizeScale 字体大小缩放比例
     */
    public TypewriterText(Font font, long charDelay, float fontSizeScale) {
        this.font = font;
        this.charDelay = charDelay;
        this.fontSizeScale = fontSizeScale;
        this.fullText = "";
        this.currentText = "";
        this.currentIndex = 0;
        this.lastUpdateTime = 0;
        this.isComplete = false;
        this.isPlaying = false;
    }
    
    /**
     * 设置要显示的文本
     */
    public void setText(String text) {
        this.fullText = text;
        this.currentText = "";
        this.currentIndex = 0;
        this.isComplete = false;
        this.isPlaying = false;
    }
    
    /**
     * 设置字体大小缩放比例
     * @param fontSizeScale 字体大小缩放比例（1.0为原始大小，0.5为一半大小，2.0为两倍大小）
     */
    public void setFontSizeScale(float fontSizeScale) {
        this.fontSizeScale = fontSizeScale;
    }
    
    /**
     * 获取当前字体大小缩放比例
     */
    public float getFontSizeScale() {
        return fontSizeScale;
    }
    
    /**
     * 开始播放打字机效果
     */
    public void start() {
        if (fullText.isEmpty()) {
            isComplete = true;
            return;
        }
        
        isPlaying = true;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 更新文本显示进度
     */
    public void update() {
        if (!isPlaying || isComplete) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= charDelay) {
            if (currentIndex < fullText.length()) {
                currentText = fullText.substring(0, currentIndex + 1);
                currentIndex++;
                lastUpdateTime = currentTime;
            } else {
                isComplete = true;
                isPlaying = false;
            }
        }
    }
    
    /**
     * 立即完成文本显示
     */
    public void complete() {
        if (!isComplete) {
            currentText = fullText;
            currentIndex = fullText.length();
            isComplete = true;
            isPlaying = false;
        }
    }
    
    /**
     * 重置文本显示
     */
    public void reset() {
        currentText = "";
        currentIndex = 0;
        isComplete = false;
        isPlaying = false;
    }
    
    /**
     * 渲染文本
     * @param guiGraphics GUI图形上下文
     * @param x 文本X坐标
     * @param y 文本Y坐标
     * @param color 文本颜色
     */
    public void render(GuiGraphics guiGraphics, int x, int y, int color) {
        if (currentText.isEmpty()) {
            return;
        }
        
        // 修复换行符处理：确保\r\n也被正确分割
        String normalizedText = currentText.replace("\r\n", "\n");
        
        // 支持多行文本渲染和颜色代码
        String[] lines = normalizedText.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            List<ColoredTextSegment> segments = parseColorCodes(lines[i]);
            int currentX = x;
            
            for (ColoredTextSegment segment : segments) {
                guiGraphics.drawString(font, segment.getText(), currentX, y + i * font.lineHeight, segment.getColor());
                currentX += font.width(segment.getText());
            }
        }
    }
    
    /**
     * 渲染文本（带阴影效果）
     * @param guiGraphics GUI图形上下文
     * @param x 文本X坐标
     * @param y 文本Y坐标
     * @param textColor 文本颜色
     * @param shadowColor 阴影颜色
     */
    public void renderWithShadow(GuiGraphics guiGraphics, int x, int y, int textColor, int shadowColor) {
        if (currentText.isEmpty()) {
            return;
        }
        
        // 修复换行符处理：确保\r\n也被正确分割
        String normalizedText = currentText.replace("\r\n", "\n");
        
        String[] lines = normalizedText.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            List<ColoredTextSegment> segments = parseColorCodes(lines[i]);
            int currentX = x;
            
            for (ColoredTextSegment segment : segments) {
                guiGraphics.drawString(font, segment.getText(), currentX, y + i * font.lineHeight, segment.getColor(), true);
                currentX += font.width(segment.getText());
            }
        }
    }
    
    /**
     * 渲染文本（支持文本区域限制）
     * @param guiGraphics GUI图形上下文
     * @param x 文本区域X坐标
     * @param y 文本区域Y坐标
     * @param width 文本区域宽度
     * @param height 文本区域高度
     */
    public void render(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (currentText.isEmpty()) {
            return;
        }
        
        // 应用字体大小缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(fontSizeScale, fontSizeScale, fontSizeScale);
        
        // 调整坐标和尺寸以适应缩放
        int scaledX = (int)(x / fontSizeScale);
        int scaledY = (int)(y / fontSizeScale);
        int scaledWidth = (int)(width / fontSizeScale);
        int scaledHeight = (int)(height / fontSizeScale);
        
        // 分割文本为多行，考虑文本区域宽度限制和颜色代码
        List<ColoredTextLine> wrappedLines = wrapTextWithColors(currentText, scaledWidth);
        
        // 计算最大可显示行数
        int maxLines = scaledHeight / font.lineHeight;
        int linesToShow = Math.min(wrappedLines.size(), maxLines);
        
        // 渲染文本行（支持颜色代码）
        for (int i = 0; i < linesToShow; i++) {
            ColoredTextLine line = wrappedLines.get(i);
            int currentX = scaledX;
            
            for (ColoredTextSegment segment : line.getSegments()) {
                guiGraphics.drawString(font, segment.getText(), currentX, scaledY + i * font.lineHeight, segment.getColor(), true);
                currentX += font.width(segment.getText());
            }
        }
        
        guiGraphics.pose().popPose();
    }
    
    /**
     * 文本换行处理
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        String bracketedText = "「" + text + "」";
        
        String[] paragraphs = bracketedText.split("\\n");
        
        for (String paragraph : paragraphs) {
            StringBuilder currentLine = new StringBuilder();
            
            // 基于字符数量进行换行，适用于中文文本
            for (int i = 0; i < paragraph.length(); i++) {
                char currentChar = paragraph.charAt(i);
                
                // 测试添加字符后的宽度
                String testLine = currentLine.toString() + currentChar;
                int testWidth = font.width(testLine);
                
                if (testWidth <= maxWidth) {
                    // 可以添加到当前行
                    currentLine.append(currentChar);
                } else {
                    // 当前行已满，开始新行
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(String.valueOf(currentChar));
                }
            }
            
            // 添加最后一行
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    /**
     * 获取当前显示的文本
     */
    public String getCurrentText() {
        return currentText;
    }
    
    /**
     * 获取完整文本
     */
    public String getFullText() {
        return fullText;
    }
    
    /**
     * 检查是否显示完成
     */
    public boolean isComplete() {
        return isComplete;
    }
    
    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * 获取显示进度（0.0 - 1.0）
     */
    public float getProgress() {
        if (fullText.isEmpty()) {
            return 1.0f;
        }
        return (float) currentIndex / fullText.length();
    }
    
    /**
     * 获取文本宽度
     */
    public int getWidth() {
        if (currentText.isEmpty()) {
            return 0;
        }
        
        // 计算多行文本的最大宽度
        String[] lines = currentText.split("\\n");
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        return maxWidth;
    }
    
    /**
     * 获取文本高度
     */
    public int getHeight() {
        if (currentText.isEmpty()) {
            return 0;
        }
        
        String[] lines = currentText.split("\\n");
        return lines.length * font.lineHeight;
    }
    
    /**
     * 带颜色代码的文本换行处理
     */
    private List<ColoredTextLine> wrapTextWithColors(String text, int maxWidth) {
        List<ColoredTextLine> lines = new ArrayList<>();
        
        // 修复换行符处理：确保\r\n也被正确分割
        String normalizedText = text.replace("\r\n", "\n");
        
        // 在文本头尾添加方括号
        String bracketedText = "「" + normalizedText + "」";
        
        String[] paragraphs = bracketedText.split("\\n");
        
        for (String paragraph : paragraphs) {
            ColoredTextLine currentLine = new ColoredTextLine();
            int currentLineWidth = 0;
            
            // 解析颜色代码并处理换行
            List<ColoredTextSegment> segments = parseColorCodes(paragraph);
            
            for (ColoredTextSegment segment : segments) {
                String segmentText = segment.getText();
                int segmentWidth = font.width(segmentText);
                
                if (currentLineWidth + segmentWidth <= maxWidth) {
                    // 可以添加到当前行
                    currentLine.addSegment(segment);
                    currentLineWidth += segmentWidth;
                } else {
                    // 当前行已满，检查是否需要分割文本段
                    if (segmentWidth > maxWidth) {
                        // 文本段本身就很长，需要字符级别的分割
                        List<ColoredTextSegment> splitSegments = splitTextSegment(segment, maxWidth - currentLineWidth);
                        
                        for (ColoredTextSegment splitSegment : splitSegments) {
                            int splitWidth = font.width(splitSegment.getText());
                            
                            if (currentLineWidth + splitWidth <= maxWidth) {
                                // 可以添加到当前行
                                currentLine.addSegment(splitSegment);
                                currentLineWidth += splitWidth;
                            } else {
                                // 当前行已满，开始新行
                                if (!currentLine.isEmpty()) {
                                    lines.add(currentLine);
                                }
                                currentLine = new ColoredTextLine();
                                currentLine.addSegment(splitSegment);
                                currentLineWidth = splitWidth;
                            }
                        }
                    } else {
                        // 当前行已满，开始新行
                        if (!currentLine.isEmpty()) {
                            lines.add(currentLine);
                        }
                        currentLine = new ColoredTextLine();
                        currentLine.addSegment(segment);
                        currentLineWidth = segmentWidth;
                    }
                }
            }
            
            // 添加最后一行
            if (!currentLine.isEmpty()) {
                lines.add(currentLine);
            }
        }
        
        return lines;
    }
    
    /**
     * 分割长文本段为多个小文本段（字符级别分割）
     */
    private List<ColoredTextSegment> splitTextSegment(ColoredTextSegment segment, int availableWidth) {
        List<ColoredTextSegment> splitSegments = new ArrayList<>();
        String text = segment.getText();
        int color = segment.getColor();
        
        StringBuilder currentText = new StringBuilder();
        int currentWidth = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int charWidth = font.width(String.valueOf(c));
            
            if (currentWidth + charWidth <= availableWidth) {
                // 可以添加到当前段
                currentText.append(c);
                currentWidth += charWidth;
            } else {
                // 当前段已满，保存并开始新段
                if (currentText.length() > 0) {
                    splitSegments.add(new ColoredTextSegment(currentText.toString(), color));
                }
                currentText = new StringBuilder(String.valueOf(c));
                currentWidth = charWidth;
                // 后续段仍然使用最大宽度限制，确保能够正确换行
                // availableWidth 保持不变
            }
        }
        
        // 添加最后一个段
        if (currentText.length() > 0) {
            splitSegments.add(new ColoredTextSegment(currentText.toString(), color));
        }
        
        return splitSegments;
    }
    
    /**
     * 解析颜色代码
     * 支持格式：&c红色 &a绿色 &b蓝色 &e黄色 &f白色 &0黑色 &6橙色 &9浅蓝 &d粉色 &8灰色
     */
    private List<ColoredTextSegment> parseColorCodes(String text) {
        List<ColoredTextSegment> segments = new ArrayList<>();
        
        // 默认颜色为白色
        int currentColor = 0xFFFFFF;
        StringBuilder currentText = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '&' && i + 1 < text.length()) {
                // 遇到颜色代码
                char colorCode = text.charAt(i + 1);
                int newColor = getColorFromCode(colorCode);
                
                if (newColor != -1) {
                    // 保存当前文本段
                    if (currentText.length() > 0) {
                        segments.add(new ColoredTextSegment(currentText.toString(), currentColor));
                        currentText = new StringBuilder();
                    }
                    
                    // 更新颜色
                    currentColor = newColor;
                    i++; // 跳过颜色代码字符
                } else {
                    // 无效颜色代码，当作普通字符处理
                    currentText.append(c);
                }
            } else {
                currentText.append(c);
            }
        }
        
        // 添加最后一个文本段
        if (currentText.length() > 0) {
            segments.add(new ColoredTextSegment(currentText.toString(), currentColor));
        }
        
        return segments;
    }
    
    /**
     * 根据颜色代码获取颜色值
     */
    private int getColorFromCode(char code) {
        switch (code) {
            case '0': return 0x000000; // 黑色
            case '1': return 0x0000AA; // 深蓝
            case '2': return 0x00AA00; // 深绿
            case '3': return 0x00AAAA; // 青色
            case '4': return 0xAA0000; // 深红
            case '5': return 0xAA00AA; // 紫色
            case '6': return 0xFFAA00; // 金色
            case '7': return 0xAAAAAA; // 灰色
            case '8': return 0x555555; // 深灰
            case '9': return 0x5555FF; // 浅蓝
            case 'a': return 0x55FF55; // 浅绿
            case 'b': return 0x55FFFF; // 天蓝
            case 'c': return 0xFF5555; // 红色
            case 'd': return 0xFF55FF; // 粉色
            case 'e': return 0xFFFF55; // 黄色
            case 'f': return 0xFFFFFF; // 白色
            case 'r': return 0xFFFFFF; // 重置为白色
            default: return -1; // 无效代码
        }
    }
    
    /**
     * 带颜色的文本段
     */
    private static class ColoredTextSegment {
        private final String text;
        private final int color;
        
        public ColoredTextSegment(String text, int color) {
            this.text = text;
            this.color = color;
        }
        
        public String getText() {
            return text;
        }
        
        public int getColor() {
            return color;
        }
    }
    
    /**
     * 带颜色的文本行（包含多个文本段）
     */
    private static class ColoredTextLine {
        private final List<ColoredTextSegment> segments;
        
        public ColoredTextLine() {
            this.segments = new ArrayList<>();
        }
        
        public void addSegment(ColoredTextSegment segment) {
            segments.add(segment);
        }
        
        public List<ColoredTextSegment> getSegments() {
            return segments;
        }
        
        public boolean isEmpty() {
            return segments.isEmpty();
        }
    }
}