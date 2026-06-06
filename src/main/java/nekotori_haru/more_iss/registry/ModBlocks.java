package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.block.ArcaneCraftingTableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, More_iss.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, More_iss.MODID);

    public static final RegistryObject<Block> ARCANE_CRAFTING_TABLE = BLOCKS.register("fusion_table", () ->
            new ArcaneCraftingTableBlock(Block.Properties.of().mapColor(MapColor.STONE).strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> ARCANE_CRAFTING_TABLE_ITEM = ITEMS.register("fusion_table", () ->
            new BlockItem(ARCANE_CRAFTING_TABLE.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}