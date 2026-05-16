package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.effect.OverburstEffect;
import nekotori_haru.more_iss.effect.SacrificialBleedEffect; // 💡自傷デバフのインポートを追加
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

    // 💡 自傷デバフ（サクリフィシャル・ブリード）の登録
    public static final RegistryObject<MobEffect> SACRIFICIAL_BLEED =
            MOB_EFFECTS.register("sacrificial_bleed", SacrificialBleedEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}