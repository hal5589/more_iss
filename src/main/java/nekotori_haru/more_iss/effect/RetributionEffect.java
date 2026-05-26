package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class RetributionEffect extends MobEffect {

    // 🪪 モディファイアのUUIDを定数として固定
    private static final UUID RETRIBUTION_HEALTH_UUID = UUID.fromString("c0b117bc-8aef-4b4d-a77b-603126ec054c");

    public RetributionEffect() {
        // HARMFUL（悪性デバフ）、色は血のような暗い赤（#4A0000）
        super(MobEffectCategory.HARMFUL, 0x4A0000);
    }

    // 🌟【バグ修正：付与された瞬間の処理】
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        var maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            // 重複登録を防ぐため、一度既存のモディファイアを安全に削除
            maxHealthAttr.removeModifier(RETRIBUTION_HEALTH_UUID);

            // 🛠️ 確実性を高めるため「MULTIPLY_BASE（基礎値への乗算）」に変更し、50%減少させる
            AttributeModifier modifier = new AttributeModifier(
                    RETRIBUTION_HEALTH_UUID,
                    "Retribution Health Penalty",
                    -0.5D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            maxHealthAttr.addTransientModifier(modifier);

            // ⚠️ 最大体力が減ったことで、現在の体力が最大体力を超えて見た目がバグるのを防ぐため、現在の体力を同期
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
        super.addAttributeModifiers(entity, attributeMap, amplifier);
    }

    // 🌟【バグ修正：デバフが解除された瞬間の処理】
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        var maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            // 「報い」が終わったらペナルティ用のモディファイアを確実に剥がして、最大体力を元に戻す
            maxHealthAttr.removeModifier(RETRIBUTION_HEALTH_UUID);
        }
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }
}