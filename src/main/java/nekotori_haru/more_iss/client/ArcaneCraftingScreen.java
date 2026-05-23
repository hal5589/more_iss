package nekotori_haru.more_iss.client;

import com.mojang.blaze3d.systems.RenderSystem;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArcaneCraftingScreen extends AbstractContainerScreen<ArcaneCraftingMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("more_iss", "textures/gui/arcane_crafting_table.png");

    private static final int GUI_WIDTH  = 175;
    private static final int GUI_HEIGHT = 243;

    private static final int CENTER_SLOT_X = ArcaneCraftingMenu.CENTER_X;
    private static final int CENTER_SLOT_Y = ArcaneCraftingMenu.CENTER_Y;

    private final List<ImpactParticle> impactParticles = new ArrayList<>();
    private final boolean[] slotTriggered = new boolean[8];
    private boolean hasSentCompletionPacket = false;

    private final Random random = new Random();
    private int animTick = 0;

    private float ringScale = 1.0f;
    private float ringVelocity = 0.0f;
    private final float ringTarget = 1.0f;

    private int hideItemsTimer = 0;
    private boolean wasCraftingLastFrame = false;

    public ArcaneCraftingScreen(ArcaneCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelX = 8; this.titleLabelY = 5;
        this.inventoryLabelX = 8; this.inventoryLabelY = 152;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int x = (this.width  - GUI_WIDTH)  / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        boolean isCrafting = menu.isCraftingActive();

        if (isCrafting && !wasCraftingLastFrame) {
            this.animTick = 0;
            this.hasSentCompletionPacket = false;
            this.ringScale = 1.0f;
            this.ringVelocity = 0.0f;
            for (int i = 0; i < 8; i++) this.slotTriggered[i] = false;
        }

        if (isCrafting) {
            this.animTick++;
            float springConstant = 0.35f;
            float damping = 0.75f;
            float force = (ringTarget - ringScale) * springConstant;
            ringVelocity += force;
            ringVelocity *= damping;
            ringScale += ringVelocity;
        }

        if (wasCraftingLastFrame && !isCrafting) hideItemsTimer = 20;
        wasCraftingLastFrame = isCrafting;
        if (hideItemsTimer > 0) hideItemsTimer--;

        ItemStack[] savedStacks = new ItemStack[8];
        if (isCrafting || hideItemsTimer > 0) {
            for (int i = 0; i < 8; i++) {
                Slot slot = menu.slots.get(i);
                savedStacks[i] = slot.getItem();
                slot.setByPlayer(ItemStack.EMPTY);
            }
        }

        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        if (isCrafting || hideItemsTimer > 20) {
            for (int i = 0; i < 8; i++) menu.slots.get(i).setByPlayer(savedStacks[i]);
        }

        if (isCrafting) {
            int totalSlotsAtCenter = 0;
            int interval = 10;
            int baseSyncTick = 7 * interval;
            float bulgeDuration = 6f;
            float moveDuration = 10f;

            if (this.animTick >= baseSyncTick + 10) {
                drawPulseRing(gfx, x + CENTER_SLOT_X + 8, y + CENTER_SLOT_Y + 8, this.ringScale);
            }

            // ⭕ 【新実装】開始から10 Ticksで 1.0倍→1.35倍へなめらかに拡大
            float growProg = Math.min(1.0f, (float)this.animTick / 10f);
            float currentScale = 1.0f + (0.35f * growProg);

            for (int i = 0; i < 8; i++) {
                ItemStack itemToRender = savedStacks[i];
                if (itemToRender == null || itemToRender.isEmpty()) {
                    totalSlotsAtCenter++;
                    continue;
                }

                int origX = ArcaneCraftingMenu.CIRCLE_POS[i][0];
                int origY = ArcaneCraftingMenu.CIRCLE_POS[i][1];
                double angle = (-Math.PI / 2) + (i * Math.PI / 4);

                double bulge = 0;
                int myActionStartTick = baseSyncTick + (10 + (i * 5));

                if (this.animTick >= myActionStartTick) {
                    float bulgeProg = Math.min(bulgeDuration, (this.animTick - myActionStartTick)) / bulgeDuration;
                    float smoothBulge = (float) Math.sin(bulgeProg * Math.PI / 2f);
                    bulge = 6.0 * smoothBulge;
                }

                double floatX = origX + Math.cos(angle) * bulge;
                double floatY = origY + Math.sin(angle) * bulge;

                int moveStartTick = myActionStartTick + (int)bulgeDuration;
                int moveEndTick = moveStartTick + (int)moveDuration;
                float easedProgInward = 0f;

                if (this.animTick < moveStartTick) {
                    // 中心へ行く前は拡大された状態で待機
                } else if (this.animTick >= moveStartTick && this.animTick < moveEndTick) {
                    float moveProg = (float)(this.animTick - moveStartTick) / moveDuration;
                    easedProgInward = (float) Math.pow(moveProg, 2.5);
                } else {
                    easedProgInward = 1.0f;
                }

                double curX = (easedProgInward < 1.0f) ? (floatX + (CENTER_SLOT_X - floatX) * easedProgInward) : CENTER_SLOT_X;
                double curY = (easedProgInward < 1.0f) ? (floatY + (CENTER_SLOT_Y - floatY) * easedProgInward) : CENTER_SLOT_Y;

                if (easedProgInward < 1.0f) {
                    int renderX = x + (int)curX;
                    int renderY = y + (int)curY;

                    gfx.pose().pushPose();
                    gfx.pose().translate(0, 0, 100);
                    gfx.pose().translate(renderX + 8, renderY + 8, 0);
                    gfx.pose().scale(currentScale, currentScale, 1.0f); // ⭕ 滑らかな可変スケール
                    gfx.pose().translate(-(renderX + 8), -(renderY + 8), 0);

                    gfx.renderItem(itemToRender, renderX, renderY);
                    gfx.renderItemDecorations(this.font, itemToRender, renderX, renderY);
                    gfx.pose().popPose();
                }

                if (easedProgInward >= 1.0f) {
                    totalSlotsAtCenter++;
                    if (!this.slotTriggered[i]) {
                        this.slotTriggered[i] = true;
                        this.ringScale = 1.4f;
                        this.ringVelocity = 0.1f;
                        for (int p = 0; p < 8 + random.nextInt(4); p++) {
                            double pAngle = random.nextDouble() * Math.PI * 2;
                            double speed = (15 + random.nextInt(25)) / 3.5 + (random.nextDouble() * 1.5);
                            impactParticles.add(new ImpactParticle(CENTER_SLOT_X + 8, CENTER_SLOT_Y + 8, Math.cos(pAngle) * speed, Math.sin(pAngle) * speed, 8 + random.nextInt(5), 0.91, random.nextBoolean()));
                        }
                    }
                }
            }
            if (totalSlotsAtCenter == 8 && !hasSentCompletionPacket) {
                hasSentCompletionPacket = true;
                this.sendCraftCompletionToServer();
            }
        }
        if (!impactParticles.isEmpty()) renderImpactParticles(gfx, x, y);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    private void drawPulseRing(GuiGraphics gfx, int cx, int cy, float scale) {
        int numSegments = 24;
        float baseRadius = 13.0f * scale;
        for (int i = 0; i < numSegments; i++) {
            double angle = (i * 2 * Math.PI) / numSegments;
            int rx = cx + (int)(Math.cos(angle) * baseRadius);
            int ry = cy + (int)(Math.sin(angle) * baseRadius);
            gfx.fill(rx - 1, ry - 1, rx + 1, ry + 1, (0xB2 << 24) | 0xBD93F9);
        }
    }

    private void renderImpactParticles(GuiGraphics gfx, int baseX, int baseY) {
        impactParticles.removeIf(p -> p.age <= 0);
        for (ImpactParticle p : impactParticles) {
            p.x += p.vx; p.y += p.vy; p.vx *= p.friction; p.vy *= p.friction; p.age--;
            int alpha = (int)(190 * Math.pow((float)p.age / p.maxAge, 1.5));
            if (alpha < 0) alpha = 0;
            if (p.isBall) {
                int col = (alpha << 24) | 0x9B59EF;
                gfx.fill(baseX + (int)p.x - 2, baseY + (int)p.y - 1, baseX + (int)p.x + 2, baseY + (int)p.y + 2, col);
                gfx.fill(baseX + (int)p.x - 1, baseY + (int)p.y - 2, baseX + (int)p.x + 2, baseY + (int)p.y - 1, col);
                gfx.fill(baseX + (int)p.x - 1, baseY + (int)p.y + 2, baseX + (int)p.x + 2, baseY + (int)p.y + 3, col);
            } else {
                gfx.fill(baseX + (int)p.x - 1, baseY + (int)p.y - 1, baseX + (int)p.x + 1, baseY + (int)p.y + 1, (alpha << 24) | 0xFFF5FF);
            }
        }
    }

    private void sendCraftCompletionToServer() {}

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        gfx.blit(TEXTURE, (this.width-GUI_WIDTH)/2, (this.height-GUI_HEIGHT)/2, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        if (menu.isCraftingActive()) drawRotatingRing(gfx, (this.width-GUI_WIDTH)/2 + 89, (this.height-GUI_HEIGHT)/2 + 80, Math.min(1.0f, this.animTick/140f));
    }

    private void drawRotatingRing(GuiGraphics gfx, int cx, int cy, float prog) {
        for (int i = 0; i < 16; i++) {
            float a = (System.currentTimeMillis() % 1500) / 1500f * 6.28f + i * 0.39f;
            float r = 57 * (1.1f - (float)Math.pow(prog, 3.0) * 0.9f);
            gfx.fill(cx+(int)(Math.cos(a)*r)-1, cy+(int)(Math.sin(a)*r)-1, cx+(int)(Math.cos(a)*r)+1, cy+(int)(Math.sin(a)*r)+1, ((int)(140*(i+1f)/16) << 24) | 0xBB88FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderLabels(gfx, mouseX, mouseY);
        gfx.drawString(this.font, Component.translatable("gui.more_iss.catalyst"), 116, 132, 0x404040, false);
    }

    private static class ImpactParticle {
        double x, y, vx, vy, friction; int age, maxAge; boolean isBall;
        ImpactParticle(double x, double y, double vx, double vy, int age, double friction, boolean isBall) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.age = age; this.maxAge = age; this.friction = friction; this.isBall = isBall;
        }
    }
}