package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}