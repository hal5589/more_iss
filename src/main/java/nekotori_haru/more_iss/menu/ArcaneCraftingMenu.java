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

public class ArcaneCraftingMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    // ─── GUI 寸法 ─────────────────────────────────────────────────────────
    // テクスチャ (177x246) 上のスロット配置
    // 円の中心: (88, 75), 半径: 48px
    private static final int CX = 88;
    private static final int CY = 75;

    /**
     * 周囲8スロット [x, y] の左上座標（スロット幅は16px → 中心 = 座標+8）
     * スロット0: 上, スロット1: 右上, … 時計回り
     */
    private static final int[][] CIRCLE_POS = {
            { 80,  19 },   // 0: 上
            { 113, 33 },   // 1: 右上
            { 128, 67 },   // 2: 右
            { 113, 100},   // 3: 右下
            {  80, 115},   // 4: 下
            {  46, 100},   // 5: 左下
            {  32,  67},   // 6: 左
            {  46,  33},   // 7: 左上
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
        this.addSlot(new Slot(container, 8, CX - 8, CY - 8));  // 左上(80, 67)

        // ── 触媒スロット（スロット9）─────────────────────────────────────
        this.addSlot(new CatalystSlot(container, 9, 152, 0));

        // ── プレイヤーインベントリ（3行）────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        161 + row * 18));
            }
        }

        // ── ホットバー ───────────────────────────────────────────────────
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv,
                    col,
                    8 + col * 18,
                    219));
        }

        this.addDataSlots(this.data);
    }

    // ─── BlockEntity 取得ヘルパー ─────────────────────────────────────────
    private static Container getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        var be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArcaneCraftingTableBlockEntity acbe) return acbe;
        return new SimpleContainer(10);
    }

    // ─── クライアントへの同期値 ───────────────────────────────────────────
    public int  getCraftingTick()   { return data.get(0); }
    public boolean isCraftingActive() { return data.get(1) == 1; }

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
        // 触媒スロットには何でも入れられるが、クラフト中は取り出し不可
    }
}