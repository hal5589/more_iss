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

/**
 * ArcaneCraftingMenu
 *
 * スロット配置 (GUIサイズ 196x210):
 * 0-7  : 円形スロット（中心88,75 / 半径48px）
 * 8    : 中央スロット（出力のみ）
 * 9    : 触媒スロット（右上 x=144, y=8 / 最大1024スタック）
 * 10-36: プレイヤーインベントリ
 * 37-45: ホットバー
 */
public class ArcaneCraftingMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    // GUI中心座標・半径
    private static final int CX = 88;
    private static final int CY = 75;
    private static final int R  = 48;

    // 円形スロット8個の座標（スロット左上角、アイコン16px前提）
    private static final int[][] CIRCLE_POS = {
            {CX - 8,      CY - R - 8},  // 0: 上
            {CX + 26,     CY - 42},     // 1: 右上
            {CX + R - 8,  CY - 8},      // 2: 右
            {CX + 26,     CY + 26},     // 3: 右下
            {CX - 8,      CY + R - 8},  // 4: 下
            {CX - 42,     CY + 26},     // 5: 左下
            {CX - R - 8,  CY - 8},      // 6: 左
            {CX - 42,     CY - 42},     // 7: 左上
    };

    // ネットワーク越しのコンストラクタ
    public ArcaneCraftingMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf));
    }

    // 直接生成用コンストラクタ（サーバー側 BlockEntity から呼ばれる）
    public ArcaneCraftingMenu(int id, Inventory playerInv, Container container) {
        super(More_iss.ARCANE_CRAFTING_MENU.get(), id);
        this.container = container;

        // 渡されたcontainerがBlockEntityであれば、そのdataAccessを共有する
        if (container instanceof ArcaneCraftingTableBlockEntity table) {
            this.data = table.dataAccess;
        } else {
            this.data = new SimpleContainerData(2);
        }

        // 円形スロット 0-7
        for (int i = 0; i < 8; i++) {
            addSlot(new Slot(container, i, CIRCLE_POS[i][0], CIRCLE_POS[i][1]));
        }

        // 中央スロット 8（出力のみ・入力不可）
        addSlot(new Slot(container, 8, CX - 8, CY - 8) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // 触媒スロット 9（右上UI・最大1024スタック）
        addSlot(new CatalystSlot(container, 9, 144, 8));

        // プレイヤーインベントリ（3行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        118 + row * 18));
            }
        }
        // ホットバー
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 176));
        }

        addDataSlots(data);
    }

    private static Container getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArcaneCraftingTableBlockEntity table) return table;
        return new SimpleContainer(10);
    }

    // -------------------------------------------------------------------------
    // アニメーション情報（クライアント側GUIで参照）
    // -------------------------------------------------------------------------

    public int getCraftingTick() { return data.get(0); }
    public boolean isCraftingActive() { return data.get(1) == 1; }

    // -------------------------------------------------------------------------
    // Shift+クリック
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == 8) {
            // 出力スロット → プレイヤーインベントリ
            if (!moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, result);
        } else if (index >= 10) {
            // プレイヤー → 円形スロット
            if (!moveItemStackTo(stack, 0, 8, false)) return ItemStack.EMPTY;
        } else {
            // 円形スロット → プレイヤー
            if (!moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    // -------------------------------------------------------------------------
    // 触媒スロット（最大スタック数を1024に拡張）
    // -------------------------------------------------------------------------

    private static class CatalystSlot extends Slot {
        public CatalystSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return ArcaneCraftingTableBlockEntity.CATALYST_MAX_STACK;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return ArcaneCraftingTableBlockEntity.CATALYST_MAX_STACK;
        }
    }
}