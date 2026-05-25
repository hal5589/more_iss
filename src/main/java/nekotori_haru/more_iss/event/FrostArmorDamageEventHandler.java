package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.effect.FrostArmorEffect;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 氷の鎧専用のダメージ変換・衝撃吸収（Absorption）制御イベントハンドラー
 */
public class FrostArmorDamageEventHandler {

    // ForgeのMinecraft Forge Event Busに手動登録するため、アノテーションは不要になります
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDamageConvert(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || event.isCanceled()) return;

        // サーバーサイドのみで処理を実行（クライアント側での重複処理やクラッシュを防止）
        if (entity.level().isClientSide) return;

        // 1. 対象に「氷の鎧」エフェクトが付与されているかチェック
        MobEffectInstance effectInstance = entity.getEffect(ModEffects.FROST_ARMOR.get());
        if (effectInstance != null) {

            // 最終的に赤ハート（実体力）を削ることが確定した最終実ダメージ
            float netDamageToHealth = event.getAmount();

            // 【無限増殖ストッパー】
            // ダメージが既存の黄色ハートで完全に吸われ、赤ハートへのダメージが0以下の場合は処理を終了
            if (netDamageToHealth <= 0) {
                return;
            }

            // 2. エフェクトインスタンスから、現在のレベルに応じた変換率（100%〜140%等）を取得
            if (effectInstance.getEffect() instanceof FrostArmorEffect frostArmorEffect) {
                int amplifier = effectInstance.getAmplifier();
                float conversionPercent = frostArmorEffect.getConversionPercent(amplifier);

                // 実ダメージ量に変換率を掛け合わせ、新しく生成する衝撃吸収量を計算
                float generatedAbsorption = netDamageToHealth * conversionPercent;

                // 現在残っている黄色ハートの残高を取得
                float currentAbsorption = entity.getAbsorptionAmount();

                // 3. 確定した実ダメージに応じた新しい黄色ハートの壁をセット
                entity.setAbsorptionAmount(currentAbsorption + generatedAbsorption);

                // 4. シールド発動時のパーティクル演出（サーバーレベルであることを確定させて安全にスポーン）
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            15, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }
    }
}