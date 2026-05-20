package nekotori_haru.more_iss;

import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.spell.ender.EnderShootingStar;
import nekotori_haru.more_iss.spell.ender.FreischutzSpell;
import nekotori_haru.more_iss.spell.blood.OverburstBloodSpell;
import nekotori_haru.more_iss.spell.lightning.ThunderboltFlash;
import nekotori_haru.more_iss.spell.holy.ProvidentialConduitSpell;
import nekotori_haru.more_iss.spell.eldritch.SoulLinkSpell;
import nekotori_haru.more_iss.spell.blood.SacrificialEdgeSpell;
import nekotori_haru.more_iss.spell.ice.AbsoluteZeroSpell;
import nekotori_haru.more_iss.spell.eldritch.DisintegrationSpell;
import nekotori_haru.more_iss.util.DisintegrationState;
import nekotori_haru.more_iss.util.DisintegrationTargetManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(More_iss.MODID)
public class More_iss {

    public static final String MODID = "more_iss";
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- レジストリ定義 ---
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    // --- オブジェクト登録 ---
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));

    // 魔法の登録
    public static final RegistryObject<AbstractSpell> ENDER_SHOOTING_STAR = SPELLS.register("ender_shooting_star", EnderShootingStar::new);
    public static final RegistryObject<AbstractSpell> FREISCHUTZ = SPELLS.register("freischutz", FreischutzSpell::new);
    public static final RegistryObject<AbstractSpell> RAISEN = SPELLS.register("raisen", ThunderboltFlash::new);
    public static final RegistryObject<AbstractSpell> OVERBURST_BLOOD = SPELLS.register("overburst_blood", OverburstBloodSpell::new);
    public static final RegistryObject<AbstractSpell> PROVIDENTIAL_CONDUIT = SPELLS.register("providential_conduit", ProvidentialConduitSpell::new);
    public static final RegistryObject<AbstractSpell> SACRIFICIAL_EDGE = SPELLS.register("sacrificial_edge", SacrificialEdgeSpell::new);
    public static final RegistryObject<AbstractSpell> SOUL_LINK = SPELLS.register("soul_link", SoulLinkSpell::new);
    public static final RegistryObject<AbstractSpell> ABSOLUTE_ZERO = SPELLS.register("absolute_zero", AbsoluteZeroSpell::new);

    // 崩壊魔法本体のレジストリ登録
    public static final RegistryObject<AbstractSpell> DISINTEGRATION = SPELLS.register("disintegration", DisintegrationSpell::new);

    // Forgeへのイベントバス登録コンストラクタ
    public More_iss() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. エフェクト（バフ・デバフ）を最優先で登録
        ModEffects.register(modEventBus);

        // 2. その他のシステム（ブロック、アイテム、魔法など）を登録
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SPELLS.register(modEventBus);
        ENTITIES.register(modEventBus);

        // 🌟【維持】崩壊魔法システム裏側のイベントハンドラーを初期化・常駐化
        DisintegrationState.init();
        DisintegrationTargetManager.init();

    }
}