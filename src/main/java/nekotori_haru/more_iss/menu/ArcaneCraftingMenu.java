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

    // 正確な円形スロット座標（人力修正版）
    public static final int[][] CIRCLE_POS = {
            { 80, 14 },   // 0: 上
            { 120, 31 },  // 1: 右上
            { 136, 71 },  // 2: 右
            { 120, 111 }, // 3: 右下
            { 80, 127 },  // 4: 下
            { 40, 111 },  // 5: 左下
            { 23, 71 },   // 6: 左
            { 40, 31 }    // 7: 左上
    };

    public static final int CENTER_X = 80;  // 中央スロット
    public static final int CENTER_Y = 71;
    public static final int CATALYST_X = 152; // 触媒スロット
    public static final int CATALYST_Y = 127;

    public ArcaneCraftingMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, getBlockEntity(playerInv, buf));
    }

    public ArcaneCraftingMenu(int windowId, Inventory playerInv, Container container) {
        super(More_iss.ARCANE_CRAFTING_MENU.get(), windowId);
        this.container = container;

        if (container instanceof ArcaneCraftingTableBlockEntity be) {
            this.data = be.dataAccess;
        } else {
            this.data = new SimpleContainerData(2);
        }

        // 0-7: 周囲の円形スロット（最大スタック数を1に制限）
        for (int i = 0; i < 8; i++) {
            this.addSlot(new Slot(container, i, CIRCLE_POS[i][0], CIRCLE_POS[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return !ArcaneCraftingMenu.this.isCraftingActive(); }
                @Override public boolean mayPickup(Player playerIn) { return !ArcaneCraftingMenu.this.isCraftingActive(); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        // 8: 中央スロット（最大スタック数を1に制限）
        this.addSlot(new Slot(container, 8, CENTER_X, CENTER_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return !ArcaneCraftingMenu.this.isCraftingActive(); }
            @Override public boolean mayPickup(Player playerIn) { return !ArcaneCraftingMenu.this.isCraftingActive(); }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 9: 触媒スロット（こちらは通常通りスタック可能）
        this.addSlot(new CatalystSlot(container, 9, CATALYST_X, CATALYST_Y));

        // プレイヤーインベントリ（X=8, Y=162）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 162 + row * 18));
            }
        }

        // ホットバー（X=8, Y=220）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 220));
        }

        this.addDataSlots(this.data);
    }

    private static Container getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        BlockEntity be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArcaneCraftingTableBlockEntity acbe) return acbe;
        return new SimpleContainer(10);
    }

    public int getCraftingTick() { return data.get(0); }
    public boolean isCraftingActive() { return data.get(1) == 1; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 8) {
                if (!this.moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, result);
            } else if (index < 10) {
                if (!this.moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 8, 9, false)) {
                    if (!this.moveItemStackTo(stack, 0, 8, false)) {
                        if (!this.moveItemStackTo(stack, 9, 10, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) { return this.container.stillValid(player); }

    static class CatalystSlot extends Slot {
        CatalystSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
    }
}