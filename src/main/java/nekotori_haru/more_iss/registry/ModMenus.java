package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.item.RingOfManaConversionItem;
import nekotori_haru.more_iss.item.RingOfManaFurnaceItem;
import nekotori_haru.more_iss.item.RingOfThunderResonanceItem;
import nekotori_haru.more_iss.menu.ManaFurnaceContainer;
import nekotori_haru.more_iss.menu.RingOfManaConversionContainer;
import nekotori_haru.more_iss.menu.RingOfThunderResonanceContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, More_iss.MODID);

    public static final RegistryObject<MenuType<RingOfManaConversionContainer>> RING_OF_MANA_CONVERSION =
            MENU_TYPES.register("ring_of_mana_conversion",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        ItemStack ringStack = data.readItem();
                        ItemStackHandler handler = RingOfManaConversionItem.getInventory(ringStack);
                        return new RingOfManaConversionContainer(windowId, inv, handler, ringStack);
                    }));

    public static final RegistryObject<MenuType<ManaFurnaceContainer>> MANA_FURNACE =
            MENU_TYPES.register("mana_furnace",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        ItemStack ringStack = data.readItem();
                        ItemStackHandler handler = RingOfManaFurnaceItem.getInventory(ringStack);
                        return new ManaFurnaceContainer(windowId, inv, handler, ringStack);
                    }));

    public static final RegistryObject<MenuType<RingOfThunderResonanceContainer>> RING_OF_THUNDER_RESONANCE = MENU_TYPES.register("ring_of_thunder_resonance",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                ItemStack stack = data.readItem();
                ItemStackHandler handler = RingOfThunderResonanceItem.getInventory(stack);
                return new RingOfThunderResonanceContainer(windowId, inv, handler, stack);
            }));
}