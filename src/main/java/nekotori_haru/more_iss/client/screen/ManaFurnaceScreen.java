package nekotori_haru.more_iss.client.screen;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.item.RingOfManaFurnaceItem;
import nekotori_haru.more_iss.menu.ManaFurnaceContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManaFurnaceScreen extends BaseRingScreen<ManaFurnaceContainer> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(More_iss.MODID, "textures/gui/container/ring_base.png");

    private static final int GAUGE_X = 12;
    private static final int GAUGE_Y = 148;
    private static final int GAUGE_W = 152;
    private static final int GAUGE_H = 8;
    private static final int STATUS_Y = 140;

    private final ManaFurnaceContainer container;

    public ManaFurnaceScreen(ManaFurnaceContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.container = container;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        renderChargeGauge(guiGraphics, x, y);
    }

    private void renderChargeGauge(GuiGraphics guiGraphics, int guiX, int guiY) {
        int charge = RingOfManaFurnaceItem.getCharge(container.getRingStack());
        int maxCharge = RingOfManaFurnaceItem.MAX_CHARGE;
        int fillW = (int) ((float) GAUGE_W * charge / maxCharge);

        // 背景（濃いグレー）
        guiGraphics.fill(guiX + GAUGE_X, guiY + GAUGE_Y,
                guiX + GAUGE_X + GAUGE_W, guiY + GAUGE_Y + GAUGE_H,
                0xFF444444);

        // 前景（緑）
        if (fillW > 0) {
            guiGraphics.fill(guiX + GAUGE_X, guiY + GAUGE_Y,
                    guiX + GAUGE_X + fillW, guiY + GAUGE_Y + GAUGE_H,
                    0xFF00FF00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int charge = RingOfManaFurnaceItem.getCharge(container.getRingStack());
        int maxCharge = RingOfManaFurnaceItem.MAX_CHARGE;
        boolean active = RingOfManaFurnaceItem.isActive(container.getRingStack());

        // 状態テキスト
        Component statusText = Component.translatable(
                active
                        ? "item.more_iss.ring_of_mana_furnace.status_on"
                        : "item.more_iss.ring_of_mana_furnace.status_off"
        );
        int statusColor = active ? 0x55FF55 : 0x888888;
        guiGraphics.drawString(this.font, statusText, GAUGE_X, STATUS_Y, statusColor, false);

        // チャージ数値
        String chargeStr = charge + " / " + maxCharge;
        int textW = this.font.width(chargeStr);
        guiGraphics.drawString(this.font, chargeStr,
                GAUGE_X + GAUGE_W - textW, STATUS_Y, 0xFFFFFF, false);

        // MAX点滅
        if (charge >= maxCharge) {
            long ticks = System.currentTimeMillis() / 500;
            if (ticks % 2 == 0) {
                Component readyText = Component.translatable("item.more_iss.ring_of_mana_furnace.ready");
                int rW = this.font.width(readyText);
                guiGraphics.drawString(this.font, readyText,
                        (this.imageWidth - rW) / 2, GAUGE_Y + GAUGE_H + 3,
                        0xFFD700, false);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}