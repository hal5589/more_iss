package nekotori_haru.more_iss.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import nekotori_haru.more_iss.spell.eldritch.DisintegrationSpell;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisintegrationState {

    private static final Map<UUID, DamagePhaseEntry> DAMAGE_PHASES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> BLACKLIST = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenTargetEntry> FROZEN_TARGETS = new ConcurrentHashMap<>();

    private static Method actuallyHurtMethod = null;
    private static boolean initialized = false;

    static {
        try {
            // 🌟 1.20.1製品版の本名 m_6475_ を動的に取得
            actuallyHurtMethod = LivingEntity.class.getDeclaredMethod("m_6475_", DamageSource.class, float.class);
            actuallyHurtMethod.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static synchronized void init() {
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, DisintegrationState::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DisintegrationState::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, DisintegrationState::onLivingHeal);
        initialized = true;
    }

    public static boolean isDisintegrating(UUID uuid) {
        return BLACKLIST.containsKey(uuid) || DAMAGE_PHASES.containsKey(uuid);
    }

    public static void addToBlacklist(UUID uuid) {
        BLACKLIST.put(uuid, System.currentTimeMillis());
    }

    public static void removeFromBlacklist(UUID uuid) {
        BLACKLIST.remove(uuid);
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
        Entity targetEntity = level.getEntity(targetUUID);
        if (targetEntity == null) return;

        double lx = targetEntity.getX();
        double ly = targetEntity.getY();
        double lz = targetEntity.getZ();

        FrozenTargetEntry freeze = FROZEN_TARGETS.get(targetUUID);
        if (freeze != null) {
            lx = freeze.lockX;
            ly = freeze.initialY;
            lz = freeze.lockZ;
        }

        DamagePhaseEntry entry = new DamagePhaseEntry(casterUUID, maxTicks, baseDamage, level, lx, ly, lz);

        if (freeze != null) {
            entry.lastLightPos.copyFrom(freeze.lastLightPos);
            entry.currentHoverOffset = freeze.currentHoverOffset;
        }
        FROZEN_TARGETS.remove(targetUUID);

        DAMAGE_PHASES.put(targetUUID, entry);
        addToBlacklist(targetUUID);
    }

    public static void stopDamagePhase(UUID targetUUID) {
        DamagePhaseEntry entry = DAMAGE_PHASES.get(targetUUID);
        if (entry != null) {
            removeMultiLightBlock(entry.lastLightPos);
        }
        DAMAGE_PHASES.remove(targetUUID);
        removeFromBlacklist(targetUUID);
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

    private static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() != null && isDisintegrating(event.getEntity().getUUID())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        UUID uuid = entity.getUUID();
        ServerLevel level = (ServerLevel) entity.level();

        // ── 【演出A】 詠唱中：位置固定 ──
        if (FROZEN_TARGETS.containsKey(uuid)) {
            FrozenTargetEntry freeze = FROZEN_TARGETS.get(uuid);
            Entity caster = level.getEntity(freeze.casterUUID);

            if (caster instanceof LivingEntity livingCaster && !caster.isRemoved()) {
                if (freeze.currentHoverOffset < 1.0F) freeze.currentHoverOffset += 0.05F;
                double targetY = freeze.initialY + freeze.currentHoverOffset;

                entity.teleportTo(freeze.lockX, targetY, freeze.lockZ);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.fallDistance = 0.0F;

                if (entity instanceof Mob mob) {
                    mob.getNavigation().moveTo(freeze.lockX, targetY, freeze.lockZ, 0.0D);
                    mob.setTarget(livingCaster);
                }

                lockLookAtCaster(entity, caster);
                updateMultiLightSource(level, freeze.lastLightPos, entity);

                long time = level.getGameTime();
                double rotateAngle = time * 0.05;
                spawnGeometricGradientCircle(level, entity.getX(), freeze.initialY + 0.1, entity.getZ(), 3.8, rotateAngle);
                spawnSparsePropertyRing(level, entity.getX(), entity.getY() + 0.2, entity.getZ(), 3.0, -rotateAngle, "SOUL_FLAME");
                spawnSparsePropertyRing(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ(), 2.5, rotateAngle * 1.2, "FIRE_AND_SWEEP");
                spawnSparsePropertyRing(level, entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(), 2.0, -rotateAngle * 0.8, "WHITE");
            } else {
                removeMultiLightBlock(freeze.lastLightPos);
                FROZEN_TARGETS.remove(uuid);
            }
        }

        // ── 【演出B & ダメージ】 本発動：強力な持続位置固定 ──
        DamagePhaseEntry entry = DAMAGE_PHASES.get(uuid);
        if (entry == null || entry.isEnding) return;

        if (entity.isRemoved() || entity.isDeadOrDying()) {
            entry.isEnding = true;
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            stopDamagePhase(uuid);
            return;
        }

        // 本発動中も「絶対に」位置を完全固定（改良1）
        Entity caster = level.getEntity(entry.casterUUID);
        if (caster instanceof LivingEntity) {
            if (entry.currentHoverOffset < 1.0F) entry.currentHoverOffset += 0.05F;
            double targetY = entry.lockY + entry.currentHoverOffset;

            entity.teleportTo(entry.lockX, targetY, entry.lockZ);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0.0F;
            lockLookAtCaster(entity, caster);
        }

        entry.elapsedTicks++;

        // ⏰ 【タイムアップ時】処刑
        if (entry.elapsedTicks > entry.maxTicks) {
            entry.isEnding = true;
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            level.getChunkSource().broadcastAndSend(entity, new ClientboundEntityEventPacket(entity, (byte) 3));
            TrueHealthManipulator.forceSetTrueHealth(entity, 0.0f);
            executeTrueDeath(level, entity);
            stopDamagePhase(uuid);
            return;
        }

        updateMultiLightSource(level, entry.lastLightPos, entity);
        spawnMegaDisintegrationBeam(level, entity.getX(), entity.getY(), entity.getZ(), entry.elapsedTicks);

        // 📐 ダメージ計算
        float currentPercent = 0.003f + (entry.elapsedTicks * 0.0003f);
        float damageAmount = entity.getMaxHealth() * currentPercent;
        if (damageAmount < 1.0f) damageAmount = 1.0f;

        DamageSource source = buildSource(level, entry);

        // 無敵時間（iFrames）を毎チック強制粉砕（改良2）
        entity.invulnerableTime = 0;
        entity.hurtDuration = 0;

        // actuallyHurt を直接叩いて他MODの盾やHPロックを完全無視（改良3）
        if (actuallyHurtMethod != null) {
            try {
                actuallyHurtMethod.invoke(entity, source, damageAmount);
            } catch (Throwable t) {
                entity.hurt(source, damageAmount);
            }
        } else {
            entity.hurt(source, damageAmount);
        }

        // 周囲への拡散
        AABB searchArea = entity.getBoundingBox().inflate(3.0);
        List<LivingEntity> targetsInRange = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        for (LivingEntity nearby : targetsInRange) {
            if (nearby.isDeadOrDying() || nearby.getUUID().equals(entry.casterUUID) || nearby.getUUID().equals(uuid)) continue;

            nearby.invulnerableTime = 0;
            if (actuallyHurtMethod != null) {
                try { actuallyHurtMethod.invoke(nearby, source, nearby.getMaxHealth() * currentPercent); }
                catch (Throwable t) { nearby.hurt(source, nearby.getMaxHealth() * currentPercent); }
            } else {
                nearby.hurt(source, nearby.getMaxHealth() * currentPercent);
            }
        }
    }

    private static void lockLookAtCaster(LivingEntity entity, Entity caster) {
        double dx = caster.getX() - entity.getX();
        double dy = (caster.getY() + caster.getEyeHeight()) - (entity.getY() + entity.getEyeHeight());
        double dz = caster.getZ() - entity.getZ();
        double xzLen = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, xzLen) * 180.0D / Math.PI);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setYHeadRot(yaw);
    }

    public static void executeTrueDeath(ServerLevel level, LivingEntity entity) {
        boolean isFEInstalled = ModList.get().isLoaded("fantasy_ending");

        try {
            if (isFEInstalled) {
                String uuidStr = entity.getUUID().toString();
                var server = level.getServer();
                var source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity forceSetHealth " + uuidStr + " 0.0");
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity softGetHealthKill " + uuidStr);
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity forceKill " + uuidStr);
            } else {
                try {
                    java.lang.reflect.Field healthIdField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                    healthIdField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    EntityDataAccessor<Float> healthId = (EntityDataAccessor<Float>) healthIdField.get(null);

                    SynchedEntityDataUtil.forceSet(entity.getEntityData(), healthId, 0.0f);
                } catch (Throwable e) {
                    entity.setHealth(0.0f);
                }
            }

            forceDropItems(level, entity);
            entity.remove(Entity.RemovalReason.KILLED);

            // 🌟 修正：一般クラスになったAccessorを静的呼び出ししてメモリから抹消
            EntityTickList tickList = nekotori_haru.more_iss.mixin.ServerLevelAccessor.getEntityTickList(level);
            if (tickList != null) {
                int entityId = entity.getId();
                var activeMap = nekotori_haru.more_iss.mixin.EntityTickListAccessor.getActive(tickList);
                var passiveMap = nekotori_haru.more_iss.mixin.EntityTickListAccessor.getPassive(tickList);
                var iteratedMap = nekotori_haru.more_iss.mixin.EntityTickListAccessor.getIterated(tickList);

                if (activeMap != null) activeMap.remove(entityId);
                if (passiveMap != null) passiveMap.remove(entityId);
                if (iteratedMap != null) iteratedMap.remove(entityId);
            }

            try {
                java.lang.reflect.Field removalReasonField = Entity.class.getDeclaredField("removalReason");
                removalReasonField.setAccessible(true);
                removalReasonField.set(entity, Entity.RemovalReason.KILLED);
            } catch (Throwable e) {
                entity.discard();
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void forceDropItems(ServerLevel level, LivingEntity entity) {
        LootTable lootTable = level.getServer().getLootData().getLootTable(entity.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .create(LootContextParamSets.ENTITY);

        lootTable.getRandomItems(params).forEach(stack -> {
            if (!stack.isEmpty()) {
                ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
                level.addFreshEntity(item);
            }
        });
    }

    private static void updateMultiLightSource(ServerLevel level, MultiLightLocation loc, LivingEntity entity) {
        BlockPos basePos = BlockPos.containing(entity.getX(), entity.getY() + 0.1, entity.getZ());
        BlockPos bodyPos = BlockPos.containing(entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
        if (basePos.equals(loc.lastBase) && bodyPos.equals(loc.lastBody)) return;
        removeMultiLightBlock(loc);

        BlockState maxLightState = Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, 15);
        if (level.getBlockState(basePos).isAir()) { level.setBlock(basePos, maxLightState, 3); loc.lastBase = basePos; }
        if (level.getBlockState(bodyPos).isAir()) { level.setBlock(bodyPos, maxLightState, 3); loc.lastBody = bodyPos; }
        loc.level = level;
    }

    private static void removeMultiLightBlock(MultiLightLocation loc) {
        if (loc == null || loc.level == null) return;
        if (loc.lastBase != null && loc.level.getBlockState(loc.lastBase).is(Blocks.LIGHT)) loc.level.setBlock(loc.lastBase, Blocks.AIR.defaultBlockState(), 3);
        if (loc.lastBody != null && loc.level.getBlockState(loc.lastBody).is(Blocks.LIGHT)) loc.level.setBlock(loc.lastBody, Blocks.AIR.defaultBlockState(), 3);
        loc.lastBase = null; loc.lastBody = null;
    }

    private static void spawnGeometricGradientCircle(ServerLevel level, double cx, double cy, double cz, double r, double angleOffset) {
        int points = 64;
        for (int i = 0; i < points; i++) {
            double angle = ((i * 2 * Math.PI) / points) + angleOffset;
            double x = cx + r * Math.cos(angle); double z = cz + r * Math.sin(angle);
            float factor = (float) ((Math.cos(angle) + 1.0) / 2.0);
            DustParticleOptions coloredDust = new DustParticleOptions(new Vector3f(factor, 1.0f - Math.abs(factor - 0.5f) * 2.0f, 1.0f - factor), 1.2f);
            level.sendParticles(coloredDust, x, cy, z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSparsePropertyRing(ServerLevel level, double cx, double cy, double cz, double r, double angleOffset, String type) {
        int points = 32;
        for (int i = 0; i < points; i++) {
            if (i % 3 != 0) continue;
            double angle = ((i * 2 * Math.PI) / points) + angleOffset;
            double x = cx + r * Math.cos(angle); double z = cz + r * Math.sin(angle);
            switch (type) {
                case "SOUL_FLAME"    -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, cy, z, 1, 0, 0, 0, 0);
                case "FIRE_AND_SWEEP" -> level.sendParticles(ParticleTypes.FLAME, x, cy, z, 1, 0, 0, 0, 0);
                case "WHITE"         -> level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void spawnMegaDisintegrationBeam(ServerLevel level, double cx, double cy, double cz, int ticks) {
        double radius = 3.5; int basePoints = 48; double speedOffset = ticks * 0.3;
        for (int i = 0; i < basePoints; i++) {
            double angle = speedOffset + (i * (2 * Math.PI / basePoints));
            double x = cx + radius * Math.cos(angle); double z = cz + radius * Math.sin(angle);
            level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 0, 0.0, 3.5, 0.0, 0.5);
        }
    }

    private static void triggerFinalExplosion(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 3, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 5, 1.0, 1.0, 1.0, 0.0);

        AABB shakeArea = new AABB(x, y, z, x, y, z).inflate(32.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, shakeArea);
        ClientboundExplodePacket shakePacket = new ClientboundExplodePacket(x, y, z, 12.0F, new ArrayList<>(), new Vec3(0, 0.6, 0));
        for (ServerPlayer player : players) {
            player.connection.send(shakePacket);
            player.connection.send(new ClientboundAnimatePacket(player, 2));
        }
    }

    private static DamageSource buildSource(ServerLevel level, DamagePhaseEntry entry) {
        var damageTypeRegistry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var dmgTypeHolder = damageTypeRegistry.getHolderOrThrow(DisintegrationSpell.DISINTEGRATION_DAMAGE_TYPE);
        Entity caster = level.getEntity(entry.casterUUID);
        return caster != null ? new DamageSource(dmgTypeHolder, caster) : new DamageSource(dmgTypeHolder);
    }

    private static class MultiLightLocation {
        ServerLevel level = null; BlockPos lastBase = null; BlockPos lastBody = null;
        void copyFrom(MultiLightLocation other) { this.level = other.level; this.lastBase = other.lastBase; this.lastBody = other.lastBody; }
    }

    private static class DamagePhaseEntry {
        final UUID casterUUID; final int maxTicks; final float baseDamage; final ServerLevel level;
        final double lockX, lockY, lockZ;
        int elapsedTicks = 0; boolean isEnding = false;
        float currentHoverOffset = 0.0F;
        final MultiLightLocation lastLightPos = new MultiLightLocation();
        DamagePhaseEntry(UUID casterUUID, int maxTicks, float baseDamage, ServerLevel level, double lx, double ly, double lz) {
            this.casterUUID = casterUUID; this.maxTicks = maxTicks; this.baseDamage = baseDamage; this.level = level;
            this.lockX = lx; this.lockY = ly; this.lockZ = lz;
        }
    }

    private static class FrozenTargetEntry {
        final UUID casterUUID; final double lockX, initialY, lockZ; float currentHoverOffset = 0.0F;
        final MultiLightLocation lastLightPos = new MultiLightLocation();
        FrozenTargetEntry(UUID casterUUID, double lockX, double initialY, double lockZ) { this.casterUUID = casterUUID; this.lockX = lockX; this.initialY = initialY; this.lockZ = lockZ; }
    }
}