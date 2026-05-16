package nekotori_haru.more_iss.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SacrificialBleedEffect extends MobEffect {

    public SacrificialBleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x990000);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 現在の累積Hit数に応じたダメージ計算（2.0f ＋ 1Hitあたり0.25f）
        float currentVoidDamage = 2.0f + ((float) amplifier * 0.25f);

        // 💡 修正ポイント：このダメージで死亡するかどうかをチェック
        // 防御貫通（奈落扱い）で殺すため、HPが計算ダメージ以下ならカスタム死亡処理へ
        if (entity.getHealth() <= currentVoidDamage) {
            // 💡 独自の死亡メッセージ用キーを持ったダメージソースを作成
            // (第1引数に 奈落をベースにしたDamageType を指定し、無敵・防具を完全貫通させる)
            DamageSource customSource = new DamageSource(
                    entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FELL_OUT_OF_WORLD)
            ) {
                @Override
                public Component getLocalizedDeathMessage(LivingEntity deadEntity) {
                    // 言語ファイル（json）で指定するカスタムキルログのキー
                    String id = "death.attack.sacrificial_purge";
                    return Component.translatable(id, deadEntity.getDisplayName());
                }
            };

            // このカスタムソースでダメージを与えて死亡させる（キルログが上書きされる）
            entity.hurt(customSource, currentVoidDamage);
        } else {
            // まだ死なない場合は、通常通り奈落ダメージとして刻む
            entity.hurt(entity.damageSources().fellOutOfWorld(), currentVoidDamage);
        }
    }
}