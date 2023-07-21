package com.tterrag.blur.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.List;

public class BlurConfig extends MidnightConfig {
    public static final String style = "style";
    public static final String screens = "screens";
    public static final int MAX_STRENGTH = 100;
    public static final int MAX_QUALITY = 8;


    @Entry(category = screens)
    public static List<String> blurExclusions = Lists.newArrayList(ChatScreen.class.getName(),
        "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay$UserInputGuiScreen",
        "ai.arcblroth.projectInception.client.InceptionInterfaceScreen",
        "net.optifine.gui.GuiChatOF",
        "io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen",
        "net.coderbot.iris.gui.screen.ShaderPackScreen");
    @Entry(category = style, min = 0, max = 5000, width = 4)
    public static int fadeTimeMillis = 200;
    @Entry(category = style, min = 0, max = 5000, width = 4)
    public static int fadeOutTimeMillis = 200;
    @Entry(category = style)
    public static boolean ease = true;
    @Entry(category = style, isSlider = true, min = 0, max = MAX_STRENGTH)
    public static int strength = 8;
    @Entry(category = style, isSlider = true, min = 1, max = MAX_QUALITY)
    public static int quality = 6;
    @Entry(category = style, isColor = true, width = 7, min = 7)
    public static String gradientStart = "#000000";
    @Entry(category = style, isSlider = true, min = 0, max = 255)
    public static int gradientStartAlpha = 75;
    @Entry(category = style, isColor = true, width = 7, min = 7)
    public static String gradientEnd = "#000000";
    @Entry(category = style, isSlider = true, min = 0, max = 255)
    public static int gradientEndAlpha = 75;
    @Entry(category = screens)
    public static boolean showScreenTitle = false;
}