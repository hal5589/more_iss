package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.spell.blood.OverburstBloodSpell;
import nekotori_haru.more_iss.spell.blood.SacrificialEdgeSpell;
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
import nekotori_haru.more_iss.spell.synthesis.PolychromaticLanceSpell;

public class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, More_iss.MODID);

    // 火系統
    public static final RegistryObject<AbstractSpell> FLAME_RAY = SPELLS.register("flame_ray", FlameRaySpell::new);
    public static final RegistryObject<AbstractSpell> MARK_OF_DETONATION = SPELLS.register("mark_of_detonation", MarkOfDetonationSpell::new);
    public static final RegistryObject<AbstractSpell> NAPALM_RAIN = SPELLS.register("napalm_rain", NapalmRainSpell::new);
    public static final RegistryObject<AbstractSpell> PHOENIX_BLESSING = SPELLS.register("phoenix_blessing", PhoenixBlessingSpell::new);
    public static final RegistryObject<AbstractSpell> METEOR_FALL = SPELLS.register("meteor_fall", MeteorFallSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_PYROMANCER = SPELLS.register("summon_pyromancer", SummonPyromancerSpell::new);
    public static final RegistryObject<AbstractSpell> INFERNO_STEP = SPELLS.register("inferno_step", InfernoStepSpell::new);


    // 氷系統
    public static final RegistryObject<AbstractSpell> FROST_ARMOR = SPELLS.register("frost_armor", FrostArmorSpell::new);
    public static final RegistryObject<AbstractSpell> GLACIAL_EXECUTION = SPELLS.register("glacial_execution", GlacialExecutionSpell::new);
    public static final RegistryObject<AbstractSpell> ABSOLUTE_ZERO = SPELLS.register("absolute_zero", AbsoluteZeroSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_CRYOMANCER = SPELLS.register("summon_cryomancer", SummonCryomancerSpell::new);
    public static final RegistryObject<AbstractSpell> CRYO_CONVERGENCE = SPELLS.register("cryo_convergence", CryoConvergenceSpell::new);

    // 雷系統
    public static final RegistryObject<AbstractSpell> RAISEN = SPELLS.register("raisen", ThunderboltFlash::new);
    public static final RegistryObject<AbstractSpell> PLASMA_STEP = SPELLS.register("plasma_step", PlasmaStepSpell::new);

    // 自然系統
    public static final RegistryObject<AbstractSpell> SOLAR_RAY = SPELLS.register("solar_ray", SolarRaySpell::new);
    public static final RegistryObject<AbstractSpell> UNFADING = SPELLS.register("unfading", UnfadingSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_APOTHECARIST = SPELLS.register("summon_apothecarist", SummonApothecaristSpell::new);


    // 聖系統
    public static final RegistryObject<AbstractSpell> HOLY_RAY = SPELLS.register("holy_ray", HolyRaySpell::new);
    public static final RegistryObject<AbstractSpell> HEAVENLY_BLAST = SPELLS.register("heavenly_blast", HeavenlyBlastSpell::new);
    public static final RegistryObject<AbstractSpell> PROVIDENTIAL_CONDUIT = SPELLS.register("providential_conduit", ProvidentialConduitSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_PRIEST = SPELLS.register("summon_priest", SummonPriestSpell::new);


    // エンダー系統
    public static final RegistryObject<AbstractSpell> ENDER_SHOOTING_STAR = SPELLS.register("ender_shooting_star", EnderShootingStar::new);
    public static final RegistryObject<AbstractSpell> FREISCHUTZ = SPELLS.register("freischutz", FreischutzSpell::new);
    public static final RegistryObject<AbstractSpell> VOID_RAY = SPELLS.register("void_ray", VoidRaySpell::new);

    // 召喚系統
    public static final RegistryObject<AbstractSpell> SPECTAL_RAY = SPELLS.register("spectal_ray", SpectalRaySpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_ARCHEVOKER = SPELLS.register("summon_archevoker", SummonArchevokerSpell::new);


    // エルドリッチ系統
    public static final RegistryObject<AbstractSpell> SOUL_LINK = SPELLS.register("soul_link", SoulLinkSpell::new);
    public static final RegistryObject<AbstractSpell> OBLIVION = SPELLS.register("oblivion", OblivionSpell::new);


    // 合成系統
    public static final RegistryObject<AbstractSpell> OVERBURST_BLOOD = SPELLS.register("overburst_blood", OverburstBloodSpell::new);
    public static final RegistryObject<AbstractSpell> SACRIFICIAL_EDGE = SPELLS.register("sacrificial_edge", SacrificialEdgeSpell::new);
    public static final RegistryObject<AbstractSpell> DISINTEGRATION = SPELLS.register("disintegration", DisintegrationSpell::new);
    public static final RegistryObject<AbstractSpell> FUNNEL = SPELLS.register("funnel", FunnelSpell::new);
    public static final RegistryObject<AbstractSpell> POLYCHROMATIC_LANCE = SPELLS.register("polychromatic_lance", PolychromaticLanceSpell::new);
    public static final RegistryObject<AbstractSpell> POLYCHROMATIC_BEAM = SPELLS.register("polychromatic_beam", PolychromaticBeamSpell::new);
    public static final RegistryObject<AbstractSpell> SUMMON_WIZARDS = SPELLS.register("summon_wizards", SummonWizardsSpell::new);
    public static final RegistryObject<AbstractSpell> STARLIGHT = SPELLS.register("starlight", StarlightSpell::new);
    public static final RegistryObject<AbstractSpell> SEVEN_COLORED_CAGE = SPELLS.register("seven_colored_cage", SevenColoredCageSpell::new);
    public static final RegistryObject<AbstractSpell> LITTLE_MONOLITH = SPELLS.register("little_monolith", LittleMonolithSpell::new);


    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }
}