package nekotori_haru.more_iss.menu;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ArcaneCraftingMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    // ─── GUI 寸法 ────────────────────────────────────────────────────────
    // テクスチャ (175x243) 上のスロット配置
    // 円の中心: (87, 54), 半径: 40px
    private static final int CX = 87;
    private static final int CY = 54;
    private static final int R = 40;

    /**
     * 周囲8スロット [x, y] の左上座標（スロット幅は16px → 中心 = 座標+8）
     * スロット0: 上, スロット1: 右上, … 時計回り
     */
    private static final int[][] CIRCLE_POS = {
            { CX - 8,      CY - R - 8 }, // 0: 上
            { CX + 26,     CY - 32 },    // 1: 右上
            { CX + R - 8,  CY - 8 },     // 2: 右
            { CX + 26,     CY + 16 },    // 3: 右下
            { CX - 8,      CY + R - 8 }, // 4: 下
            { CX - 42,     CY + 16 },    // 5: 左下
            { CX - R - 8,  CY - 8 },     // 6: 左
            { CX - 42,     CY - 32 },    // 7: 左上
    };

    // ─── ファクトリ（パケットから生成） ───────────────────────────────────
    public ArcaneCraftingMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, getBlockEntity(playerInv, buf));
    }

    // ─── メインコンストラクタ ─────────────────────────────────────────────
    public ArcaneCraftingMenu(int windowId, Inventory playerInv, Container container) {
        super(More_iss.ARCANE_CRAFTING_MENU.get(), windowId);
        this.container = container;

        // ContainerData（サーバー←→クライアント同期）
        if (container instanceof ArcaneCraftingTableBlockEntity be) {
            this.data = be.dataAccess;
        } else {
            this.data = new SimpleContainerData(2);
        }

        // ── クラフトスロット（周囲8個）───────────────────────────────────
        for (int i = 0; i < 8; i++) {
            this.addSlot(new Slot(container, i, CIRCLE_POS[i][0], CIRCLE_POS[i][1]));
        }

        // ── 中央スロット（入力兼出力: スロット8）─────────────────────────
        //    アイテムを直接置いて、完成品がここに入る
        this.addSlot(new Slot(container, 8, CX - 8, CY - 8) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // ── 触媒スロット（スロット9）右下の魔法陣外───────────────────────
        this.addSlot(new CatalystSlot(container, 9, 152, 48));

        // ── プレイヤーインベントリ（3行）────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        135 + row * 18));
            }
        }

        // ── ホットバー ───────────────────────────────────────────────────
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv,
                    col,
                    8 + col * 18,
                    193));
        }

        this.addDataSlots(this.data);
    }

    // ─── BlockEntity 取得ヘルパー ─────────────────────────────────────────
    private static Container getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        BlockEntity be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArcaneCraftingTableBlockEntity acbe) return acbe;
        return new SimpleContainer(10);
    }

    // ─── クライアントへの同期値 ───────────────────────────────────────────
    public int getCraftingTick() {
        return data.get(0);
    }

    public boolean isCraftingActive() {
        return data.get(1) == 1;
    }

    // ─── Shift クリック ───────────────────────────────────────────────────
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == 8) {
                // 中央スロット → インベントリへ
                if (!this.moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, result);
            } else if (index < 10) {
                // クラフト/触媒スロット → インベントリへ
                if (!this.moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
            } else {
                // インベントリ → クラフトスロット（0-8）
                if (!this.moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    // ─── 触媒専用スロット ─────────────────────────────────────────────────
    static class CatalystSlot extends Slot {
        CatalystSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }
}