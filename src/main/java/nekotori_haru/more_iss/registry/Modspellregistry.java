package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.spell.holy.ProvidentialConduitSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class Modspellregistry {

    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY,
                    "more_iss"
            );

    public static final RegistryObject<AbstractSpell> PROVIDENTIAL_CONDUIT_SPELL =
            SPELLS.register("providential_conduit", ProvidentialConduitSpell::new);
}