package nekotori_haru.more_iss.menu;

import nekotori_haru.more_iss.item.RingOfManaConversionItem;
import nekotori_haru.more_iss.registry.ModItems;
import nekotori_haru.more_iss.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class RingOfManaConversionContainer extends BaseRingContainer {

    public RingOfManaConversionContainer(int id, Inventory playerInv, ItemStackHandler handler, ItemStack ringStack) {
        super(ModMenus.RING_OF_MANA_CONVERSION.get(), id, playerInv, handler, ringStack,
                1, 80, 35, 1);
    }

    @Override
    protected boolean isAcceptedItem(ItemStack stack) {
        return stack.getItem() == ModItems.MANA_CIRCUIT.get();
    }

    @Override
    protected int getMaxStackSizePerSlot() {
        return RingOfManaConversionItem.MAX_STACK_PER_SLOT;
    }

    // ★ プレイヤーインベントリを -1px ずらす
    @Override
    protected int getPlayerInventoryXOffset() {
        return -1;
    }

    @Override
    protected int getPlayerInventoryYOffset() {
        return -1;
    }

    @Override
    protected void saveInventory() {
        RingOfManaConversionItem.saveInventory(ringStack, inventory);
    }
}