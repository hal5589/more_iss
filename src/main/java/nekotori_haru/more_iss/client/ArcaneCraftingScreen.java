package nekotori_haru.more_iss.client;

import com.mojang.blaze3d.systems.RenderSystem;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArcaneCraftingScreen extends AbstractContainerScreen<ArcaneCraftingMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("more_iss", "textures/gui/arcane_crafting_table.png");

    // GUI サイズ（テクスチャに合わせる）
    private static final int GUI_WIDTH  = 177;
    private static final int GUI_HEIGHT = 246;

    // 8スロットが並ぶ円の中心・半径（スロット座標の中心）
    private static final int CX = 88;
    private static final int CY = 75;

    // クラフトアニメーション進捗 (0.0 = 開始, 1.0 = 完成直前)
    private float animProgress = 0f;

    // 完成フラッシュ演出用
    private float burstProgress = 0f;   // 0→1 で完成エフェクト再生
    private boolean wasCrafting = false;

    // 周囲→中央 へ流れるパーティクル
    private static final class Particle {
        float srcX, srcY;   // 出発スロット中心（GUI相対）
        float t;            // 0.0(出発) → 1.0(中央到着)
        float speed;
        int color;
    }
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    private static final double[] ANGLES = {
            -Math.PI / 2,           // 0: 上
            -Math.PI / 4,           // 1: 右上
            0,                     // 2: 右
            Math.PI / 4,           // 3: 右下
            Math.PI / 2,           // 4: 下
            3 * Math.PI / 4,       // 5: 左下
            Math.PI,               // 6: 左
            -3 * Math.PI / 4,       // 7: 左上
    };
    private static final double RADIUS = 48.0;

    public ArcaneCraftingScreen(ArcaneCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        // インベントリラベルの Y 位置
        this.inventoryLabelY = GUI_HEIGHT - 94;
    }

    // ─── 背景描画 ────────────────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width  - GUI_WIDTH)  / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // テクスチャをそのまま描画（256x256 テクスチャの左上 177x246 を使用）
        gfx.blit(TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        // ── クラフト中のみエフェクト ──────────────────────────────────────
        boolean crafting = menu.isCraftingActive();
        int tick = menu.getCraftingTick();

        if (crafting) {
            float prog = 1f - tick / 100f;   // 0.0(開始) → 1.0(完成)
            animProgress = prog;

            // パーティクル生成
            spawnParticles(x, y);

            // パーティクル描画
            drawParticles(gfx, x, y, partialTick);

            // 回転リング
            drawRotatingRing(gfx, x + CX, y + CY, prog);

            // 完成間近（prog > 0.85）でフラッシュ予備動作
            if (prog > 0.85f) {
                float pulse = (prog - 0.85f) / 0.15f;
                int alpha = (int)(pulse * 80);
                gfx.fill(x + CX - 8, y + CY - 8,
                        x + CX + 8, y + CY + 8,
                        (alpha << 24) | 0xCCAAFF);
            }

            wasCrafting = true;
        } else {
            animProgress = 0f;
            particles.clear();

            // 完成バースト演出
            if (wasCrafting && burstProgress == 0f) {
                burstProgress = 0.01f;
            }
            wasCrafting = false;
        }

        // バースト演出（クラフト完了後）
        if (burstProgress > 0f) {
            drawBurstEffect(gfx, x + CX, y + CY, burstProgress);
            burstProgress += 0.045f;
            if (burstProgress >= 1f) burstProgress = 0f;
        }
    }

    // ─── ラベル描画 ──────────────────────────────────────────────────────────
    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // タイトルラベル（GUI上部）
        gfx.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0xE0D0FF, false);

        // インベントリラベル
        gfx.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 触媒スロットのラベル
        gfx.drawString(this.font,
                Component.translatable("gui.more_iss.catalyst"),
                152 - 28, 2, 0xA090CC, false);

        // クラフト中：進捗バー
        if (menu.isCraftingActive()) {
            int tick = menu.getCraftingTick();
            int barW = (int)(52f * (1f - tick / 100f));
            // 背景（暗い）
            gfx.fill(68, 158, 120, 163, 0xFF2A0A5B);
            // 前景（紫）
            gfx.fill(68, 158, 68 + barW, 163, 0xFF9B59EF);
        }
    }

    // ─── 全体描画 ────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // パーティクル管理
    // ═══════════════════════════════════════════════════════════════════════

    /** クラフト中、各スロット位置からパーティクルを一定確率でスポーン */
    private void spawnParticles(int guiLeft, int guiTop) {
        // 空のスロットからはスポーンしない（描画コストを抑える）
        for (int i = 0; i < 8; i++) {
            if (!menu.getSlot(i).hasItem()) continue;
            if (rng.nextFloat() > 0.18f) continue;   // スポーン確率

            double angle = ANGLES[i];
            float sx = CX + (float)(RADIUS * Math.cos(angle));
            float sy = CY + (float)(RADIUS * Math.sin(angle));

            Particle p = new Particle();
            p.srcX  = sx;
            p.srcY  = sy;
            p.t     = 0f;
            p.speed = 0.018f + rng.nextFloat() * 0.012f;
            // 青紫〜白の色
            int r = 140 + rng.nextInt(80);
            int g = 80  + rng.nextInt(80);
            int b = 220 + rng.nextInt(36);
            p.color = (r << 16) | (g << 8) | b;
            particles.add(p);
        }
        // 古いパーティクルを削除
        particles.removeIf(p -> p.t >= 1f);
    }

    /** パーティクルを描画し、t を進める */
    private void drawParticles(GuiGraphics gfx, int guiLeft, int guiTop, float partialTick) {
        for (Particle p : particles) {
            // 補間: 出発点 → 中心
            float x = p.srcX + (CX - p.srcX) * p.t;
            float y = p.srcY + (CY - p.srcY) * p.t;

            // 中央に近いほど小さく・薄くなる
            float size = (float)(1.0 - p.t * 0.5);          // 1.0 → 0.5
            int alpha  = (int)(255 * (1f - p.t * 0.7f));

            int px = guiLeft + (int)x;
            int py = guiTop  + (int)y;
            int half = Math.max(1, (int)size);

            int color = (alpha << 24) | p.color;
            gfx.fill(px - half, py - half, px + half, py + half, color);

            p.t += p.speed;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 回転リング（クラフト進捗に応じて拡大）
    // ═══════════════════════════════════════════════════════════════════════
    private void drawRotatingRing(GuiGraphics gfx, int cx, int cy, float prog) {
        int   dotCount = 12;
        long  ms       = System.currentTimeMillis();
        float baseAngle = (ms % 2000) / 2000f * (float)(2 * Math.PI);
        float ringR    = 10f + prog * 12f;   // progに応じて広がる（10→22px）

        for (int i = 0; i < dotCount; i++) {
            float a   = baseAngle + i * (float)(2 * Math.PI) / dotCount;
            int   rx  = cx + (int)(Math.cos(a) * ringR);
            int   ry  = cy + (int)(Math.sin(a) * ringR);
            int   alpha = (int)(140 + 100 * (i + 1f) / dotCount);
            int   color = (alpha << 24) | 0x9966FF;
            gfx.fill(rx - 1, ry - 1, rx + 1, ry + 1, color);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 完成バースト演出（中央から四方に光が弾ける）
    // ═══════════════════════════════════════════════════════════════════════
    private void drawBurstEffect(GuiGraphics gfx, int cx, int cy, float t) {
        // t: 0→1、easeOut
        float ease = 1f - (1f - t) * (1f - t);

        // 光線を8方向に伸ばす
        int rayCount = 12;
        for (int i = 0; i < rayCount; i++) {
            double angle = i * 2 * Math.PI / rayCount;
            float len   = ease * 28f;
            float alpha = (1f - ease) * 255f;
            int   x2    = cx + (int)(Math.cos(angle) * len);
            int   y2    = cy + (int)(Math.sin(angle) * len);

            int ia = (int)alpha;
            int color = (ia << 24) | 0xFFEEFF;
            gfx.fill(x2 - 1, y2 - 1, x2 + 1, y2 + 1, color);
        }

        // 中央フラッシュ（白→透明）
        int fa    = (int)((1f - ease) * 200f);
        int fSize = (int)(ease * 14f);
        gfx.fill(cx - fSize, cy - fSize, cx + fSize, cy + fSize,
                (fa << 24) | 0xFFFFFF);
    }
}