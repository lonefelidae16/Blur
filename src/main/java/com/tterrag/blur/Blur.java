package com.tterrag.blur;

import com.tterrag.blur.config.BlurConfig;
import eu.midnightdust.lib.util.MidnightColorUtil;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import ladysnake.satin.api.managed.uniform.Uniform1f;
import ladysnake.satin.api.managed.uniform.UniformMat4;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.Objects;

public class Blur implements ClientModInitializer {

    public static final String MODID = "blur";
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static long start;
    public static String prevScreen;
    public static boolean screenHasBackground;

    private static final ManagedShaderEffect blur = ShaderEffectManager.getInstance().manage(new Identifier(MODID, "shaders/post/fade_in_blur.json"),
            shader -> shader.setUniformValue("Radius", (float) BlurConfig.radius));
    private static final Uniform1f blurProgress = blur.findUniform1f("Progress");
    private static final UniformMat4 GAUSSIAN_KERNEL = blur.findUniformMat4("Kernel");
    private static int currentRadius = 0;

    @Override
    public void onInitializeClient() {
        BlurConfig.init("blur", BlurConfig.class);

        ShaderEffectRenderCallback.EVENT.register((deltaTick) -> {
            if (start > 0) {
                float progress = getProgress(client.currentScreen != null);
                blurProgress.set(progress);
                int progRadius = MathHelper.floor(progress * BlurConfig.radius);
                if (currentRadius != progRadius && progRadius > 0) {
                    // Re-calculation for Kernel
                    currentRadius = progRadius;
                    float[] weight = new float[16];
                    float peak = 0;
                    int strength = progRadius * progRadius + 1;
                    for (int i = 0; i < weight.length; ++i) {
                        float r = 1f + 2f * i;
                        float w = (float) Math.exp(-0.5 * (r * r) / strength);
                        weight[i] = w;
                        if (i > 0) {
                            w *= 2f;
                        }
                        peak += w;
                    }
                    for (int i = 0; i < weight.length; ++i) {
                        weight[i] /= peak;
                    }
                    Matrix4f mat4 = new Matrix4f();
                    mat4.set(weight);
                    GAUSSIAN_KERNEL.set(mat4);
                }
                blur.render(deltaTick);
            }
        });
    }

    private static boolean doFade = false;

    public static void onScreenChange(Screen newGui) {
        if (client.world != null) {
            boolean excluded = newGui == null || BlurConfig.blurExclusions.stream().anyMatch(exclusion -> newGui.getClass().getName().contains(exclusion));
            if (!excluded) {
                screenHasBackground = false;
                if (BlurConfig.showScreenTitle) System.out.println(newGui.getClass().getName());
                blur.setUniformValue("Radius", (float) BlurConfig.radius);
                if (doFade) {
                    start = System.currentTimeMillis();
                    doFade = false;
                }
                prevScreen = newGui.getClass().getName();
            } else if (newGui == null && BlurConfig.fadeOutTimeMillis > 0 && !Objects.equals(prevScreen, "")) {
                blur.setUniformValue("Radius", (float) BlurConfig.radius);
                start = System.currentTimeMillis();
                doFade = true;
            } else {
                screenHasBackground = false;
                start = -1;
                doFade = true;
                prevScreen = "";
            }
        }
    }

    private static float getProgress(boolean fadeIn) {
        float x;
        if (fadeIn) {
            x = Math.min((System.currentTimeMillis() - start) / (float) BlurConfig.fadeTimeMillis, 1);
            if (BlurConfig.ease) x *= (2 - x);  // easeInCubic
        }
        else {
            x = Math.max(1 + (start - System.currentTimeMillis()) / (float) BlurConfig.fadeOutTimeMillis, 0);
            if (BlurConfig.ease) x *= (2 - x);  // easeOutCubic
            if (x <= 0) {
                start = 0;
                screenHasBackground = false;
            }
        }
        return x;
    }

    public static int getBackgroundColor(boolean second, boolean fadeIn) {
        int a = second ? BlurConfig.gradientEndAlpha : BlurConfig.gradientStartAlpha;
        var col = MidnightColorUtil.hex2Rgb(second ? BlurConfig.gradientEnd : BlurConfig.gradientStart);
        int r = (col.getRGB() >> 16) & 0xFF;
        int b = (col.getRGB() >> 8) & 0xFF;
        int g = col.getRGB() & 0xFF;
        float prog = getProgress(fadeIn);
        a *= prog;
        r *= prog;
        g *= prog;
        b *= prog;
        return a << 24 | r << 16 | b << 8 | g;
    }
}
