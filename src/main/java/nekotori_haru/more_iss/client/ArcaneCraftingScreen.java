package nekotori_haru.more_iss.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ArcaneCraftingScreen extends AbstractContainerScreen<ArcaneCraftingMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("more_iss", "textures/gui/arcane_crafting_table.png");

    // GUI サイズ
    private static final int GUI_WIDTH  = 196;
    private static final int GUI_HEIGHT = 210;

    // 円形スロット中心・半径 (スクリーン座標変換後)
    private static final int CX = 88;
    private static final int CY = 75;
    private static final int R  = 48;

    // アニメーション用パーティクル状態
    private float animProgress = 0f; // 0.0~1.0

    // 円形スロットの角度 (ラジアン)
    private static final double[] ANGLES = {
        -Math.PI / 2,               // 0: 上      (270°)
        -Math.PI / 2 + Math.PI/4,   // 1: 右上
        0,                           // 2: 右
        Math.PI / 4,                 // 3: 右下
        Math.PI / 2,                 // 4: 下
        Math.PI / 2 + Math.PI/4,    // 5: 左下
        Math.PI,                     // 6: 左
        Math.PI + Math.PI/4,        // 7: 左上
    };

    public ArcaneCraftingScreen(ArcaneCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        // 背景テクスチャ描画
        gfx.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // ---- クラフトアニメーション描画 ----
        if (menu.isCraftingActive()) {
            int tick   = menu.getCraftingTick();
            float prog = 1f - (tick / (float) nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity.CRAFT_DURATION);
            animProgress = prog;

            drawCraftingAnimation(gfx, x, y, prog, partialTick);
        } else {
            animProgress = 0f;
        }
    }

    /**
     * クラフト演出描画
     * prog: 0.0(開始) → 1.0(完成直前)
     *
     * 演出: 円形スロットのアイテムが中心に向かって移動する光のストリーク
     */
    private void drawCraftingAnimation(GuiGraphics gfx, int baseX, int baseY, float prog, float partialTick) {
        int scx = baseX + CX; // 画面上の中心X
        int scy = baseY + CY; // 画面上の中心Y

        // 各スロットから中心へ向かうエフェクト
        for (int i = 0; i < 8; i++) {
            // スロットの元の位置
            double angle = ANGLES[i];
            double sx = scx + Math.cos(angle) * R;
            double sy = scy + Math.sin(angle) * R;

            // prog に応じて中心に近づく
            double px = sx + (scx - sx) * prog;
            double py = sy + (scy - sy) * prog;

            // 光のドットを描画 (GUI Graphics で矩形)
            int dotX = (int) px;
            int dotY = (int) py;
            int alpha = (int)(255 * (1f - prog * 0.5f));

            // 外側のグロー (大きい半透明)
            gfx.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3,
                    (alpha / 3 << 24) | 0xAA88FF);
            // 内側のコア (小さい不透明)
            gfx.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1,
                    (alpha << 24) | 0xFFEEFF);
        }

        // 中心の輝き (prog が高いほど大きく)
        if (prog > 0.7f) {
            float flare = (prog - 0.7f) / 0.3f;
            int flareSize = (int)(flare * 12);
            int flareAlpha = (int)(flare * 200);
            gfx.fill(scx - flareSize, scy - flareSize, scx + flareSize, scy + flareSize,
                    (flareAlpha << 24) | 0xCCAAFF);
        }

        // 回転する円形エフェクト
        drawRotatingRing(gfx, scx, scy, prog);
    }

    /** 回転する光の輪 */
    private void drawRotatingRing(GuiGraphics gfx, int cx, int cy, float prog) {
        int numDots = 12;
        long time = System.currentTimeMillis();
        float rot = (time % 2000) / 2000f * (float)(Math.PI * 2);
        float ringR = R * (1f - prog * 0.6f); // 収縮

        for (int i = 0; i < numDots; i++) {
            float angle = rot + (float)(i * Math.PI * 2 / numDots);
            int dx = (int)(cx + Math.cos(angle) * ringR);
            int dy = (int)(cy + Math.sin(angle) * ringR);
            int alpha = (int)(180 * (float)(i + 1) / numDots);
            gfx.fill(dx - 1, dy - 1, dx + 1, dy + 1, (alpha << 24) | 0x9966FF);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // タイトル
        gfx.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        // インベントリラベル
        gfx.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        // 触媒ラベル
        gfx.drawString(font,
                net.minecraft.network.chat.Component.translatable("gui.more_iss.catalyst"),
                132, 0, 0x6644AA, false);

        // クラフト中プログレスバー
        if (menu.isCraftingActive()) {
            int barW = (int)(52 * (1f - menu.getCraftingTick() /
                    (float) nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity.CRAFT_DURATION));
            gfx.fill(68, 158, 68 + barW, 163, 0xFF9966FF);
            gfx.fill(68, 158, 120, 163, 0x44FFFFFF); // 背景
        }
    }
}
