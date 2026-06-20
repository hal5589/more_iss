package nekotori_haru.more_iss;

import com.mojang.logging.LogUtils;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import nekotori_haru.more_iss.client.ArcaneCraftingScreen;
import nekotori_haru.more_iss.client.model.GlacialExecution;
import nekotori_haru.more_iss.client.renderer.*;
import nekotori_haru.more_iss.entity.EternalWizardEntity;
import nekotori_haru.more_iss.event.FrostArmorDamageEventHandler;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import nekotori_haru.more_iss.network.ModNetwork;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeSerializer;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeType;
import nekotori_haru.more_iss.registry.*;
import nekotori_haru.more_iss.util.DisintegrationState;
import nekotori_haru.more_iss.util.DisintegrationTargetManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(More_iss.MODID)
public class More_iss {

    public static final String MODID = "more_iss";
    public static final Logger LOGGER = LogUtils.getLogger();

    // ───────────── DeferredRegister ─────────────
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // ───────────── ブロックエンティティ ─────────────
    public static final RegistryObject<BlockEntityType<ArcaneCraftingTableBlockEntity>> ARCANE_CRAFTING_TABLE_BE = BLOCK_ENTITIES.register("fusion_table", () ->
            BlockEntityType.Builder.of(ArcaneCraftingTableBlockEntity::new, ModBlocks.ARCANE_CRAFTING_TABLE.get()).build(null));

    public static final RegistryObject<MenuType<ArcaneCraftingMenu>> ARCANE_CRAFTING_MENU = MENU_TYPES.register("fusion_table", () ->
            IForgeMenuType.create(ArcaneCraftingMenu::new));

    // ───────────── コンストラクタ ─────────────
    public More_iss() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // レジストリ登録
        ModAttributes.register(modEventBus);
        SynthesisSchoolRegistry.SCHOOLS.register(modEventBus);
        ModEffects.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSpells.register(modEventBus);
        ModEntities.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        ModItems.register(modEventBus);

        BLOCK_ENTITIES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        // イベントリスナー
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerRecipes);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerLayerDefinitions);

        // ⭐ 属性登録イベント（EternalWizardEntity 用）
        modEventBus.addListener(this::registerAttributes);

        DisintegrationState.init();
        DisintegrationTargetManager.init();

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new FrostArmorDamageEventHandler());
    }

    // ───────────── 共通セットアップ ─────────────
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModNetwork.register());
    }

    // ───────────── レシピ登録 ─────────────
    private void registerRecipes(net.minecraftforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_TYPE)) {
            event.register(Registries.RECIPE_TYPE, new ResourceLocation(MODID, "arcane_crafting"), () -> ArcaneCraftingRecipeType.INSTANCE);
        }
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            event.register(Registries.RECIPE_SERIALIZER, new ResourceLocation(MODID, "arcane_crafting"), () -> ArcaneCraftingRecipeSerializer.INSTANCE);
        }
    }

    // ───────────── クライアントセットアップ ─────────────
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ARCANE_CRAFTING_MENU.get(), ArcaneCraftingScreen::new));
    }

    // ───────────── レンダラー登録 ─────────────
    private void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BASE_BEAM_VISUAL.get(), BaseBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.BEAM_WARNING.get(), BeamWarningRenderer::new);
        event.registerEntityRenderer(ModEntities.NAPALM_BOMB.get(), NapalmBombRenderer::new);
        event.registerEntityRenderer(ModEntities.POLYCHROMATIC_LANCE.get(), PolychromaticLanceRenderer::new);
        event.registerEntityRenderer(ModEntities.POLYCHROMATIC_BEAM.get(), PolychromaticBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.GLACIAL_SWORD.get(), GlacialSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.SUMMONED_PYROMANCER.get(), SummonedPyromancerRenderer::new);
        event.registerEntityRenderer(ModEntities.ETERNAL_WIZARD.get(), EternalWizardRenderer::new);
        event.registerEntityRenderer(ModEntities.STAR.get(), StarRenderer::new);
    }

    // ───────────── レイヤー定義登録 ─────────────
    private void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BaseBeamRenderer.MODEL_LAYER_LOCATION, BaseBeamRenderer::createBodyLayer);
        event.registerLayerDefinition(BeamWarningRenderer.MODEL_LAYER_LOCATION, BeamWarningRenderer::createBodyLayer);
        event.registerLayerDefinition(GlacialExecution.LAYER_LOCATION, GlacialExecution::createBodyLayer);
    }

    // ⭐ 属性登録メソッド（これが EternalWizardEntity 召喚に必須！）


    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ETERNAL_WIZARD.get(), EternalWizardEntity.prepareAttributes().build());
        LOGGER.info("EternalWizardEntity attributes registered successfully!");
    }
}