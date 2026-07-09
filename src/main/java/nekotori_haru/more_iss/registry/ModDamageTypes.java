package nekotori_haru.more_iss.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> SYNTHESIS_MAGIC = create("synthesis_magic");

    private static ResourceKey<DamageType> create(String name) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                new ResourceLocation("more_iss", name)
        );
    }
}