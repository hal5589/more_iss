package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DemonicCovenantEffect extends MobEffect {
    public DemonicCovenantEffect() {
        // HARMFUL（悪性デバフ）に変更。色は禍々しい夜の紫（#4B0082）
        super(MobEffectCategory.HARMFUL, 0x4B0082);
    }
}