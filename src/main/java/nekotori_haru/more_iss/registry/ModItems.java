package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.item.UpgradeOrbItem;
import io.redspace.ironsspellbooks.util.ItemPropertiesHelper;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.item.RingOfManaCycleItem;
import nekotori_haru.more_iss.item.SpellbookOfConcentration;
import nekotori_haru.more_iss.item.ringofsynthesis.RingOfSynthesisItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, More_iss.MODID);

    public static final RegistryObject<Item> RING_OF_SYNTHESIS = ITEMS.register("ring_of_synthesis",
            () -> new RingOfSynthesisItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> SYNTHESIS_UPGRADE_ORB = ITEMS.register("synthesis_upgrade_orb",
            () -> new UpgradeOrbItem(
                    ItemPropertiesHelper.material().rarity(Rarity.EPIC).fireResistant(),
                    UpgradeOrbTypeRegistry.SYNTHESIS_SPELL_POWER  // 後述
            ));

    public static final RegistryObject<Item> PHOENIX_FALLEN_FEATHER = ITEMS.register("phoenix_fallen_feather",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BLAZING_NOVA = ITEMS.register("blazing_nova",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> RECORD_OF_THE_GODS = ITEMS.register("record_of_the_gods",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> PRIMAL_CORE = ITEMS.register("primal_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ABSOLUTE_ZERO_SHARD = ITEMS.register("absolute_zero_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FORBIDDEN_GRIMOIRE = ITEMS.register("forbidden_grimoire",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> RING_OF_MANA_CYCLE = ITEMS.register("ring_of_mana_cycle",
            () -> new RingOfManaCycleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> MANA_CIRCUIT = ITEMS.register("mana_circuit",
            () -> new RingOfManaCycleItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> SPELLBOOK_OF_CONCENTRATION = ITEMS.register("spellbook_of_concentration",
            SpellbookOfConcentration::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

}