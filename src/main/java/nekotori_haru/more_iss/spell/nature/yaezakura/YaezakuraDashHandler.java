package nekotori_haru.more_iss.spell.nature.yaezakura;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModParticles;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class YaezakuraDashHandler {

    private static final Map<UUID, DelayedDamageTask> ACTIVE_TASKS = new ConcurrentHashMap<>();

    public static void scheduleDelayedDamage(LivingEntity caster, AABB box, float damagePerHit,
                                             int initialDelay, int interval, int hitCount,
                                             Vec3 startPos, Vec3 endPos) {
        if (caster.level().isClientSide) return;
        UUID taskId = UUID.randomUUID();
        ACTIVE_TASKS.put(taskId, new DelayedDamageTask(
                caster, box, damagePerHit, initialDelay, interval, hitCount,
                startPos, endPos
        ));
        More_iss.LOGGER.info("Scheduled delayed damage: id={}, hits={}", taskId, hitCount);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE_TASKS.isEmpty()) return;

        Iterator<Map.Entry<UUID, DelayedDamageTask>> it = ACTIVE_TASKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DelayedDamageTask> entry = it.next();
            DelayedDamageTask task = entry.getValue();

            if (task.caster == null || !task.caster.isAlive()) {
                it.remove();
                continue;
            }

            task.tickCount++;

            if (task.tickCount >= task.initialDelay) {
                int elapsed = task.tickCount - task.initialDelay;
                if (elapsed % task.interval == 0 && task.hitsDone < task.hitCount) {
                    applyDamage(task);
                    task.hitsDone++;
                }
                if (task.hitsDone >= task.hitCount) {
                    it.remove();
                }
            }
        }
    }

    private static void applyDamage(DelayedDamageTask task) {
        Level level = task.caster.level();
        if (!(level instanceof ServerLevel server)) return;

        DamageSource damageSource;
        if (task.caster instanceof Player player) {
            damageSource = task.caster.damageSources().playerAttack(player);
        } else {
            damageSource = task.caster.damageSources().mobAttack(task.caster);
        }

        // 経路上に等間隔で斬撃パーティクルを発生させる
        spawnSlashParticlesAlongPath(server, task.startPos, task.endPos, task.caster);

        var entities = level.getEntitiesOfClass(LivingEntity.class, task.box,
                e -> e != task.caster && Utils.hasLineOfSight(level, task.caster, e, true));

        for (LivingEntity target : entities) {
            target.invulnerableTime = 0;
            target.hurt(damageSource, task.damagePerHit);

            Vec3 targetHead = target.position().add(0, target.getBbHeight(), 0);
            spawnCherryBurst(server, targetHead);
        }
    }

    // 経路上に等間隔（0.5ブロック間隔）で斬撃パーティクルをスポーン
    private static void spawnSlashParticlesAlongPath(ServerLevel level, Vec3 start, Vec3 end, LivingEntity caster) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.1) return;
        Vec3 step = dir.normalize().scale(0.5);

        Vec3 forward = new Vec3(caster.getLookAngle().x, 0, caster.getLookAngle().z).normalize();

        for (double d = 0; d < length; d += 0.5) {
            Vec3 pos = start.add(step.scale(d / 0.5));

            double angleDeg = 30 + caster.getRandom().nextDouble() * 30;
            double angleRad = Math.toRadians(angleDeg);
            double cos = Math.cos(angleRad);
            double sin = Math.sin(angleRad);
            Vec3 dirVec = new Vec3(
                    forward.x * cos - forward.z * sin,
                    0,
                    forward.x * sin + forward.z * cos
            ).normalize();

            double speed = 0.6 + caster.getRandom().nextDouble() * 0.4;
            Vec3 velocity = new Vec3(
                    dirVec.x * speed,
                    (caster.getRandom().nextDouble() - 0.2) * 0.3,
                    dirVec.z * speed
            );

            double yOff = (caster.getRandom().nextDouble() - 0.5) * 0.5;
            Vec3 spawnPos = pos.add(0, caster.getBbHeight() + yOff, 0);

            level.sendParticles(
                    ModParticles.YAEZAKURA_SLASH.get(),
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    0,           // ← count=0 で velocity を正しく使用
                    velocity.x, velocity.y, velocity.z,
                    0
            );
        }
    }

    // 桜の花びら炸裂
    private static void spawnCherryBurst(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 8; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.2 + level.random.nextDouble() * 0.4;
            Vec3 vel = new Vec3(
                    Math.cos(angle) * Math.cos(pitch) * speed,
                    Math.sin(pitch) * speed,
                    Math.sin(angle) * Math.cos(pitch) * speed
            );
            level.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    pos.x, pos.y, pos.z,
                    1,
                    vel.x, vel.y, vel.z,
                    0
            );
        }
    }

    // ---- 内部クラス ----
    private static class DelayedDamageTask {
        final LivingEntity caster;
        final AABB box;
        final float damagePerHit;
        final int initialDelay;
        final int interval;
        final int hitCount;
        final Vec3 startPos;
        final Vec3 endPos;
        int tickCount = 0;
        int hitsDone = 0;

        DelayedDamageTask(LivingEntity caster, AABB box, float damagePerHit,
                          int initialDelay, int interval, int hitCount,
                          Vec3 startPos, Vec3 endPos) {
            this.caster = caster;
            this.box = box;
            this.damagePerHit = damagePerHit;
            this.initialDelay = initialDelay;
            this.interval = interval;
            this.hitCount = hitCount;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }
}