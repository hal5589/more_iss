package nekotori_haru.more_iss.client;

import com.mojang.blaze3d.systems.RenderSystem;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ArcaneCraftingScreen extends AbstractContainerScreen<ArcaneCraftingMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("more_iss", "textures/gui/arcane_crafting_table.png");

    // GUI サイズ（テクスチャ: 175x243）
    private static final int GUI_WIDTH  = 175;
    private static final int GUI_HEIGHT = 243;

    // 円形UI の中心（画像内座標）
    private static final int CX = 83;
    private static final int CY = 54;
    private static final int RADIUS = 37;

    // クラフトアニメーション進捗
    private float animProgress = 0f;

    public ArcaneCraftingScreen(ArcaneCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000;  // 非表示
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width  - GUI_WIDTH)  / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // テクスチャを描画
        gfx.blit(TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        // ── クラフト中のエフェクト ─────────────────────────────────────────
        if (menu.isCraftingActive()) {
            int tick = menu.getCraftingTick();
            float prog = 1f - (tick / 100f);
            animProgress = prog;

            // 円形エフェクト
            drawRotatingRing(gfx, x + CX, y + CY, prog);

            // 中央の光
            int centerAlpha = (int)(100 * prog);
            gfx.fill(x + CX - 8, y + CY - 8, x + CX + 8, y + CY + 8,
                    (centerAlpha << 24) | 0xCC99FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // タイトル
        gfx.drawString(this.font, this.title, 6, 6, 0xE0D0FF, false);

        // インベントリラベル
        gfx.drawString(this.font, this.playerInventoryTitle, 8, 102, 0x404040, false);

        // 🔥 削除: 触媒ラベル表示
        // gfx.drawString(this.font,
        //         Component.translatable("gui.more_iss.catalyst"),
        //         140, 35, 0xA090CC, false);

        // クラフト進捗バー
        if (menu.isCraftingActive()) {
            int tick = menu.getCraftingTick();
            int barW = (int)(40f * (1f - tick / 100f));
            gfx.fill(67, 107, 107, 112, 0xFF2A0A5B);  // 背景
            gfx.fill(67, 107, 67 + barW, 112, 0xFF9B59EF);  // 進捗
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    // ── 回転リング（クラフト進捗エフェクト） ──────────────────────────────
    private void drawRotatingRing(GuiGraphics gfx, int cx, int cy, float prog) {
        int dotCount = 12;
        long ms = System.currentTimeMillis();
        float angle = (ms % 2000) / 2000f * (float)(2 * Math.PI);
        float ringR = 8f + prog * 16f;

        for (int i = 0; i < dotCount; i++) {
            float a = angle + i * (float)(2 * Math.PI) / dotCount;
            int rx = cx + (int)(Math.cos(a) * ringR);
            int ry = cy + (int)(Math.sin(a) * ringR);
            int alpha = (int)(140 + 100 * (i + 1f) / dotCount);
            gfx.fill(rx - 1, ry - 1, rx + 1, ry + 1, (alpha << 24) | 0x9966FF);
        }
    }
}