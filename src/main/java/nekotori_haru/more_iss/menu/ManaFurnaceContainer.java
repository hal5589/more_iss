package nekotori_haru.more_iss.menu;

import nekotori_haru.more_iss.item.RingOfManaFurnaceItem;
import nekotori_haru.more_iss.registry.ModItems;
import nekotori_haru.more_iss.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class ManaFurnaceContainer extends BaseRingContainer {

    private static final int SLOT_COUNT    = 1;
    private static final int SLOTS_PER_ROW = 1;
    private static final int SLOT_START_X  = 80;
    private static final int SLOT_START_Y  = 35;

    public ManaFurnaceContainer(int windowId, Inventory playerInv,
                                ItemStackHandler handler, ItemStack ringStack) {
        super(ModMenus.MANA_FURNACE.get(), windowId, playerInv, handler, ringStack,
                SLOT_COUNT, SLOT_START_X, SLOT_START_Y, SLOTS_PER_ROW);
    }

    @Override
    protected boolean isAcceptedItem(ItemStack stack) {
        return stack.getItem() == ModItems.MANA_CIRCUIT.get();
    }

    @Override
    protected int getMaxStackSizePerSlot() {
        return 64;
    }

    // ★ プレイヤーインベントリを -1px ずらす（右下にずれている場合）
    @Override
    protected int getPlayerInventoryXOffset() {
        return -1; // 左に1px
    }

    @Override
    protected int getPlayerInventoryYOffset() {
        return -1; // 上に1px
    }

    public ItemStack getRingStack() {
        return this.ringStack;
    }

    @Override
    protected void saveInventory() {
        RingOfManaFurnaceItem.saveInventory(this.ringStack, this.inventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}