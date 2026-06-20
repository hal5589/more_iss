package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.*;
import nekotori_haru.more_iss.entity.summoned.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, More_iss.MODID);

    public static final RegistryObject<EntityType<BaseBeamVisualEntity>> BASE_BEAM_VISUAL = ENTITIES.register("base_beam_visual", () ->
            EntityType.Builder.<BaseBeamVisualEntity>of(BaseBeamVisualEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(64).updateInterval(1).build("base_beam_visual"));

    public static final RegistryObject<EntityType<NapalmBombEntity>> NAPALM_BOMB = ENTITIES.register("napalm_bomb", () ->
            EntityType.Builder.<NapalmBombEntity>of(NapalmBombEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(64).updateInterval(1).build("napalm_bomb"));

    public static final RegistryObject<EntityType<GlacialSwordEntity>> GLACIAL_SWORD = ENTITIES.register("glacial_sword", () ->
            EntityType.Builder.<GlacialSwordEntity>of(GlacialSwordEntity::new, MobCategory.MISC)
                    .sized(3.0f, 4.0f).clientTrackingRange(64).updateInterval(1).build("glacial_sword"));

    public static final RegistryObject<EntityType<PolychromaticLanceEntity>> POLYCHROMATIC_LANCE =
            ENTITIES.register("polychromatic_lance", () -> EntityType.Builder.<PolychromaticLanceEntity>of(
                            PolychromaticLanceEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("polychromatic_lance"));

    public static final RegistryObject<EntityType<PolychromaticBeamEntity>> POLYCHROMATIC_BEAM =
            ENTITIES.register("polychromatic_beam", () -> EntityType.Builder.<PolychromaticBeamEntity>of(
                            PolychromaticBeamEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("polychromatic_beam"));

    public static final RegistryObject<EntityType<SummonedCryomancer>> SUMMONED_CRYOMANCER = ENTITIES.register(
            "summoned_cryomancer",
            () -> EntityType.Builder.<SummonedCryomancer>of(
                            SummonedCryomancer::new,
                            MobCategory.MISC
                    )
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("summoned_cryomancer")
    );

    public static final RegistryObject<EntityType<SummonedPyromancer>> SUMMONED_PYROMANCER = ENTITIES.register(
            "summoned_pyromancer",
            () -> EntityType.Builder.<SummonedPyromancer>of(
                            SummonedPyromancer::new,
                            MobCategory.MISC
                    )
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("summoned_pyromancer")
    );

    // ⭐ 修正: ID を "summoned_priest" に変更
    public static final RegistryObject<EntityType<SummonedPriest>> SUMMONED_PRIEST = ENTITIES.register(
            "summoned_priest",  // ← 正しいIDに修正
            () -> EntityType.Builder.<SummonedPriest>of(
                            SummonedPriest::new,
                            MobCategory.MISC
                    )
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("summoned_priest")  // ← build() も一致させる
    );

    public static final RegistryObject<EntityType<SummonedArchevoker>> SUMMONED_ARCHEVOKER = ENTITIES.register(
            "summoned_archevoker",
            () -> EntityType.Builder.<SummonedArchevoker>of(
                            SummonedArchevoker::new,
                            MobCategory.MISC
                    )
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("summoned_archevoker")
    );

    public static final RegistryObject<EntityType<SummonedApothecarist>> SUMMONED_APOTHECARIST = ENTITIES.register(
            "summoned_apothecarist",
            () -> EntityType.Builder.<SummonedApothecarist>of(
                            SummonedApothecarist::new,
                            MobCategory.MISC
                    )
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("summoned_apothecarist")
    );

    public static final RegistryObject<EntityType<EternalWizardEntity>> ETERNAL_WIZARD = ENTITIES.register(
            "eternal_wizard",
            () -> EntityType.Builder.<EternalWizardEntity>of(
                            EternalWizardEntity::new,
                            MobCategory.MONSTER
                    )
                    .sized(0.8f, 2.2f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("eternal_wizard")
    );

    public static final RegistryObject<EntityType<StarEntity>> STAR = ENTITIES.register("star",
            () -> EntityType.Builder.<StarEntity>of(StarEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(More_iss.MODID + ":star")
    );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}