package nekotori_haru.more_iss.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.util.RenderUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * EternalWizardEntity 専用ボスバーのクライアント側管理＋描画。
 *
 * バニラのボスバーとは独立して描画するため、バニラのボスバーはそのまま表示される。
 * 位置はバニラのボスバーの下に配置する。
 */
@Mod.EventBusSubscriber(modid = More_iss.MODID, value = Dist.CLIENT)
public class ClientBossBarOverlay {

    private static final int TIMEOUT_TICKS = 30;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 7;
    // ⭐ バニラのボスバーと重ならないように少し下にずらす（バニラは約 10〜20px 程度）
    private static final int BAR_TOP_MARGIN = 40;
    private static final int BAR_SPACING = 23;

    private static final float HUE_CYCLE_MS = 4000.0f;
    private static final float RAINBOW_SATURATION = 0.20f;
    private static final float RAINBOW_VALUE = 1.0f;
    private static final int CAP_WIDTH = 7;
    private static final int BORDER_BLACK = 1;
    private static final int BORDER_WHITE = 1;
    private static final int NAME_GAP = 2;

    private static final int DRAIN_DURATION_TICKS = 8;
    private static final int DRAIN_TRAIL_COLOR = 0xFFFFFFFF;
    private static final float DAMAGE_EPSILON = 0.0005f;

    private static final int JITTER_DURATION_TICKS = 8;
    private static final int JITTER_MAX_OFFSET_PX = 3;
    private static final int FLASH_DURATION_TICKS = 6;

    private static final Map<Integer, BarEntry> ACTIVE_BARS = new LinkedHashMap<>();

    private static class BarEntry {
        float progress;
        float displayedProgress;
        float drainStartProgress;
        int drainTicksElapsed;
        Component name;
        int ticksSinceUpdate;
        int jitterTicksRemaining;
        int flashTicksRemaining;

        BarEntry(float progress, Component name) {
            this.progress = progress;
            this.displayedProgress = progress;
            this.drainStartProgress = progress;
            this.drainTicksElapsed = DRAIN_DURATION_TICKS;
            this.name = name;
            this.ticksSinceUpdate = 0;
            this.jitterTicksRemaining = 0;
            this.flashTicksRemaining = 0;
        }
    }

    public static void updateBar(int entityId, float progress, Component name, boolean isHit) {
        BarEntry entry = ACTIVE_BARS.get(entityId);
        if (entry == null) {
            ACTIVE_BARS.put(entityId, new BarEntry(progress, name));
            return;
        }

        boolean damaged = progress < entry.progress - DAMAGE_EPSILON;
        boolean shouldTrigger = isHit || damaged;

        if (shouldTrigger) {
            entry.jitterTicksRemaining = JITTER_DURATION_TICKS;
            entry.flashTicksRemaining = FLASH_DURATION_TICKS;
        }

        if (damaged) {
            entry.drainStartProgress = entry.displayedProgress;
            entry.drainTicksElapsed = 0;
        }

        entry.progress = progress;
        entry.name = name;
        entry.ticksSinceUpdate = 0;

        if (progress > entry.displayedProgress) {
            entry.displayedProgress = progress;
            entry.drainStartProgress = progress;
            entry.drainTicksElapsed = DRAIN_DURATION_TICKS;
        }
    }

    public static void updateBar(int entityId, float progress, Component name) {
        updateBar(entityId, progress, name, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ACTIVE_BARS.entrySet().removeIf(e -> {
            BarEntry entry = e.getValue();
            entry.ticksSinceUpdate++;
            return entry.ticksSinceUpdate > TIMEOUT_TICKS;
        });

        for (BarEntry entry : ACTIVE_BARS.values()) {
            if (entry.drainTicksElapsed < DRAIN_DURATION_TICKS) {
                entry.drainTicksElapsed++;
                float t = Math.min(1.0f, entry.drainTicksElapsed / (float) DRAIN_DURATION_TICKS);
                entry.displayedProgress = entry.drainStartProgress + (entry.progress - entry.drainStartProgress) * t;
            } else {
                entry.displayedProgress = entry.progress;
            }
            if (entry.jitterTicksRemaining > 0) {
                entry.jitterTicksRemaining--;
            }
            if (entry.flashTicksRemaining > 0) {
                entry.flashTicksRemaining--;
            }
        }
    }

    // ⭐ バニラのボスバーはキャンセルせず、Post で独自バーを描画する
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }

        if (ACTIVE_BARS.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = BAR_TOP_MARGIN;

        long time = System.currentTimeMillis();

        for (BarEntry entry : ACTIVE_BARS.values()) {
            renderBar(graphics, x, y, entry, time);
            y += BAR_SPACING;
        }
    }

