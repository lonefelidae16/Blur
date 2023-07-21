package com.tterrag.blur;

import com.tterrag.blur.config.BlurConfig;
import eu.midnightdust.lib.util.MidnightColorUtil;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import ladysnake.satin.api.managed.uniform.Uniform1i;
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
            shader -> {
                shader.setUniformValue("DeltaEnd", BlurConfig.MAX_STRENGTH);
                shader.setUniformValue("EffectMax", getSmoothMax());
                updateKernel();
            });
    /**
     * The strength of Blur
     */
    private static final Uniform1i BLUR_STRENGTH = blur.findUniform1i("Strength");
    /**
     * Specify the Gaussian kernel
     */
    private static final UniformMat4 GAUSS_KERNEL = blur.findUniformMat4("Kernel");
    /**
     * Specify the max value of Smooth effect
     */
    private static final Uniform1i SMOOTH_MAX = blur.findUniform1i("EffectMax");
    private static int currentDist = -1;
    private static final float[][] GAUSSIAN_DISTRIBUTIONS = new float[BlurConfig.MAX_STRENGTH][];
    private static final int MAT4_SIZE = 4 * 4;

    static {
        // -- static init for Gaussian distributions
        for (int i = 0; i < BlurConfig.MAX_STRENGTH; ++i) {
            final int radius = i + 1;
            GAUSSIAN_DISTRIBUTIONS[i] = generateGauss(MAT4_SIZE, radius);
        }
        // -- end of init Gaussian distributions
    }

    @Override
    public void onInitializeClient() {
        BlurConfig.init("blur", BlurConfig.class);

        ShaderEffectRenderCallback.EVENT.register((deltaTick) -> {
            if (start > 0) {
                int strength = getStrengthWithProgress();
                BLUR_STRENGTH.set(strength);
                int distIdx = Math.min(strength - 1, BlurConfig.MAX_STRENGTH - 1);
                if (currentDist != distIdx && distIdx >= 0) {
                    currentDist = distIdx;
                    updateKernel();
                }
                blur.render(deltaTick);
            }
        });
    }

    private static boolean doFade = false;

    public static void onScreenChange(Screen newGui) {
        if (client.world != null) {
            boolean excluded = newGui == null || BlurConfig.blurExclusions.stream().anyMatch(exclusion -> newGui.getClass().getName().contains(exclusion));
            BLUR_STRENGTH.set(getStrengthWithProgress());
            SMOOTH_MAX.set(getSmoothMax());
            if (!excluded) {
                screenHasBackground = false;
                if (BlurConfig.showScreenTitle) System.out.println(newGui.getClass().getName());
                if (doFade) {
                    start = System.currentTimeMillis();
                    doFade = false;
                }
                prevScreen = newGui.getClass().getName();
            } else if (newGui == null && BlurConfig.fadeOutTimeMillis > 0 && !Objects.equals(prevScreen, "")) {
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

    private static int getStrengthWithProgress() {
        return MathHelper.floor(BlurConfig.strength * getProgress(client.currentScreen != null));
    }

    private static int getSmoothMax() {
        return MathHelper.lerp((float) BlurConfig.quality / BlurConfig.MAX_QUALITY, 0, 100);
    }

    private static void updateKernel() {
        if (currentDist < 0) {
            return;
        }

        // TODO: waiting to resolve an issue https://github.com/Ladysnake/Satin/issues/34
//        diff --git a/src/main/java/com/tterrag/blur/Blur.java b/src/main/java/com/tterrag/blur/Blur.java
//        index 012e868..39a67f0 100644
//        --- a/src/main/java/com/tterrag/blur/Blur.java
//        +++ b/src/main/java/com/tterrag/blur/Blur.java
//        @@ -130,9 +130,7 @@ public class Blur implements ClientModInitializer {
//                return;
//                }
//
//        -        Matrix4f mat4 = new Matrix4f();
//        -        mat4.set(GAUSSIAN_DISTRIBUTIONS[currentDist]);
//        -        GAUSS_KERNEL.set(mat4);
//        +        GAUSS_KERNEL.set(GAUSSIAN_DISTRIBUTIONS[currentDist]);
//        }
//
//        /**

        Matrix4f mat4 = new Matrix4f();
        mat4.set(GAUSSIAN_DISTRIBUTIONS[currentDist]);
        GAUSS_KERNEL.set(mat4);
    }

    /**
     * Calculates Gaussian distribution.
     *
     * @param size The kernel size.
     * @param strength The strength.
     * @return Generated float array.
     */
    private static float[] generateGauss(final int size, int strength) {
        if (size <= 0) {
            return new float[0];
        }

        final float[] generated = new float[size];
        float peak = 0;
        final float sigma = (size - 1) * 0.16666667f;
        final float d = sigma * strength * strength * 0.35f;
        for (int i = 0; i < size; ++i) {
            // Create distribution
            int r = 1 + 2 * i;
            float weight = (float) Math.exp(-0.5 * (r * r) / d);
            generated[i] = weight;
            if (i > 0) {
                weight *= 2f;
            }
            peak += weight;
        }
        for (int i = 0; i < size; ++i) {
            // Normalize
            generated[i] /= peak;
        }
        return generated;
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
