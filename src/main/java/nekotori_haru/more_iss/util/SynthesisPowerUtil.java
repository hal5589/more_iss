package nekotori_haru.more_iss.util;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.ArrayList;
import java.util.List;

public final class SynthesisPowerUtil {

    /** 基本倍率（デフォルト1.0倍 = 元の100%） */
    private static final float BASE_MULTIPLIER = 1.0f;

    // Iron's Spells の属性リスト
    private static final List<Attribute> SPELL_POWER_ATTRIBUTES = new ArrayList<>();

    static {
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.FIRE_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.ICE_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.LIGHTNING_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.ENDER_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.HOLY_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.NATURE_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.BLOOD_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.EVOCATION_SPELL_POWER.get());
        SPELL_POWER_ATTRIBUTES.add(AttributeRegistry.ELDRITCH_SPELL_POWER.get());
    }

    private SynthesisPowerUtil() {}

    /**
     * 合成魔法専用の倍率計算
     * 全属性のスペルパワー増加分をそのまま合計して倍率に変換（上限なし）
     *
     * 計算式: 倍率 = 1.0 + Σ(各属性の増加分)
     *
     * 例: 各属性が+10%（0.1）ずつ、9属性 → 総増加分 = 0.9 → 倍率 = 1.9倍（+90%）
     * 例: 各属性が+100%（1.0）ずつ、9属性 → 総増加分 = 9.0 → 倍率 = 10.0倍
     * 例: attributeコマンドで各属性+1000%（10.0）ずつ → 総増加分 = 90.0 → 倍率 = 91.0倍
     */
    public static float getMultiplier(LivingEntity caster) {
        if (caster == null) return BASE_MULTIPLIER;

        double totalBonus = 0.0;

        for (Attribute attribute : SPELL_POWER_ATTRIBUTES) {
            if (attribute != null) {
                AttributeInstance instance = caster.getAttribute(attribute);
                if (instance != null) {
                    double value = instance.getValue();
                    // 基準値1.0からの増加分のみを加算
                    if (value > 1.0) {
                        totalBonus += (value - 1.0);
                    }
                }
            }
        }

        // 倍率 = 1.0 + 総増加分（そのまま！上限なし！）
        return (float)(BASE_MULTIPLIER + totalBonus);
    }

    /**
     * 実ダメージ計算のヘルパー
     * 基本ダメージに合成倍率を乗算する
     */
    public static float calculateDamage(float baseDamage, LivingEntity caster) {
        return baseDamage * getMultiplier(caster);
    }
}