package nekotori_haru.more_iss.util;

import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 「静止→1本ずつ順番に発射」を実現するスケジューラ。
 *
 * ─ 設計方針 ─────────────────────────────────────────
 * LightningLanceProjectile は独自の tick() で毎フレーム移動するため、
 * ワールドに追加した後に速度ゼロを維持することが難しい。
 *
 * そこで静止フェーズ中はエンティティをワールドに追加せず、
 * スポーン座標とパラメータだけ LanceBlueprint として保持し、
 * 発射タイミングになって初めて生成 & addFreshEntity する。
 * ─────────────────────────────────────────────────────
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DelayedLanceScheduler {

    private static final List<LanceJob> JOBS = new ArrayList<>();

    /**
     * ランス群をスケジュール登録する。
     *
     * @param level         サーバーワールド
     * @param caster        詠唱者（ランスの owner として使用）
     * @param blueprints    スポーン情報リスト（インデックス順に発射）
     * @param target        ホーミング先（null 可）
     * @param hoverTicks    静止時間（tick）。この間はパーティクルで演出
     * @param intervalTicks 1本ずつ発射する間隔（tick）
     * @param damage        ランスのダメージ量
     */
    public static void schedule(ServerLevel level,
                                LivingEntity caster,
                                List<LanceBlueprint> blueprints,
                                LivingEntity target,
                                int hoverTicks,
                                int intervalTicks,
                                float damage) {
        JOBS.add(new LanceJob(level, caster, blueprints, target,
                hoverTicks, intervalTicks, damage));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<LanceJob> iter = JOBS.iterator();
        while (iter.hasNext()) {
            LanceJob job = iter.next();
            job.tick();
            if (job.isDone()) {
                iter.remove();
            }
        }
    }

    // ─────────────────────────────────────────────────
    // 内部クラス
    // ─────────────────────────────────────────────────

    private static class LanceJob {
        private final ServerLevel level;
        private final LivingEntity caster;
        private final List<LanceBlueprint> blueprints;
        private final LivingEntity target;
        private final int hoverTicks;
        private final int intervalTicks;
        private final float damage;

        private int tickCounter = 0;
        private int nextIndex = 0;

        LanceJob(ServerLevel level, LivingEntity caster, List<LanceBlueprint> blueprints,
                 LivingEntity target, int hoverTicks, int intervalTicks, float damage) {
            this.level = level;
            this.caster = caster;
            this.blueprints = blueprints;
            this.target = target;
            this.hoverTicks = hoverTicks;
            this.intervalTicks = intervalTicks;
            this.damage = damage;
        }

        void tick() {
            tickCounter++;

            // ── パーティクル演出: 静止中は全座標、発射中は未発射分のみ継続 ──
            // 発射済み(nextIndex未満)はランス本体が飛んでいるので不要
            if (tickCounter % 3 == 0) {
                int particleStart = (tickCounter <= hoverTicks) ? 0 : nextIndex;
                for (int i = particleStart; i < blueprints.size(); i++) {
                    Vec3 p = blueprints.get(i).spawnPos();
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            p.x, p.y, p.z, 4, 0.15, 0.15, 0.15, 0.02);
                }
            }

            // ── 静止フェーズ中は発射しない ──
            if (tickCounter <= hoverTicks) return;

            // ── 発射フェーズ: intervalTicks ごとに1本ずつ ──
            int elapsed = tickCounter - hoverTicks;
            int shouldFire = (elapsed - 1) / intervalTicks + 1;

            while (nextIndex < shouldFire && nextIndex < blueprints.size()) {
                spawnAndShoot(blueprints.get(nextIndex));
                nextIndex++;
            }
        }

        /** ランスをその場で生成して即発射 */
        private void spawnAndShoot(LanceBlueprint bp) {
            LightningLanceProjectile lance = new LightningLanceProjectile(level, caster);
            Vec3 pos = bp.spawnPos();
            lance.setPos(pos.x, pos.y, pos.z);
            lance.setDamage(damage);

            Vec3 direction;
            if (target != null && target.isAlive()) {
                Vec3 to = target.getBoundingBox().getCenter();
                direction = to.subtract(pos).normalize();
                lance.setHomingTarget(target);
            } else {
                direction = bp.fallbackDir();
            }

            lance.shoot(direction.scale(1.2));
            level.addFreshEntity(lance);

            // 発射時のパーティクルバースト
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.1);
        }

        boolean isDone() {
            return nextIndex >= blueprints.size();
        }
    }

    /**
     * ランス1本分のスポーン情報（エンティティは持たない）。
     *
     * @param spawnPos    スポーン・発射起点の座標
     * @param fallbackDir ターゲット不在時の発射方向
     */
    public record LanceBlueprint(Vec3 spawnPos, Vec3 fallbackDir) {}
}