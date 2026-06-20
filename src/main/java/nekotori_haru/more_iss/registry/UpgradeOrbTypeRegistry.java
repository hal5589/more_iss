package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import nekotori_haru.more_iss.More_iss;

public class UpgradeOrbTypeRegistry {

    public static final ResourceKey<UpgradeOrbType> SYNTHESIS_SPELL_POWER =
            ResourceKey.create(
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY,
                    new ResourceLocation(More_iss.MODID, "synthesis_power")
            );
}