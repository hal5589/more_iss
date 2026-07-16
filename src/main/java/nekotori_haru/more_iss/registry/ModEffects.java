package nekotori_haru.more_iss.registry;

import com.mojang.text2speech.OperatingSystem;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.effect.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "more_iss");

    // オーバーバーストバフの登録
    public static final RegistryObject<MobEffect> OVERBURST = MOB_EFFECTS.register("overburst",
            OverburstEffect::new);

    // 自傷デバフ（サクリフィシャル・ブリード）の登録
    public static final RegistryObject<MobEffect> SACRIFICIAL_BLEED =
            MOB_EFFECTS.register("sacrificial_bleed", SacrificialBleedEffect::new);

    // 血液の凝固（魔法レベル記録用・不可視マーカーデバフ）の登録
    public static final RegistryObject<MobEffect> SACRIFICIAL_MARKER =
            MOB_EFFECTS.register("sacrificial_marker", SacrificialMarkerEffect::new);

    // ⭕ 変更：「魂の保護」バフの登録IDを干渉回避のため「soul_protection」に変更
    public static final RegistryObject<MobEffect> SOUL_LINK_PROTECTION =
            MOB_EFFECTS.register("soul_protection", SoulLinkEffect::new);

    // 「魔弾の射手」関連デバフの登録
    public static final RegistryObject<MobEffect> DEMONIC_COVENANT =
            MOB_EFFECTS.register("demonic_covenant", DemonicCovenantEffect::new);

    public static final RegistryObject<MobEffect> FROST_ARMOR =
            MOB_EFFECTS.register("frost_armor", FrostArmorEffect::new);

    public static final RegistryObject<MobEffect> RETRIBUTION =
            MOB_EFFECTS.register("retribution", RetributionEffect::new);

    public static final RegistryObject<MobEffect> MELTING =
            MOB_EFFECTS.register("melting", MeltingEffect::new);

    public static final RegistryObject<MobEffect> DATONATION =
            MOB_EFFECTS.register("datonation", DetonationEffect::new);

    public static final RegistryObject<MobEffect> PHOENIX_BLESSING =
            MOB_EFFECTS.register("phoenix_blessing", PhoenixBlessingEffect::new);

    // 「忘却の彼方」デバフ（魔法使用を完全に封印するマーカーエフェクト）の登録
    public static final RegistryObject<MobEffect> OBLIVION =
            MOB_EFFECTS.register("oblivion", OblivionEffect::new);

    // 魔力炉バフ（SPELL_POWER +400%）の登録
    public static final RegistryObject<MobEffect> MANA_FURNACE_POWER =
            MOB_EFFECTS.register("mana_furnace_power", ManaFurnaceEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}