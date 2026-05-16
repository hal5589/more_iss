package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class OverburstEffect extends MobEffect {
    public OverburstEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
        // 属性モディファイアは削除し、イベントハンドラ側で動的に計算します
    }
}