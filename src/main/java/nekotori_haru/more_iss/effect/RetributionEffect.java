package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class RetributionEffect extends MobEffect {
    public RetributionEffect() {
        // HARMFUL（悪性デバフ）、色は血のような暗い赤（#4A0000）
        super(MobEffectCategory.HARMFUL, 0x4A0000);

        // 最大体力を50%減少させるモディファイアを紐付け
        // MULTIPLIER_TOTAL を ADD_MULTIPLIED_TOTAL に修正
        this.addAttributeModifier(Attributes.MAX_HEALTH,
                "c0b117bc-8aef-4b4d-a77b-603126ec054c",
                -0.5D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}