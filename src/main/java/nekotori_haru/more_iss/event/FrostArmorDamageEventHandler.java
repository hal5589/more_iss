package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDamageEvent; // 🌟 DamageEvent（LOWEST）に戻す
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 氷の鎧専用のダメージ変換・衝撃吸収（Absorption）制御イベントハンドラー
 */
public class FrostArmorDamageEventHandler {

    // 🌟 全ての防具・エンチャント・ポーションの軽減計算が「完全に終わった直後」に割り込む
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDamageConvert(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || event.isCanceled()) return;
        if (entity.level().isClientSide) return;

        // 1. プレイヤーに「氷の鎧」バフがかかっているかチェック
        MobEffectInstance effectInstance = entity.getEffect(ModEffects.FROST_ARMOR.get());
        if (effectInstance != null) {

            // 🌟 これが「防具や耐性で極限まで軽減された後」かつ「既存の黄色ハートも突き抜けた」最終確定実ダメージです
            float finalArmorReducedDamage = event.getAmount();

            // 🛑 【無限増殖ストッパー】
            // 今回の攻撃が、既存の黄色ハートだけで完全に吸いきれた場合（＝赤ハートへのダメージが0の場合）は、
            // 新しいシールドは 1ミリも生成せず、ここで完全に処理を終了します。
            if (finalArmorReducedDamage <= 0) {
                return;
            }

            // 100倍されたアンプリファイアから、安全に元の魔法レベルを復元
            int rawAmplifier = effectInstance.getAmplifier();
            int spellLevel = rawAmplifier / 100;
            if (spellLevel <= 0) {
                spellLevel = 1;
            }

            // 仕様通りの変換率を計算（レベル5 = 1.4倍）
            float conversionPercent = 1.0f + (spellLevel - 1) * 0.10f;

            // 🌟 全ての軽減計算が終わった「ガチの確定被弾ダメージ」をベースに、1.4倍のシールド量を計算
            float generatedAbsorption = finalArmorReducedDamage * conversionPercent;

            // 現在残っている黄色ハートの残高（今回の貫通によって0になっているはずですが、念のため取得）
            float currentAbsorption = entity.getAbsorptionAmount();

            // 2. 軽減後ダメージに応じた新しいシールドの壁を上乗せセット
            entity.setAbsorptionAmount(currentAbsorption + generatedAbsorption);

            // 3. シールド補充時のパーティクル演出
            if (entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        15, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
}