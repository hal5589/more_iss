package nekotori_haru.more_iss.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4f;

import java.awt.*;

public class RenderUtils {

    public static final String WAVE_MARKER = ":_M";

    private static final float SATURATION = 0.15f;
    private static final float BRIGHTNESS = 1.0f;

    public static void renderWavingTextDirect(
            Font font,
            String text,
            float x,
            float y,
            float time,
            float speed,
            float amplitude,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            boolean dropShadow
    ) {
        Matrix4f matrix = poseStack.last().pose();
        float currentX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);

            float waveY = y + (float) Math.sin(time * speed + i * 0.5) * amplitude;

            long now = System.currentTimeMillis();
            float hue = (now % 2000) / 2000f;
            float offset = (float) i / text.length();
            float charHue = (hue + offset) % 1.0f;
            int color = Color.HSBtoRGB(charHue, SATURATION, BRIGHTNESS);

            font.drawInBatch(
                    charStr, currentX, waveY, color, dropShadow,
                    matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
            currentX += font.width(charStr);
        }
    }

    public static MutableComponent withWaveMarker(String text) {
        return Component.literal(WAVE_MARKER + text);
    }
}