package nekotori_haru.more_iss.registry;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class MoreIssConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    // ダメージキャップの設定値
    public static final ForgeConfigSpec.ConfigValue<Double> BOSS_DAMAGE_CAP;

    static {
        BUILDER.push("Eternal Wizard");

        BUILDER.comment("Damage cap for EternalWizardEntity (per hit). Default: 20.0");
        BOSS_DAMAGE_CAP = BUILDER
                .comment("Maximum damage per hit. Set to 0 to disable cap.")
                .defineInRange("damageCap", 20.0, 0.0, 10000.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    public static float getDamageCap() {
        return BOSS_DAMAGE_CAP.get().floatValue();
    }

    public static boolean isDamageCapEnabled() {
        return BOSS_DAMAGE_CAP.get() > 0.0;
    }
}