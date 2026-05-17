package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SoulLinkEffect extends MobEffect {
    public SoulLinkEffect() {
        // 有益な効果（バフ）として登録
        super(MobEffectCategory.BENEFICIAL, 0x3A0066);
    }
}