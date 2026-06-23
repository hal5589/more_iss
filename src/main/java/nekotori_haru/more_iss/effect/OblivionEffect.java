package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「忘却の彼方 (Oblivion)」デバフ。
 * このエフェクトを持つ間、対象はISS本体・他アドオンを含む
 * 全ての魔法のキャスト試行が即座にキャンセルされる。
 * 実際のキャンセル判定は MixinAbstractSpell / MixinAbstractSpellCastingMob 側で
 * hasEffect() を見るだけなので、このクラス自体はマーカーとしての役割のみ。
 * tickごとの追加処理は持たない。
 */
public class OblivionEffect extends MobEffect {

    // 深淵を思わせる暗紫色
    private static final int OBLIVION_COLOR = 0x2A0845;

    public OblivionEffect() {
        super(MobEffectCategory.HARMFUL, OBLIVION_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 毎tickの処理は不要（封印判定はMixin側でhasEffectを見るだけ）
        return false;
    }
}