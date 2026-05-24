package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, More_iss.MODID);

    // 融合魔法威力（ベース値 1.0 = 100%, 最小 0.0, 最大 100.0）
    public static final RegistryObject<Attribute> SYNTHESIS_SPELL_POWER = ATTRIBUTES.register("synthesis_spell_power",
            () -> new RangedAttribute("attribute.name." + More_iss.MODID + ".synthesis_spell_power", 1.0D, 0.0D, 100.0D).setSyncable(true));

    // 融合魔法耐性（ベース値 0.0 = 0%, 最小 -100.0, 最大 1.0 = 100%軽減）
    public static final RegistryObject<Attribute> SYNTHESIS_MAGIC_RESIST = ATTRIBUTES.register("synthesis_magic_resist",
            () -> new RangedAttribute("attribute.name." + More_iss.MODID + ".synthesis_magic_resist", 0.0D, -100.0D, 1.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}