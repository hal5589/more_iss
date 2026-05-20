package nekotori_haru.more_iss.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import nekotori_haru.more_iss.spell.eldritch.DisintegrationSpell;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisintegrationState {

    private static final Map<UUID, DamagePhaseEntry> DAMAGE_PHASES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECOVERY_BLACKLIST = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenTargetEntry> FROZEN_TARGETS = new ConcurrentHashMap<>();

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, DisintegrationState::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DisintegrationState::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, DisintegrationState::onLivingHeal);
        initialized = true;
    }

    private static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;

        if (isDisintegrating(entity.getUUID())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    public static boolean isDisintegrating(UUID uuid) {
        return RECOVERY_BLACKLIST.containsKey(uuid);
    }

    public static void startCastingPhase(LivingEntity caster, LivingEntity initialTarget, double radius) {
        if (caster.level().isClientSide()) return;

        AABB searchArea = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = caster.level().getEntitiesOfClass(LivingEntity.class, searchArea);

        for (LivingEntity target : targets) {
            if (target.isDeadOrDying() || target.getUUID().equals(caster.getUUID())) continue;

            FROZEN_TARGETS.put(target.getUUID(), new FrozenTargetEntry(
                    caster.getUUID(),
                    target.getX(), target.getY(), target.getZ()
            ));
        }
    }

    public static void stopCastingPhase() {
        for (FrozenTargetEntry freeze : FROZEN_TARGETS.values()) {
            removeMultiLightBlock(freeze.lastLightPos);
        }
        FROZEN_TARGETS.clear();
    }

    public static void startDamagePhase(UUID targetUUID, UUID casterUUID, int maxTicks, float baseDamage, ServerLevel level) {
        int extendedTicks = 600;
        DamagePhaseEntry entry = new DamagePhaseEntry(casterUUID, extendedTicks, baseDamage, level);

        FrozenTargetEntry freeze = FROZEN_TARGETS.get(targetUUID);
        if (freeze != null) {
            entry.lastLightPos.copyFrom(freeze.lastLightPos);
        }
        FROZEN_TARGETS.remove(targetUUID);

        DAMAGE_PHASES.put(targetUUID, entry);
        DisintegrationTargetManager.lock(targetUUID, level.getEntity(targetUUID));

        RECOVERY_BLACKLIST.put(targetUUID, System.currentTimeMillis());
    }

    public static void stopDamagePhase(UUID targetUUID) {
        DamagePhaseEntry entry = DAMAGE_PHASES.get(targetUUID);
        if (entry != null) {
            removeMultiLightBlock(entry.lastLightPos);
        }
        DAMAGE_PHASES.remove(targetUUID);
        DisintegrationTargetManager.release(targetUUID);
        RECOVERY_BLACKLIST.remove(targetUUID);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        UUID uuid = entity.getUUID();

        FrozenTargetEntry freeze = FROZEN_TARGETS.get(uuid);
        if (freeze != null) removeMultiLightBlock(freeze.lastLightPos);
        FROZEN_TARGETS.remove(uuid);

        DamagePhaseEntry entry = DAMAGE_PHASES.get(uuid);
        if (entry != null) {
            if (entry.isEnding) return;
            entry.isEnding = true;

            ServerLevel level = (ServerLevel) entity.level();
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            stopDamagePhase(uuid);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        UUID uuid = entity.getUUID();
        ServerLevel level = (ServerLevel) entity.level();

        // ─────────────────────────────────────────────────────────────────
        // ── 1. 詠唱中フェーズ
        // ─────────────────────────────────────────────────────────────────
        if (FROZEN_TARGETS.containsKey(uuid)) {
            FrozenTargetEntry freeze = FROZEN_TARGETS.get(uuid);
            Entity caster = level.getEntity(freeze.casterUUID);

            if (caster instanceof LivingEntity livingCaster && !caster.isRemoved()) {

                if (freeze.currentHoverOffset < 1.0F) {
                    freeze.currentHoverOffset += 0.05F;
                    if (freeze.currentHoverOffset > 1.0F) freeze.currentHoverOffset = 1.0F;
                }
                double targetY = freeze.initialY + freeze.currentHoverOffset;

                entity.teleportTo(freeze.lockX, targetY, freeze.lockZ);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.fallDistance = 0.0F;

                if (entity instanceof Mob mob) {
                    mob.getNavigation().moveTo(freeze.lockX, targetY, freeze.lockZ, 0.0D);
                    mob.setTarget(livingCaster);
                }

                double dx = caster.getX() - entity.getX();
                double dy = (caster.getY() + caster.getEyeHeight()) - (entity.getY() + entity.getEyeHeight());
                double dz = caster.getZ() - entity.getZ();
                double xzLen = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                float pitch = (float) -(Math.atan2(dy, xzLen) * 180.0D / Math.PI);
                entity.setYRot(yaw); entity.setXRot(pitch); entity.setYHeadRot(yaw);
                entity.yRotO = yaw; entity.xRotO = pitch; entity.yBodyRot = yaw;

                updateMultiLightSource(level, freeze.lastLightPos, entity);

                long time = level.getGameTime();
                double baseGroundY = freeze.initialY;
                double currentEntityY = entity.getY();
                double headY = currentEntityY + entity.getBbHeight();
                double rotateAngle = time * 0.05;

                spawnGeometricGradientCircle(level, entity.getX(), baseGroundY + 0.1, entity.getZ(), 3.8, rotateAngle);
                spawnSparsePropertyRing(level, entity.getX(), currentEntityY + 0.2, entity.getZ(), 3.0, -rotateAngle, "SOUL_FLAME");
                spawnSparsePropertyRing(level, entity.getX(), currentEntityY + (entity.getBbHeight() / 2), entity.getZ(), 2.5, rotateAngle * 1.2, "FIRE_AND_SWEEP");
                spawnSparsePropertyRing(level, entity.getX(), headY + 0.3, entity.getZ(), 2.0, -rotateAngle * 0.8, "WHITE");
                spawnSparsePropertyRing(level, entity.getX(), headY + 2.0, entity.getZ(), 1.5, rotateAngle * 1.5, "SOUL_PARTICLE");
                spawnSparsePropertyRing(level, entity.getX(), headY + 4.0, entity.getZ(), 1.0, -rotateAngle * 2.0, "EXPLOSION");

            } else {
                removeMultiLightBlock(freeze.lastLightPos);
                FROZEN_TARGETS.remove(uuid);
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // ── 2. 発動中（ダメージ・昇華）フェーズ
        // ─────────────────────────────────────────────────────────────────
        DamagePhaseEntry entry = DAMAGE_PHASES.get(uuid);
        if (entry == null) return;

        // すでに終了処理（死亡・タイムアップ）が走っているなら重複動作を防ぐために完全スキップ
        if (entry.isEnding) return;

        if (entity.isRemoved() || entity.isDeadOrDying()) {
            entry.isEnding = true;
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            stopDamagePhase(uuid);
            return;
        }

        entry.elapsedTicks++;
        if (entry.elapsedTicks > entry.maxTicks) {
            // 🌟 タイムアップ（10秒経過）：無敵の「終焉の守護者」を強制リムーブするCルート処理
            entry.isEnding = true;

            // ① 最終爆発の演出パケットを周囲に飛ばす
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());

            // ② クライアント側へ死亡アニメーション（Status: 3）を強制送信して倒れる演出を再生
            level.getChunkSource().broadcastAndSend(entity, new ClientboundEntityEventPacket(entity, (byte) 3));

            // ③ サーバー側でHP計算やMODの無敵キャンセルを完全に無視して世界から強制排除（KILLED指定）
            entity.remove(Entity.RemovalReason.KILLED);

            stopDamagePhase(uuid);
            return;
        }

        updateMultiLightSource(level, entry.lastLightPos, entity);
        spawnMegaDisintegrationBeam(level, entity.getX(), entity.getY(), entity.getZ(), entry.elapsedTicks);
        spawnRandomVerticalSweepParticles(level, entity.getX(), entity.getY(), entity.getZ(), entity.getRandom());

        // 元の割合ダメージ加速ロジック（0.003f から毎tick 0.0003f ずつ増加）
        float basePercent = 0.003f;
        float growthPercent = 0.0003f;
        float currentPercent = basePercent + (entry.elapsedTicks * growthPercent);

        float damage = entity.getMaxHealth() * currentPercent;
        if (damage < 1.0f) damage = 1.0f;

        DamageSource source = buildSource(level, entry);

        // メインターゲットへ割合ダメージ
        DisintegrationDamageUtil.dealTrueDamage(entity, source, damage, true);

        // 周囲の巻き込み判定
        double radius = 3.0;
        AABB searchArea = entity.getBoundingBox().inflate(radius);
        List<LivingEntity> targetsInRange = level.getEntitiesOfClass(LivingEntity.class, searchArea);

        for (LivingEntity nearbyTarget : targetsInRange) {
            if (nearbyTarget.isDeadOrDying() || nearbyTarget.getUUID().equals(entry.casterUUID)) continue;

            UUID targetUUID = nearbyTarget.getUUID();
            RECOVERY_BLACKLIST.put(targetUUID, System.currentTimeMillis());

            float nearbyDamage = nearbyTarget.getMaxHealth() * currentPercent;
            if (nearbyDamage < 1.0f) nearbyDamage = 1.0f;

            DisintegrationDamageUtil.dealTrueDamage(nearbyTarget, source, nearbyDamage, true);
        }

        RECOVERY_BLACKLIST.keySet().removeIf(id -> {
            if (DAMAGE_PHASES.containsKey(id)) return false;
            Entity e = level.getEntity(id);
            return !(e instanceof LivingEntity le) || le.isDeadOrDying() || !targetsInRange.contains(le);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // ── 各種ギミック制御エンジン
    // ─────────────────────────────────────────────────────────────────

    private static void updateMultiLightSource(ServerLevel level, MultiLightLocation loc, LivingEntity entity) {
        BlockPos basePos = BlockPos.containing(entity.getX(), entity.getY() + 0.1, entity.getZ());
        BlockPos bodyPos = BlockPos.containing(entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
        BlockPos headPos = BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ());

        if (basePos.equals(loc.lastBase) && bodyPos.equals(loc.lastBody) && headPos.equals(loc.lastHead)) return;

        removeMultiLightBlock(loc);

        BlockState maxLightState = Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, 15);

        if (level.getBlockState(basePos).isAir() || level.getBlockState(basePos).is(Blocks.LIGHT)) {
            level.setBlock(basePos, maxLightState, 3);
            loc.lastBase = basePos;
        }
        if (level.getBlockState(bodyPos).isAir() || level.getBlockState(bodyPos).is(Blocks.LIGHT)) {
            level.setBlock(bodyPos, maxLightState, 3);
            loc.lastBody = bodyPos;
        }
        if (level.getBlockState(headPos).isAir() || level.getBlockState(headPos).is(Blocks.LIGHT)) {
            level.setBlock(headPos, maxLightState, 3);
            loc.lastHead = headPos;
        }

        loc.level = level;
    }

    private static void removeMultiLightBlock(MultiLightLocation loc) {
        if (loc == null || loc.level == null) return;

        if (loc.lastBase != null && loc.level.getBlockState(loc.lastBase).is(Blocks.LIGHT)) {
            loc.level.setBlock(loc.lastBase, Blocks.AIR.defaultBlockState(), 3);
        }
        if (loc.lastBody != null && loc.level.getBlockState(loc.lastBody).is(Blocks.LIGHT)) {
            loc.level.setBlock(loc.lastBody, Blocks.AIR.defaultBlockState(), 3);
        }
        if (loc.lastHead != null && loc.level.getBlockState(loc.lastHead).is(Blocks.LIGHT)) {
            loc.level.setBlock(loc.lastHead, Blocks.AIR.defaultBlockState(), 3);
        }

        loc.lastBase = null;
        loc.lastBody = null;
        loc.lastHead = null;
    }

    private static void spawnGeometricGradientCircle(ServerLevel level, double cx, double cy, double cz, double r, double angleOffset) {
        int points = 64;
        for (int i = 0; i < points; i++) {
            double angle = ((i * 2 * Math.PI) / points) + angleOffset;
            double x = cx + r * Math.cos(angle);
            double z = cz + r * Math.sin(angle);

            float factor = (float) ((Math.cos(angle) + 1.0) / 2.0);
            float red = factor;
            float green = 1.0f - Math.abs(factor - 0.5f) * 2.0f;
            float blue = 1.0f - factor;

            DustParticleOptions coloredDust = new DustParticleOptions(new Vector3f(red, green, blue), 1.2f);
            level.sendParticles(coloredDust, x, cy, z, 1, 0, 0, 0, 0);

            if (i % 8 == 0) {
                double lengthFactor = (double) (i % 32) / 32.0 * r;
                level.sendParticles(coloredDust, cx + lengthFactor * Math.cos(angleOffset), cy, cz + lengthFactor * Math.sin(angleOffset), 1, 0, 0, 0, 0);
                level.sendParticles(coloredDust, cx - lengthFactor * Math.cos(angleOffset), cy, cz - lengthFactor * Math.sin(angleOffset), 1, 0, 0, 0, 0);
            }
        }
    }

    private static void spawnSparsePropertyRing(ServerLevel level, double cx, double cy, double cz, double r, double angleOffset, String type) {
        int points = 32;
        for (int i = 0; i < points; i++) {
            if (i % 3 != 0) continue;

            double angle = ((i * 2 * Math.PI) / points) + angleOffset;
            double x = cx + r * Math.cos(angle);
            double z = cz + r * Math.sin(angle);

            switch (type) {
                case "SOUL_FLAME"    -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, cy, z, 1, 0, 0, 0, 0);
                case "FIRE_AND_SWEEP" -> {
                    level.sendParticles(ParticleTypes.FLAME, x, cy, z, 1, 0, 0, 0, 0);
                    if (i % 6 == 0) {
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, cy, z, 1, 0, 0, 0, 0);
                    }
                }
                case "WHITE"         -> level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 1, 0, 0, 0, 0);
                case "SOUL_PARTICLE" -> level.sendParticles(ParticleTypes.SOUL, x, cy, z, 1, 0, 0.05, 0, 0.01);
                case "EXPLOSION"     -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void spawnMegaDisintegrationBeam(ServerLevel level, double cx, double cy, double cz, int ticks) {
        double radius = 3.5;
        int basePoints = 48;
        double speedOffset = ticks * 0.3;

        for (int i = 0; i < basePoints; i++) {
            double angle = speedOffset + (i * (2 * Math.PI / basePoints));
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);

            level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 0, 0.0, 3.5, 0.0, 0.5);
            level.sendParticles(ParticleTypes.CRIT, x, cy, z, 0, 0.0, 3.2, 0.0, 0.5);

            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 0, 0.0, 2.5, 0.0, 0.6);
                level.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 0, 0.0, 2.5, 0.0, 0.6);
            }
        }
    }

    private static void spawnRandomVerticalSweepParticles(ServerLevel level, double cx, double cy, double cz, net.minecraft.util.RandomSource random) {
        int pillarCount = 2;

        for (int p = 0; p < pillarCount; p++) {
            double randomRadius = 1.5 + (random.nextDouble() * 1.0);
            double randomAngle = random.nextDouble() * 2.0 * Math.PI;

            double fixedX = cx + randomRadius * Math.cos(randomAngle);
            double fixedZ = cz + randomRadius * Math.sin(randomAngle);

            double step = 0.7;
            double maxHeight = 15.0;

            for (double h = 0; h < maxHeight; h += step) {
                if (random.nextFloat() > 0.75f) continue;

                double targetY = cy + h;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, fixedX, targetY, fixedZ, 0, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void triggerFinalExplosion(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 3, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 2, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 5, 1.0, 1.0, 1.0, 0.0);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 40, 2.0, 2.0, 2.0, 0.5);

        AABB shakeArea = new AABB(x, y, z, x, y, z).inflate(32.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, shakeArea);

        List<BlockPos> dummyDestroyedBlocks = new ArrayList<>();
        dummyDestroyedBlocks.add(BlockPos.containing(x, y, z));

        ClientboundExplodePacket shakePacket = new ClientboundExplodePacket(
                x, y, z,
                12.0F,
                dummyDestroyedBlocks,
                new Vec3(0, 0.6, 0)
        );

        for (ServerPlayer player : players) {
            player.connection.send(shakePacket);
            player.connection.send(new ClientboundAnimatePacket(player, 2));

            double shakeOffset = (player.getRandom().nextDouble() - 0.5) * 0.2;
            player.setDeltaMovement(player.getDeltaMovement().add(shakeOffset, 0.15, -shakeOffset));
            player.hurtMarked = true;
        }
    }

    private static DamageSource buildSource(ServerLevel level, DamagePhaseEntry entry) {
        var dmgTypeHolder = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DisintegrationSpell.DISINTEGRATION_DAMAGE_TYPE);

        Entity caster = level.getEntity(entry.casterUUID);
        return caster != null ? new DamageSource(dmgTypeHolder, caster) : new DamageSource(dmgTypeHolder);
    }

    // ─────────────────────────────────────────────────────────────────
    // ── 構造体定義
    // ─────────────────────────────────────────────────────────────────

    private static class MultiLightLocation {
        ServerLevel level = null;
        BlockPos lastBase = null;
        BlockPos lastBody = null;
        BlockPos lastHead = null;

        void copyFrom(MultiLightLocation other) {
            this.level = other.level;
            this.lastBase = other.lastBase;
            this.lastBody = other.lastBody;
            this.lastHead = other.lastHead;
        }
    }

    private static class DamagePhaseEntry {
        final UUID casterUUID;
        final int  maxTicks;
        final float baseDamage;
        final ServerLevel level;
        int elapsedTicks = 0;
        boolean isEnding = false; // 重複終了処理・ドロップ無限増殖を防ぐガードフラグ
        final MultiLightLocation lastLightPos = new MultiLightLocation();

        DamagePhaseEntry(UUID casterUUID, int maxTicks, float baseDamage, ServerLevel level) {
            this.casterUUID = casterUUID;
            this.maxTicks   = maxTicks;
            this.baseDamage = baseDamage;
            this.level      = level;
        }
    }

    private static class FrozenTargetEntry {
        final UUID casterUUID;
        final double lockX, initialY, lockZ;
        float currentHoverOffset = 0.0F;
        final MultiLightLocation lastLightPos = new MultiLightLocation();

        FrozenTargetEntry(UUID casterUUID, double lockX, double initialY, double lockZ) {
            this.casterUUID = casterUUID;
            this.lockX = lockX;
            this.initialY = initialY;
            this.lockZ = lockZ;
        }
    }
}