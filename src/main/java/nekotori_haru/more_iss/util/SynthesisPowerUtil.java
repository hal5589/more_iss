package nekotori_haru.more_iss.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 融合魔法専用のスペルパワー倍率計算。
 *
 * コンセプト：「全属性の強化が乗る」
 *   → プレイヤーが装備・エンチャント等で積んでいる
 *     全スクールのスペルパワー属性値を合算し、
 *     その平均値から追加倍率を算出する。
 *
 * 倍率の計算式:
 *   avgBonus = Σ(各スクールのspellPower追加分) / スクール数
 *   multiplier = 1.0 + avgBonus × SYNTHESIS_FACTOR
 *   ※ SYNTHESIS_FACTOR=3.0 / 最小1.0 / 最大5.0 にクランプ
 *
 * 使い方:
 *   float damage = baseDamage * SynthesisPowerUtil.getMultiplier(caster);
 */
public final class SynthesisPowerUtil {

    /** 全スクール平均ボーナスに掛ける増幅係数 */
    private static final float SYNTHESIS_FACTOR = 3.0f;

    private SynthesisPowerUtil() {}

    /**
     * 融合スペルパワー倍率を返す（最低1.0、最大5.0）。
     *
     * @param caster 詠唱者
     * @return スペルパワー倍率
     */
    public static float getMultiplier(LivingEntity caster) {
        var schoolRegistry = SchoolRegistry.REGISTRY.get();
        if (schoolRegistry == null) return 1.0f;

        double totalBonus = 0.0;
        int count = 0;

        for (SchoolType school : schoolRegistry) {
            // getPowerFor() を使用してスペルパワーを取得
            double basePower = 1.0; // デフォルト値
            double actualPower = school.getPowerFor(caster);

            if (actualPower > basePower) {
                totalBonus += actualPower - basePower;
                count++;
            }
        }

        if (count == 0) return 1.0f;

        float multiplier = (float)(1.0 + (totalBonus / count) * SYNTHESIS_FACTOR);
        return Math.max(1.0f, Math.min(5.0f, multiplier));
    }
}