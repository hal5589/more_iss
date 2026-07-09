package nekotori_haru.more_iss.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemStackHandlerProvider implements ICapabilityProvider {

    private final LazyOptional<ItemStackHandler> inventory;

    public ItemStackHandlerProvider(ItemStack stack) {
        this.inventory = LazyOptional.of(() -> RingOfManaConversionItem.getInventory(stack));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventory.cast();
        }
        return LazyOptional.empty();
    }
}