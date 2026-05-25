package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.effect.FrostArmorEffect;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDamageEvent; // 🌟 DamageEventに戻す
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 氷の鎧専用のダメージ変換・衝撃吸収（Absorption）制御イベントハンドラー
 */
@Mod.EventBusSubscriber(modid = More_iss.MODID)
public class FrostArmorDamageEventHandler {

    // 🌟 全てのModやバニラの軽減計算（防具・黄色ハートの消費）が「完全に終わった直後」に割り込む
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDamageConvert(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || event.isCanceled()) return;

        // 1. 対象に「氷の鎧」エフェクトが付与されているかチェック
        MobEffectInstance effectInstance = entity.getEffect(ModEffects.FROST_ARMOR.get());
        if (effectInstance != null) {

            // 🌟 このイベントでの event.getAmount() は、既存の黄色ハートをも突き抜けて
            // 「実際に赤ハート（実体力）を削ることが確定した最終実ダメージ」が入っています。
            float netDamageToHealth = event.getAmount();

            // 🛑 【無限増殖ストッパー】
            // ダベージが既存の黄色ハートで完全に吸われ、赤ハートへのダメージが0以下の場合は、
            // 新たな黄色ハートは一切生成せず、ここで完全に処理を終了する（対策完了）
            if (netDamageToHealth <= 0) {
                return;
            }

            // 2. エフェクトインスタンスから、現在のレベルに応じた変換率（100%〜140%等）を取得
            if (effectInstance.getEffect() instanceof FrostArmorEffect frostArmorEffect) {
                int amplifier = effectInstance.getAmplifier();
                float conversionPercent = frostArmorEffect.getConversionPercent(amplifier);

                // 🌟 赤ハートが削られる「ガチの実ダメージ」をベースに、1.4倍（最大時）の値を計算
                float generatedAbsorption = netDamageToHealth * conversionPercent;

                // 現在残っている黄色ハートの残高（今回の攻撃で0になっているはずですが、念のため取得）
                float currentAbsorption = entity.getAbsorptionAmount();

                // 3. 確定した実ダメージに応じた新しい黄色ハートの壁をセット
                entity.setAbsorptionAmount(currentAbsorption + generatedAbsorption);

                // 4. シールド発動時のパーティクル演出
                if (!entity.level().isClientSide && entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            15, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }
    }
}