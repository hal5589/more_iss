package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.spell.fire.*;
import nekotori_haru.more_iss.spell.ice.*;
import nekotori_haru.more_iss.spell.nature.*;
import nekotori_haru.more_iss.spell.holy.*;
import nekotori_haru.more_iss.spell.ender.*;
import nekotori_haru.more_iss.spell.evocation.*;
import nekotori_haru.more_iss.spell.synthesis.*;
import nekotori_haru.more_iss.spell.eldritch.*;
import nekotori_haru.more_iss.spell.lightning.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, More_iss.MODID);

    // 火系統
    public static final RegistryObject<AbstractSpell> FLAME_RAY = SPELLS.register("flame_ray", FlameRaySpell::new);
    public static final RegistryObject<AbstractSpell> MARK_OF_DETONATION = SPELLS.register("mark_of_detonation", MarkOfDetonationSpell::new);
    public static final RegistryObject<AbstractSpell> NAPALM_RAIN = SPELLS.register("napalm_rain", NapalmRainSpell::new);
    public static final RegistryObject<AbstractSpell> PHOENIX_BLESSING = SPELLS.register("phoenix_blessing", PhoenixBlessingSpell::new);

    // 氷系統
    public static final RegistryObject<AbstractSpell> FROST_ARMOR = SPELLS.register("frost_armor", FrostArmorSpell::new);
    public static final RegistryObject<AbstractSpell> GLACIAL_EXECUTION = SPELLS.register("glacial_execution", GlacialExecutionSpell::new);
    public static final RegistryObject<AbstractSpell> ABSOLUTE_ZERO = SPELLS.register("absolute_zero", AbsoluteZeroSpell::new);

    // 雷系統
    public static final RegistryObject<AbstractSpell> RAISEN = SPELLS.register("raisen", ThunderboltFlash::new);

    // 自然系統
    public static final RegistryObject<AbstractSpell> SOLAR_RAY = SPELLS.register("solar_ray", SolarRaySpell::new);
    public static final RegistryObject<AbstractSpell> UNFADING = SPELLS.register("unfading", UnfadingSpell::new);

    // 聖系統
    public static final RegistryObject<AbstractSpell> HOLY_RAY = SPELLS.register("holy_ray", HolyRaySpell::new);
    public static final RegistryObject<AbstractSpell> HEAVENLY_BLAST = SPELLS.register("heavenly_blast", HeavenlyBlastSpell::new);
    public static final RegistryObject<AbstractSpell> PROVIDENTIAL_CONDUIT = SPELLS.register("providential_conduit", ProvidentialConduitSpell::new);

    // エンダー系統
    public static final RegistryObject<AbstractSpell> ENDER_SHOOTING_STAR = SPELLS.register("ender_shooting_star", EnderShootingStar::new);
    public static final RegistryObject<AbstractSpell> FREISCHUTZ = SPELLS.register("freischutz", FreischutzSpell::new);
    public static final RegistryObject<AbstractSpell> VOID_RAY = SPELLS.register("void_ray", VoidRaySpell::new);

    // 召喚系統
    public static final RegistryObject<AbstractSpell> SPECTAL_RAY = SPELLS.register("spectal_ray", SpectalRaySpell::new);

    // エルドリッチ系統
    public static final RegistryObject<AbstractSpell> SOUL_LINK = SPELLS.register("soul_link", SoulLinkSpell::new);

    // 合成系統
    public static final RegistryObject<AbstractSpell> OVERBURST_BLOOD = SPELLS.register("overburst_blood", OverburstBloodSpell::new);
    public static final RegistryObject<AbstractSpell> SACRIFICIAL_EDGE = SPELLS.register("sacrificial_edge", SacrificialEdgeSpell::new);
    public static final RegistryObject<AbstractSpell> DISINTEGRATION = SPELLS.register("disintegration", DisintegrationSpell::new);
    public static final RegistryObject<AbstractSpell> FUNNEL = SPELLS.register("funnel", FunnelSpell::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }
}