    // 以前の Pre イベントは削除（または何もしないように）
    // @SubscribeEvent を付けないか、空のメソッドにする

    private static float capInset(int x, int totalWidth, int halfHeight, int capWidth) {
        if (capWidth <= 0) return 0f;
        int distFromLeft = x;
        int distFromRight = totalWidth - 1 - x;
        int edgeDist = Math.min(distFromLeft, distFromRight);
        if (edgeDist >= capWidth) return 0f;

        float t = (capWidth - edgeDist) / (float) capWidth;
        return t * halfHeight;
    }

    private static void fillChevronShape(GuiGraphics graphics, int x, int y, int width, int height, int capWidth, int color) {
        int halfHeight = height / 2;
        for (int i = 0; i < width; i++) {
            float inset = capInset(i, width, halfHeight, capWidth);
            int top = y + Math.round(inset);
            int bottom = y + height - Math.round(inset);
            if (bottom > top) {
                graphics.fill(x + i, top, x + i + 1, bottom, color);
            }
        }
    }

    private static int blendWithWhite(int rgb, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = r + (int) ((255 - r) * t);
        g = g + (int) ((255 - g) * t);
        b = b + (int) ((255 - b) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, BarEntry entry, long time) {
        int jitterX = 0;
        int jitterY = 0;
        if (entry.jitterTicksRemaining > 0) {
            float strength = entry.jitterTicksRemaining / (float) JITTER_DURATION_TICKS;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            jitterX = Math.round((rnd.nextFloat() * 2.0f - 1.0f) * JITTER_MAX_OFFSET_PX * strength);
            jitterY = Math.round((rnd.nextFloat() * 2.0f - 1.0f) * JITTER_MAX_OFFSET_PX * strength);
        }
        int bx = x + jitterX;
        int by = y + jitterY;

        int margin = BORDER_BLACK + BORDER_WHITE;

        fillChevronShape(graphics,
                bx - margin, by - margin,
                BAR_WIDTH + margin * 2, BAR_HEIGHT + margin * 2,
                CAP_WIDTH + margin,
                0xFFFFFFFF
        );
        fillChevronShape(graphics,
                bx - BORDER_BLACK, by - BORDER_BLACK,
                BAR_WIDTH + BORDER_BLACK * 2, BAR_HEIGHT + BORDER_BLACK * 2,
                CAP_WIDTH + BORDER_BLACK,
                0xFF000000
        );
        fillChevronShape(graphics, bx, by, BAR_WIDTH, BAR_HEIGHT, CAP_WIDTH, 0xFF2A2A2A);

        String rawName = entry.name != null ? entry.name.getString() : "Boss";
        String displayName = rawName.replace(RenderUtils.WAVE_MARKER, "");
        int nameY = by - margin - NAME_GAP - 9;
        graphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                displayName,
                bx + BAR_WIDTH / 2,
                nameY,
                0xFFFFFF
        );

        float trueProgress = Mth.clamp(entry.progress, 0.0f, 1.0f);
        float shownProgress = Mth.clamp(entry.displayedProgress, 0.0f, 1.0f);
        int trueFilledWidth = Math.round(BAR_WIDTH * trueProgress);
        int shownFilledWidth = Math.round(BAR_WIDTH * shownProgress);

        int halfHeight = BAR_HEIGHT / 2;
        long cycle = (long) HUE_CYCLE_MS;
        float flashStrength = entry.flashTicksRemaining / (float) FLASH_DURATION_TICKS;

        for (int i = 0; i < trueFilledWidth; i++) {
            float inset = capInset(i, BAR_WIDTH, halfHeight, CAP_WIDTH);
            int top = by + Math.round(inset);
            int bottom = by + BAR_HEIGHT - Math.round(inset);
            if (bottom <= top) continue;

            float positionOffset = (float) i / BAR_WIDTH;
            float timeOffset = (time % cycle) / (float) cycle;
            float hue = (positionOffset + timeOffset) % 1.0f;

            int rgb = Mth.hsvToRgb(hue, RAINBOW_SATURATION, RAINBOW_VALUE);
            if (flashStrength > 0.0f) {
                rgb = blendWithWhite(rgb, flashStrength);
            }
            graphics.fill(bx + i, top, bx + i + 1, bottom, 0xFF000000 | rgb);
        }

        for (int i = trueFilledWidth; i < shownFilledWidth; i++) {
            float inset = capInset(i, BAR_WIDTH, halfHeight, CAP_WIDTH);
            int top = by + Math.round(inset);
            int bottom = by + BAR_HEIGHT - Math.round(inset);
            if (bottom <= top) continue;
            graphics.fill(bx + i, top, bx + i + 1, bottom, DRAIN_TRAIL_COLOR);
        }
    }
}