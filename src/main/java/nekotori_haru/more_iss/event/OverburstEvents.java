package nekotori_haru.more_iss.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "more_iss")
public class OverburstEvents {

    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

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
                int totalDamagePercent = packedData / 10; // 上位桁：ダメージ％ (例: 300)
                int spellLevel = packedData % 10;         // 下位1桁：魔法レベル (1, 2, 3)

                float originalDamage = event.getAmount();

                try {
                    IS_PROCESSING.set(true);

                    // 💡 魔法クラスのUI計算と100%一致する総ダメージの計算
                    // ％表記が300％なら、元のダメージを3倍にする
                    float totalCalculatedDamage = originalDamage * (totalDamagePercent / 100.0f);

                    // 💡 魔法レベル（1, 2, 3）に応じて正確に 2割、4割、6割 を算出
                    float voidRatio = spellLevel * 0.2f;
                    if (voidRatio > 0.6f) voidRatio = 0.6f;

                    // 総ダメージを「奈落」と「通常物理」に分配
                    float voidDamage = totalCalculatedDamage * voidRatio;
                    float physicalDamage = totalCalculatedDamage * (1.0f - voidRatio);

                    // 無敵フレームを強制破壊
                    target.invulnerableTime = 0;

                    // 物理ダメージの適用
                    event.setAmount(physicalDamage);

                    // 奈落ダメージの適用（fellOutOfWorld）
                    target.hurt(target.damageSources().fellOutOfWorld(), voidDamage);

                    // バフを消去
                    attacker.removeEffect(nekotori_haru.more_iss.registry.ModEffects.OVERBURST.get());

                } finally {
                    IS_PROCESSING.set(false);
                }
            }
        }
    }
}