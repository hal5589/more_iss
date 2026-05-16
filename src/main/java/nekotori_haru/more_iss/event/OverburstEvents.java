package nekotori_haru.more_iss.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "more_iss")
public class OverburstEvents {

    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    /**
     * オーバーバーストバフが適用されている間、そのモブのあらゆる回復を阻害する
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null && entity.hasEffect(nekotori_haru.more_iss.registry.ModEffects.OVERBURST.get())) {
            event.setCanceled(true);
        }
    }

    /**
     * 近接攻撃ヒット時：オーバーバーストのバフを消費して、setHealthによる直接HP減少＋死亡判定処理
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (IS_PROCESSING.get()) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // プレイヤー等の直接の近接殴りであるかチェック
        if (source.getDirectEntity() instanceof LivingEntity attacker && source.getMsgId().equals("player")) {

            if (attacker.hasEffect(nekotori_haru.more_iss.registry.ModEffects.OVERBURST.get())) {
                MobEffectInstance effect = attacker.getEffect(nekotori_haru.more_iss.registry.ModEffects.OVERBURST.get());
                if (effect == null) return;

                // 💡 魔法クラスから送られてきたパックデータを解凍
                int packedData = effect.getAmplifier();
                int totalDamagePercent = packedData / 10; // 上位桁：ダメージ％
                int spellLevel = packedData % 10;         // 下位1桁：魔法レベル

                float originalDamage = event.getAmount();

                try {
                    IS_PROCESSING.set(true);

                    // 💡 総想定ダメージの計算
                    float totalCalculatedDamage = originalDamage * (totalDamagePercent / 100.0f);

                    // 💡 魔法レベル（1, 2, 3）に応じて正確に 2割、4割、6割 を算出
                    float voidRatio = spellLevel * 0.2f;
                    if (voidRatio > 0.6f) voidRatio = 0.6f;

                    // 総ダメージを「奈落」と「通常物理」に分配
                    float voidDamage = totalCalculatedDamage * voidRatio;
                    float physicalDamage = totalCalculatedDamage * (1.0f - voidRatio);
                    float totalSumDamage = voidDamage + physicalDamage;

                    // 💡 バニラのダメージ処理をこれ以上通さないようにイベントの物理ダメージ量を0にする
                    event.setAmount(0f);

                    // 無敵フレームを強制破壊
                    target.invulnerableTime = 0;

                    // 💡 現在の体力から計算したダメージ分を直接削る (setHealth)
                    float currentHealth = target.getHealth();
                    float nextHealth = currentHealth - totalSumDamage;

                    // 💡 死亡判定チェック
                    if (nextHealth <= 0f) {
                        target.setHealth(0f);
                        // 死亡時には、適切なDamageSource（今回は奈落ダメージ扱いとしてfellOutOfWorld）を添えてdie()を呼ぶ
                        target.die(target.damageSources().fellOutOfWorld());
                    } else {
                        // 生存しているならそのままHPを適用
                        target.setHealth(nextHealth);

                        // 💡 解決策：エンティティイベント（ステータスコード 2 = 被弾して赤くなるアニメーション）を周囲のクライアントへ送信
                        // これにより、protectedなmarkHurt()を呼ぶのと同じ、あるいはそれ以上の正確な被弾演出が発生します
                        target.level().broadcastEntityEvent(target, (byte) 2);
                    }

                    // バフを消去
                    attacker.removeEffect(nekotori_haru.more_iss.registry.ModEffects.OVERBURST.get());

                } finally {
                    IS_PROCESSING.set(false);
                }
            }
        }
    }
}