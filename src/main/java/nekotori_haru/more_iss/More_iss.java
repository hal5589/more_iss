package nekotori_haru.more_iss;

import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.spell.holy.HeavenlyBlastSpell;
import nekotori_haru.more_iss.spell.synthesis.FunnelSpell;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent; // ⭕ 追加
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nekotori_haru.more_iss.block.ArcaneCraftingTableBlock;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import nekotori_haru.more_iss.client.ArcaneCraftingScreen;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import nekotori_haru.more_iss.network.ModNetwork; // ⭕ 追加
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeSerializer;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeType;
import nekotori_haru.more_iss.registry.ModAttributes;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import nekotori_haru.more_iss.spell.synthesis.OverburstBloodSpell;
import nekotori_haru.more_iss.spell.synthesis.SacrificialEdgeSpell;
import nekotori_haru.more_iss.spell.synthesis.DisintegrationSpell;
import nekotori_haru.more_iss.spell.eldritch.SoulLinkSpell;
import nekotori_haru.more_iss.spell.ender.EnderShootingStar;
import nekotori_haru.more_iss.spell.ender.FreischutzSpell;
import nekotori_haru.more_iss.spell.holy.ProvidentialConduitSpell;
import nekotori_haru.more_iss.spell.ice.AbsoluteZeroSpell;
import nekotori_haru.more_iss.spell.lightning.ThunderboltFlash;
import nekotori_haru.more_iss.util.DisintegrationState;
import nekotori_haru.more_iss.util.DisintegrationTargetManager;
import org.slf4j.Logger;

@Mod(More_iss.MODID)
public class More_iss {

    public static final String MODID = "more_iss";
    public static final Logger LOGGER = LogUtils.getLogger();

    // ───────────── DeferredRegister ─────────────

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, MODID);

    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // ───────────── ブロック ─────────────

    public static final RegistryObject<Block> ARCANE_CRAFTING_TABLE =
            BLOCKS.register("fusion_table", () ->
                    new ArcaneCraftingTableBlock(
                            Block.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(3.0f)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final RegistryObject<Item> ARCANE_CRAFTING_TABLE_ITEM =
            ITEMS.register("fusion_table", () ->
                    new BlockItem(ARCANE_CRAFTING_TABLE.get(), new Item.Properties())
            );

    public static final RegistryObject<BlockEntityType<ArcaneCraftingTableBlockEntity>> ARCANE_CRAFTING_TABLE_BE =
            BLOCK_ENTITIES.register("fusion_table", () ->
                    BlockEntityType.Builder
                            .of(ArcaneCraftingTableBlockEntity::new, ARCANE_CRAFTING_TABLE.get())
                            .build(null)
            );

    public static final RegistryObject<MenuType<ArcaneCraftingMenu>> ARCANE_CRAFTING_MENU =
            MENU_TYPES.register("fusion_table", () ->
                    IForgeMenuType.create(ArcaneCraftingMenu::new)
            );

    // ───────────── スペル ─────────────

    public static final RegistryObject<AbstractSpell> ENDER_SHOOTING_STAR =
            SPELLS.register("ender_shooting_star", EnderShootingStar::new);

    public static final RegistryObject<AbstractSpell> FREISCHUTZ =
            SPELLS.register("freischutz", FreischutzSpell::new);

    public static final RegistryObject<AbstractSpell> RAISEN =
            SPELLS.register("raisen", ThunderboltFlash::new);

    public static final RegistryObject<AbstractSpell> OVERBURST_BLOOD =
            SPELLS.register("overburst_blood", OverburstBloodSpell::new);

    public static final RegistryObject<AbstractSpell> PROVIDENTIAL_CONDUIT =
            SPELLS.register("providential_conduit", ProvidentialConduitSpell::new);

    public static final RegistryObject<AbstractSpell> SACRIFICIAL_EDGE =
            SPELLS.register("sacrificial_edge", SacrificialEdgeSpell::new);

    public static final RegistryObject<AbstractSpell> SOUL_LINK =
            SPELLS.register("soul_link", SoulLinkSpell::new);

    public static final RegistryObject<AbstractSpell> ABSOLUTE_ZERO =
            SPELLS.register("absolute_zero", AbsoluteZeroSpell::new);

    public static final RegistryObject<AbstractSpell> DISINTEGRATION =
            SPELLS.register("disintegration", DisintegrationSpell::new);

    public static final RegistryObject<AbstractSpell> HEAVENLY_BLAST =
            SPELLS.register("heavenly_blast", HeavenlyBlastSpell::new);

    public static final RegistryObject<AbstractSpell> FUNNEL =
            SPELLS.register("funnel", FunnelSpell::new);

    // ───────────── コンストラクタ ─────────────

    public More_iss() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 🔥 順番が超重要

        ModAttributes.register(modEventBus);                     // ①
        SynthesisSchoolRegistry.SCHOOLS.register(modEventBus);   // ②

        ModEffects.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITIES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        SPELLS.register(modEventBus); // ← 最後

        // イベントリスナーの登録
        modEventBus.addListener(this::commonSetup); // ⭕ 共通セットアップを追加
        modEventBus.addListener(this::registerRecipes);
        modEventBus.addListener(this::clientSetup);

        DisintegrationState.init();
        DisintegrationTargetManager.init();
    }

    // ───────────── 共通セットアップ ─────────────

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // ⭕ ここでパケットロックがかかる前の「起動時」にネットワークを安全に登録
            ModNetwork.register();
        });
    }

    // ───────────── レシピ ─────────────

    private void registerRecipes(net.minecraftforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_TYPE)) {
            event.register(
                    Registries.RECIPE_TYPE,
                    new ResourceLocation(MODID, "arcane_crafting"),
                    () -> ArcaneCraftingRecipeType.INSTANCE
            );
        }

        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            event.register(
                    Registries.RECIPE_SERIALIZER,
                    new ResourceLocation(MODID, "arcane_crafting"),
                    () -> ArcaneCraftingRecipeSerializer.INSTANCE
            );
        }
    }

    // ───────────── クライアント ─────────────

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ARCANE_CRAFTING_MENU.get(), ArcaneCraftingScreen::new)
        );
    }
}