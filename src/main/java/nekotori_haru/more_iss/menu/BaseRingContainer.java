package nekotori_haru.more_iss.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public abstract class BaseRingContainer extends AbstractContainerMenu {

    protected final ItemStackHandler inventory;
    protected final ItemStack ringStack;
    protected final int ringSlotCount;
    protected final int slotStartX;
    protected final int slotStartY;
    protected final int slotsPerRow;

    protected BaseRingContainer(MenuType<?> menuType, int id, Inventory playerInv,
                                ItemStackHandler handler, ItemStack ringStack,
                                int ringSlotCount, int slotStartX, int slotStartY, int slotsPerRow) {
        super(menuType, id);
        this.inventory = handler;
        this.ringStack = ringStack;
        this.ringSlotCount = ringSlotCount;
        this.slotStartX = slotStartX;
        this.slotStartY = slotStartY;
        this.slotsPerRow = slotsPerRow;

        // リングのインベントリスロットを追加
        for (int i = 0; i < ringSlotCount; i++) {
            int x = slotStartX + (i % slotsPerRow) * 18;
            int y = slotStartY + (i / slotsPerRow) * 18;
            this.addSlot(new SlotItemHandler(handler, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isAcceptedItem(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return getMaxStackSizePerSlot();
                }
            });
        }

        // ★ プレイヤーインベントリのオフセットを取得
        int xOff = getPlayerInventoryXOffset();
        int yOff = getPlayerInventoryYOffset();

        // プレイヤーインベントリ（27スロット）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, 9 + row * 9 + col,
                        8 + col * 18 + xOff,
                        84 + row * 18 + yOff));
            }
        }

        // プレイヤーホットバー（9スロット）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col,
                    8 + col * 18 + xOff,
                    142 + yOff));
        }
    }

    // ★ サブクラスでオーバーライドしてプレイヤーインベントリの位置を微調整
    protected int getPlayerInventoryXOffset() {
        return 0;
    }

    protected int getPlayerInventoryYOffset() {
        return 0;
    }

    // サブクラスでオーバーライドしてアイテム制限を設定
    protected boolean isAcceptedItem(ItemStack stack) {
        return true;
    }

    // サブクラスでオーバーライドして最大スタック数を設定
    protected int getMaxStackSizePerSlot() {
        return 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < ringSlotCount) {
                if (!this.moveItemStackTo(stack, ringSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isAcceptedItem(stack)) {
                    if (!this.moveItemStackTo(stack, 0, ringSlotCount, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveInventory();
    }

    protected abstract void saveInventory();
}