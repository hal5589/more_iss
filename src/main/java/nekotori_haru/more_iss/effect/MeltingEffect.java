package nekotori_haru.more_iss.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MeltingEffect extends MobEffect {
    public MeltingEffect() {
        // デバフ（有害）カテゴリ、液体や溶解をイメージしたオレンジ・黄緑系のカラーコード (例: 0xD4AF37)
        super(MobEffectCategory.HARMFUL, 13938487);

        // 🌟 レベル1ごとに防御値（Armor）を -1.0 する設定
        this.addAttributeModifier(Attributes.ARMOR, "7a853bc2-ef42-4919-b541-01f787e914bf",
                -1.0D, AttributeModifier.Operation.ADDITION);

        // 🌟 レベル1ごとに防具強度（Armor Toughness）を -1.0 する設定
        this.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, "5a198fc3-bf32-4718-a621-02f887e925cf",
                -1.0D, AttributeModifier.Operation.ADDITION);
    }
}