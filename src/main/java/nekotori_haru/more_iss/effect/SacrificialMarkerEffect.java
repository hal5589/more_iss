package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SacrificialMarkerEffect extends MobEffect {

    public SacrificialMarkerEffect() {
        // 第1引数: デバフ枠にするため HARMFUL を指定
        // 第2引数: パーティクルやアイコン等の液体カラー（不可視なので基本何色でもOKですが、血属性っぽく暗い赤にしています）
        super(MobEffectCategory.HARMFUL, 0x7A0016);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // このエフェクトは魔法レベルを「保持するだけ」のマーカーなので、
        // 毎tickのドットダメージ処理などは何も書かずに空（カラ）で大丈夫です。
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 毎tick処理（applyEffectTick）を呼び出す必要はないので常にfalseで負荷を減らします
        return false;
    }
}