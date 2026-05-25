package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;

public class FrostArmorEffect extends MobEffect {

    public FrostArmorEffect() {
        // メリットバフ（BENEFICIAL）、氷っぽい水色（0x90E0EF）に設定
        // 🌟 防御力上昇モディファイアを削除し、スーパーコンストラクタのみにしました
        super(MobEffectCategory.BENEFICIAL, 0x90E0EF);
    }

    // 4チックに1回処理を走らせる（負荷軽減＆パーティクル密度調整）
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    // バフ中のエフェクト処理（プレイヤーの周りに白い冷気パーティクルを出す）
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth();
            double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth();

            // 氷っぽい雪のパーティクルをふんわり出す
            entity.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    /**
     * ハルさんの爆盛り魔法威力（amplifier）から、変換倍率（n%）を安全に計算するメソッド
     */
    public float getConversionPercent(int amplifier) {
        // 基本変換率 5% ＋ (エフェクトレベル [＝爆盛り魔法威力値] × 1.5%)
        float percent = 0.05f + (amplifier * 0.015f);
        return Math.max(0.0f, percent); // マイナスにならないようにガード
    }
}