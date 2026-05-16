package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "more_iss")
public class SacrificialEdgeEvents {

    private static final ThreadLocal<Boolean> IS_UPDATING = ThreadLocal.withInitial(() -> false);
    private static final Random RANDOM = new Random();

    // ─── 既存ロジック：被弾時にカウントを蓄積 ───────────────────────────────────

    @SubscribeEvent
    public static void onSacrificialAbsorb(LivingDamageEvent event) {
        // エフェクトの再付与による無限ループを徹底防止
        if (IS_UPDATING.get()) return;

        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;

        // 対象が自傷デバフ「サクリフィシャル・ブリード」を所持している場合
        if (entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
            MobEffectInstance currentEffect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());
            if (currentEffect == null) return;

            // 被弾ダメージが0以下（完全に無効化されているなど）ならカウントしない
            if (event.getAmount() <= 0) return;

            try {
                IS_UPDATING.set(true);

                // 現在のアンプリファイア（＝これまでの累積自傷Hit数）を取得
                int currentHitCount = currentEffect.getAmplifier();
                int duration = currentEffect.getDuration();

                // 被弾を検知したので、純粋にカウントを+1する（ダメージの大小は無視）
                int newHitCount = currentHitCount + 1;

                // バニラのMobEffectアンプリファイアの限界上限（255）でストップさせる
                if (newHitCount > 255) newHitCount = 255;

                // 残り時間（無限状態）を維持したまま、カウント（アンプリファイア）を増やして上書き付与
                entity.addEffect(new MobEffectInstance(
                        ModEffects.SACRIFICIAL_BLEED.get(),
                        duration,
                        newHitCount, // 新しいHit数をアンプリファイアとして保存
                        currentEffect.isAmbient(),
                        currentEffect.isVisible(),
                        currentEffect.showIcon()
                ));

            } finally {
                IS_UPDATING.set(false);
            }
        }
    }

    // ─── 追加ロジック：死亡時に血液針をランダム射出 ───────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        Level world = entity.level();
        // クライアント側ではエンティティ生成などの処理を行わない
        if (world.isClientSide) return;

        // カスタムダメージソースのID判定、または対象が対象のエフェクトを持っているかチェック
        if (event.getSource().getMsgId().equals("sacrificial_purge") || entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
            MobEffectInstance effect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());

            if (effect != null) {
                int hitCount = effect.getAmplifier();

                // 💡 火力計算：蓄積されたカウント数（hitCount）× 3.5f をベース総火力とし、その5分の1を1本あたりに載せる
                // （最低保証として1本の威力が最低1.0fになるように設定）
                float baseDamage = (float) Math.max(5.0, (double) hitCount * 3.5);
                float needleDamage = baseDamage / 5.0f;

                // 血液針を5本、ランダムな3Dベクトルへ射出
                for (int i = 0; i < 5; i++) {
                    BloodNeedle needle = new BloodNeedle(world, entity);
                    needle.setDamage(needleDamage);

                    // 目の位置から少し上（胸〜頭付近）から破裂させる
                    Vec3 spawnPos = entity.getEyePosition().add(0, 0.5, 0);
                    needle.moveTo(spawnPos);

                    // 完全ランダムな3次元の向き（球面上の均等なランダムベクトル）を計算
                    float u = RANDOM.nextFloat();
                    float v = RANDOM.nextFloat();
                    float theta = u * 2.0f * (float) Math.PI;
                    float phi = (float) Math.acos(2.0f * v - 1.0f);

                    double x = Math.sin(phi) * Math.cos(theta);
                    double y = Math.sin(phi) * Math.sin(theta);
                    double z = Math.cos(phi);

                    Vec3 launchDirection = new Vec3(x, y, z).normalize();

                    // 針に速度と向きを与えてワールドにスポーン
                    needle.shoot(launchDirection);
                    world.addFreshEntity(needle);
                }
            }
        }
    }
}