package nekotori_haru.more_iss.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nekotori_haru.more_iss.More_iss;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, More_iss.MODID);

    public static final RegistryObject<Attribute> SYNTHESIS_SPELL_POWER = ATTRIBUTES.register(
            "synthesis_spell_power",
            () -> new RangedAttribute("attribute.more_iss.synthesis_spell_power", 1.0, 0.0, 2048.0)
    );

    public static final RegistryObject<Attribute> SYNTHESIS_MAGIC_RESIST = ATTRIBUTES.register(
            "synthesis_magic_resist",
            () -> new RangedAttribute("attribute.more_iss.synthesis_magic_resist", 0.0, 0.0, 1024.0)
    );

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}