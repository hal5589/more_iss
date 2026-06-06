package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PhoenixBlessingEffect extends MobEffect {
    // ⭐ public に変更
    public PhoenixBlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6600);  // オレンジ色
    }
}