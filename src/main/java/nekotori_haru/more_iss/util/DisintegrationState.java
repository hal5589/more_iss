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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import nekotori_haru.more_iss.spell.synthesis.DisintegrationSpell;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisintegrationState {

    private static final Map<UUID, DamagePhaseEntry> DAMAGE_PHASES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECOVERY_BLACKLIST = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenTargetEntry> FROZEN_TARGETS = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> FORCED_DEATH_FLAGS = new ConcurrentHashMap<>();

    private static Method actuallyHurtMethod = null;
    private static Method dieMethod = null;
    private static Method absorbMethod = null;
    private static boolean initialized = false;

    static {
        try {
            actuallyHurtMethod = LivingEntity.class.getDeclaredMethod("m_6475_", DamageSource.class, float.class);
            actuallyHurtMethod.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            dieMethod = LivingEntity.class.getDeclaredMethod("m_6667_", DamageSource.class);
            dieMethod.setAccessible(true);
        } catch (Throwable e) {
            try {
                dieMethod = LivingEntity.class.getDeclaredMethod("die", DamageSource.class);
                dieMethod.setAccessible(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        try {
            absorbMethod = LivingEntity.class.getDeclaredMethod("m_6506_", DamageSource.class, float.class);
            absorbMethod.setAccessible(true);
        } catch (Throwable e) {
            try {
                absorbMethod = LivingEntity.class.getDeclaredMethod("getDamageAfterMagicAbsorb", DamageSource.class, float.class);
                absorbMethod.setAccessible(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static synchronized void init() {
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, DisintegrationState::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DisintegrationState::onLivingDeath);
        initialized = true;
    }

    public static boolean isDisintegrating(UUID uuid) {
        return RECOVERY_BLACKLIST.containsKey(uuid);
    }

    public static void setForcedDeath(UUID uuid, boolean forced) {
        if (forced) FORCED_DEATH_FLAGS.put(uuid, true);
        else FORCED_DEATH_FLAGS.remove(uuid);
    }

    public static boolean isForcedDeath(UUID uuid) {
        return FORCED_DEATH_FLAGS.getOrDefault(uuid, false);
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

        int extendedTicks = 600;

        double lx = targetEntity.getX();
        double ly = targetEntity.getY();
        double lz = targetEntity.getZ();

        FrozenTargetEntry freeze = FROZEN_TARGETS.get(targetUUID);
        if (freeze != null) {
            lx = freeze.lockX;
            ly = freeze.initialY;
            lz = freeze.lockZ;
        }

        DamagePhaseEntry entry = new DamagePhaseEntry(casterUUID, extendedTicks, baseDamage, level, lx, ly, lz);

        if (freeze != null) {
            entry.lastLightPos.copyFrom(freeze.lastLightPos);
            entry.currentHoverOffset = freeze.currentHoverOffset;
        }
        FROZEN_TARGETS.remove(targetUUID);

        DAMAGE_PHASES.put(targetUUID, entry);
        RECOVERY_BLACKLIST.put(targetUUID, System.currentTimeMillis());

        DisintegrationTargetManager.lock(targetUUID, targetEntity);
    }

    public static void stopDamagePhase(UUID targetUUID) {
        DamagePhaseEntry entry = DAMAGE_PHASES.get(targetUUID);
        if (entry != null) {
            removeMultiLightBlock(entry.lastLightPos);
        }
        DAMAGE_PHASES.remove(targetUUID);
        DisintegrationTargetManager.release(targetUUID);
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
            RECOVERY_BLACKLIST.remove(uuid);
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

                lockLookAtCaster(entity, caster);
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
        if (entry == null || entry.isEnding) return;

        if (entity.isRemoved() || entity.isDeadOrDying()) {
            entry.isEnding = true;
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            stopDamagePhase(uuid);
            RECOVERY_BLACKLIST.remove(uuid);
            return;
        }

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

        // ⏰ 【タイムアップ処刑フェーズ】
        if (entry.elapsedTicks > entry.maxTicks) {
            entry.isEnding = true;
            triggerFinalExplosion(level, entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
            level.getChunkSource().broadcastAndSend(entity, new ClientboundEntityEventPacket(entity, (byte) 3));

            TrueHealthManipulator.forceSetTrueHealth(entity, 0.0f);
            executeTrueDeath(level, entity);

            stopDamagePhase(uuid);
            RECOVERY_BLACKLIST.remove(uuid);
            return;
        }

        updateMultiLightSource(level, entry.lastLightPos, entity);
        spawnMegaDisintegrationBeam(level, entity.getX(), entity.getY(), entity.getZ(), entry.elapsedTicks);
        spawnRandomVerticalSweepParticles(level, entity.getX(), entity.getY(), entity.getZ(), entity.getRandom());

        float damageAmount = 10.0f + (entry.elapsedTicks * 3.0f);
        DamageSource source = buildSource(level, entry);

        entity.invulnerableTime = 0;
        entity.hurtDuration = 0;

        float previousHealth = entity.getHealth();
        boolean originalInvulnerable = entity.isInvulnerable();

        try {
            entity.setInvulnerable(false);

            if (absorbMethod != null) {
                try {
                    absorbMethod.invoke(entity, source, damageAmount);
                } catch (Throwable t) {
                    if (actuallyHurtMethod != null) {
                        try { actuallyHurtMethod.invoke(entity, source, damageAmount); }
                        catch (Throwable tt) { entity.hurt(source, damageAmount); }
                    } else {
                        entity.hurt(source, damageAmount);
                    }
                }
            } else {
                if (actuallyHurtMethod != null) {
                    try { actuallyHurtMethod.invoke(entity, source, damageAmount); }
                    catch (Throwable t) { entity.hurt(source, damageAmount); }
                } else {
                    entity.hurt(source, damageAmount);
                }
            }
        } finally {
            entity.setInvulnerable(originalInvulnerable);
        }

        if (entity.getHealth() >= previousHealth && !entity.isDeadOrDying()) {
            float targetHealth = Math.max(0.0f, previousHealth - damageAmount);
            entity.setHealth(targetHealth);
        }

        // 🌀 周囲へのスプラッシュ拡散処理
        double radius = 3.0;
        AABB searchArea = entity.getBoundingBox().inflate(radius);
        List<LivingEntity> targetsInRange = level.getEntitiesOfClass(LivingEntity.class, searchArea);

        for (LivingEntity nearbyTarget : targetsInRange) {
            if (nearbyTarget.isDeadOrDying() || nearbyTarget.getUUID().equals(entry.casterUUID) || nearbyTarget.getUUID().equals(uuid)) continue;

            UUID nearUUID = nearbyTarget.getUUID();
            RECOVERY_BLACKLIST.put(nearUUID, System.currentTimeMillis());

            nearbyTarget.invulnerableTime = 0;
            float nearPrevHealth = nearbyTarget.getHealth();

            if (absorbMethod != null) {
                try {
                    absorbMethod.invoke(nearbyTarget, source, damageAmount);
                } catch (Throwable t) {
                    if (actuallyHurtMethod != null) {
                        try { actuallyHurtMethod.invoke(nearbyTarget, source, damageAmount); }
                        catch (Throwable tt) { nearbyTarget.hurt(source, damageAmount); }
                    } else {
                        nearbyTarget.hurt(source, damageAmount);
                    }
                }
            } else {
                if (actuallyHurtMethod != null) {
                    try { actuallyHurtMethod.invoke(nearbyTarget, source, damageAmount); }
                    catch (Throwable t) { nearbyTarget.hurt(source, damageAmount); }
                } else {
                    nearbyTarget.hurt(source, damageAmount);
                }
            }

            if (nearbyTarget.getHealth() >= nearPrevHealth && !nearbyTarget.isDeadOrDying()) {
                nearbyTarget.setHealth(Math.max(0.0f, nearPrevHealth - damageAmount));
            }
        }

        RECOVERY_BLACKLIST.keySet().removeIf(id -> {
            Entity e = level.getEntity(id);
            return !(e instanceof LivingEntity le) || le.isDeadOrDying() || !targetsInRange.contains(le);
        });
    }

    private static void lockLookAtCaster(LivingEntity entity, Entity caster) {
        double dx = caster.getX() - entity.getX();
        double dy = (caster.getY() + caster.getEyeHeight()) - (entity.getY() + entity.getEyeHeight());
        double dz = caster.getZ() - entity.getZ();
        double xzLen = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, xzLen) * 180.0D / Math.PI);
        entity.setYRot(yaw); entity.setXRot(pitch); entity.setYHeadRot(yaw);
        entity.yRotO = yaw; entity.xRotO = pitch; entity.yBodyRot = yaw;
    }

    private static DamageSource buildSource(ServerLevel level, DamagePhaseEntry entry) {
        var dmgTypeHolder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DisintegrationSpell.DISINTEGRATION_DAMAGE_TYPE);
        Entity caster = level.getEntity(entry.casterUUID);
        return caster != null ? new DamageSource(dmgTypeHolder, caster) : new DamageSource(dmgTypeHolder);
    }

    // 🌟 確殺プログラム本体（デバッグログ・生存猶予検証版）
    public static void executeTrueDeath(ServerLevel level, LivingEntity entity) {
        UUID uuid = entity.getUUID();
        setForcedDeath(uuid, true);

        System.out.println("[MoreIss Debug] === 確殺プログラム開始: " + entity.getType().getDescriptionId() + " ===");

        try {
            DamagePhaseEntry entry = DAMAGE_PHASES.get(uuid);
            Entity caster = entry != null ? level.getEntity(entry.casterUUID) : null;

            DamageSource killSource;
            if (caster instanceof Player player) {
                killSource = level.damageSources().playerAttack(player);
                System.out.println("[MoreIss Debug] プレイヤーキルソースを割り当てました: " + player.getName().getString());
            } else {
                killSource = buildSource(level, entry);
                System.out.println("[MoreIss Debug] 通常の魔法ダメージキルソースを割り当てました");
            }

            entity.getCombatTracker().recheckStatus();

            // Forge 死亡イベント発火
            LivingDeathEvent deathEvent = new LivingDeathEvent(entity, killSource);
            boolean deathEventCanceled = MinecraftForge.EVENT_BUS.post(deathEvent);
            System.out.println("[MoreIss Debug] LivingDeathEvent発火。キャンセル状態: " + deathEventCanceled);

            // dieメソッドの実行
            if (dieMethod != null) {
                try {
                    dieMethod.invoke(entity, killSource);
                    System.out.println("[MoreIss Debug] リフレクション経由で die(DamageSource) の呼び出しに成功しました");
                } catch (Throwable t) {
                    System.out.println("[MoreIss Debug] リフレクションによる die 呼び出しに失敗したため、ダイレクト呼び出しを試みます");
                    entity.die(killSource);
                }
            } else {
                entity.die(killSource);
                System.out.println("[MoreIss Debug] 通常の entity.die(DamageSource) を実行しました");
            }

            // Forge ドロップイベント手動補強
            Collection<ItemEntity> capturedDrops = new ArrayList<>();
            LivingDropsEvent dropsEvent = new LivingDropsEvent(entity, killSource, capturedDrops, 0, true);
            boolean dropsEventCanceled = MinecraftForge.EVENT_BUS.post(dropsEvent);
            System.out.println("[MoreIss Debug] LivingDropsEvent発火。キャンセル状態: " + dropsEventCanceled + "、キャプチャ数: " + dropsEvent.getDrops().size());

            if (!dropsEventCanceled) {
                for (ItemEntity dropItem : dropsEvent.getDrops()) {
                    if (dropItem != null && !dropItem.getItem().isEmpty()) {
                        boolean spawned = level.addFreshEntity(dropItem);
                        System.out.println("[MoreIss Debug] CapturedDrop スポーン試行: " + dropItem.getItem().getHoverName().getString() + " -> 結果: " + spawned);
                    }
                }
            }

            // 三重保険：バニラルートテーブルのダイレクトドロップ生成
            forceDropItems(level, entity, killSource);

            // HP完全ロック（生存フラグ全消去）
            entity.setHealth(0.0f);

            boolean isFEInstalled = ModList.get().isLoaded("fantasy_ending");
            if (isFEInstalled) {
                String uuidStr = uuid.toString();
                var server = level.getServer();
                var source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity forceSetHealth " + uuidStr + " 0.0");
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity softGetHealthKill " + uuidStr);
                server.getCommands().performPrefixedCommand(source, "fantasy_ending entity forceKill " + uuidStr);
                System.out.println("[MoreIss Debug] fantasy_ending の強制排除コマンド群を実行しました");
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

            // 🚨 【検証】即座に removalReason を流し込んでエンティティリストから消し去るロジックをコメントアウト
            // これにより、マイクラ本来の自然なデスルーチン（死亡アニメーション等）でアイテムを登録する時間を確保します
            System.out.println("[MoreIss Debug] 即時エンティティ抹消(remove)を意図的にスキップしました（自然消滅のテスト）");

            /*
            entity.remove(Entity.RemovalReason.KILLED);

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
            */

        } catch (Throwable t) {
            System.out.println("[MoreIss Debug] 確殺ロジック実行中に例外が発生しました");
            t.printStackTrace();
        } finally {
            setForcedDeath(uuid, false);
            System.out.println("[MoreIss Debug] === 確殺プログラム処理エンド ===");
        }
    }

    private static void forceDropItems(ServerLevel level, LivingEntity entity, DamageSource directSource) {
        try {
            if (entity.getLootTable() == null) {
                System.out.println("[MoreIss Debug] forceDropItems: このエンティティのLootTableはnullです");
                return;
            }
            System.out.println("[MoreIss Debug] forceDropItems: ダイレクトルートテーブル読込開始 -> " + entity.getLootTable().toString());

            LootTable lootTable = level.getServer().getLootData().getLootTable(entity.getLootTable());
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, directSource)
                    .create(LootContextParamSets.ENTITY);

            lootTable.getRandomItems(params).forEach(stack -> {
                if (stack != null && !stack.isEmpty()) {
                    ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
                    item.setNoPickUpDelay();
                    boolean spawned = level.addFreshEntity(item);
                    System.out.println("[MoreIss Debug] 直生成ルートテーブルドロップ: " + stack.getHoverName().getString() + " x" + stack.getCount() + " -> ワールド登録成功: " + spawned);
                }
            });
        } catch (Throwable t) {
            System.out.println("[MoreIss Debug] forceDropItems内で例外が発生しました");
            t.printStackTrace();
        }
    }

    private static void updateMultiLightSource(ServerLevel level, MultiLightLocation loc, LivingEntity entity) {
        BlockPos basePos = BlockPos.containing(entity.getX(), entity.getY() + 0.1, entity.getZ());
        BlockPos bodyPos = BlockPos.containing(entity.getX(), entity.getY() + (entity.getBbHeight() / 2), entity.getZ());
        BlockPos headPos = BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ());

        if (basePos.equals(loc.lastBase) && bodyPos.equals(loc.lastBody) && headPos.equals(loc.lastHead)) return;
        removeMultiLightBlock(loc);

        BlockState maxLightState = Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, 15);

        if (level.getBlockState(basePos).isAir() || level.getBlockState(basePos).is(Blocks.LIGHT)) {
            level.setBlock(basePos, maxLightState, 3); loc.lastBase = basePos;
        }
        if (level.getBlockState(bodyPos).isAir() || level.getBlockState(bodyPos).is(Blocks.LIGHT)) {
            level.setBlock(bodyPos, maxLightState, 3); loc.lastBody = bodyPos;
        }
        if (level.getBlockState(headPos).isAir() || level.getBlockState(headPos).is(Blocks.LIGHT)) {
            level.setBlock(headPos, maxLightState, 3); loc.lastHead = headPos;
        }
        loc.level = level;
    }

    private static void removeMultiLightBlock(MultiLightLocation loc) {
        if (loc == null || loc.level == null) return;
        if (loc.lastBase != null && loc.level.getBlockState(loc.lastBase).is(Blocks.LIGHT)) loc.level.setBlock(loc.lastBase, Blocks.AIR.defaultBlockState(), 3);
        if (loc.lastBody != null && loc.level.getBlockState(loc.lastBody).is(Blocks.LIGHT)) loc.level.setBlock(loc.lastBody, Blocks.AIR.defaultBlockState(), 3);
        if (loc.lastHead != null && loc.level.getBlockState(loc.lastHead).is(Blocks.LIGHT)) loc.level.setBlock(loc.lastHead, Blocks.AIR.defaultBlockState(), 3);
        loc.lastBase = null; loc.lastBody = null; loc.lastHead = null;
    }

    private static void spawnGeometricGradientCircle(ServerLevel level, double cx, double cy, double cz, double r, double angleOffset) {
        int points = 64;
        for (int i = 0; i < points; i++) {
            double angle = ((i * 2 * Math.PI) / points) + angleOffset;
            double x = cx + r * Math.cos(angle); double z = cz + r * Math.sin(angle);

            float factor = (float) ((Math.cos(angle) + 1.0) / 2.0);
            DustParticleOptions coloredDust = new DustParticleOptions(new Vector3f(factor, 1.0f - Math.abs(factor - 0.5f) * 2.0f, 1.0f - factor), 1.2f);
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
            double x = cx + r * Math.cos(angle); double z = cz + r * Math.sin(angle);

            switch (type) {
                case "SOUL_FLAME"    -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, cy, z, 1, 0, 0, 0, 0);
                case "FIRE_AND_SWEEP" -> {
                    level.sendParticles(ParticleTypes.FLAME, x, cy, z, 1, 0, 0, 0, 0);
                    if (i % 6 == 0) level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, cy, z, 1, 0, 0, 0, 0);
                }
                case "WHITE"         -> level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 1, 0, 0, 0, 0);
                case "SOUL_PARTICLE" -> level.sendParticles(ParticleTypes.SOUL, x, cy, z, 1, 0, 0.05, 0, 0.01);
                case "EXPLOSION"     -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void spawnMegaDisintegrationBeam(ServerLevel level, double cx, double cy, double cz, int ticks) {
        double radius = 3.5; int basePoints = 48; double speedOffset = ticks * 0.3;
        for (int i = 0; i < basePoints; i++) {
            double angle = speedOffset + (i * (2 * Math.PI / basePoints));
            double x = cx + radius * Math.cos(angle); double z = cz + radius * Math.sin(angle);

            level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 0, 0.0, 3.5, 0.0, 0.5);
            level.sendParticles(ParticleTypes.CRIT, x, cy, z, 0, 0.0, 3.2, 0.0, 0.5);

            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 0, 0.0, 2.5, 0.0, 0.6);
                level.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 0, 0.0, 2.5, 0.0, 0.6);
            }
        }
    }

    private static void spawnRandomVerticalSweepParticles(ServerLevel level, double cx, double cy, double cz, net.minecraft.util.RandomSource random) {
        int pillarCount = 3;
        for (int p = 0; p < pillarCount; p++) {
            double randomRadius = 1.5 + (random.nextDouble() * 1.0);
            double randomAngle = random.nextDouble() * 2.0 * Math.PI;
            double fixedX = cx + randomRadius * Math.cos(randomAngle);
            double fixedZ = cz + randomRadius * Math.sin(randomAngle);

            double step = 0.4; double maxHeight = 15.0;
            for (double h = 0; h < maxHeight; h += step) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, fixedX, cy + h, fixedZ, 1, 0.0, 0.0, 0.0, 0.0);
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

        ClientboundExplodePacket shakePacket = new ClientboundExplodePacket(x, y, z, 12.0F, dummyDestroyedBlocks, new Vec3(0, 0.6, 0));

        for (ServerPlayer player : players) {
            player.connection.send(shakePacket);
            player.connection.send(new ClientboundAnimatePacket(player, 2));
            double shakeOffset = (player.getRandom().nextDouble() - 0.5) * 0.2;
            player.setDeltaMovement(player.getDeltaMovement().add(shakeOffset, 0.15, -shakeOffset));
            player.hurtMarked = true;
        }
    }

    private static class MultiLightLocation {
        ServerLevel level = null; BlockPos lastBase = null; BlockPos lastBody = null; BlockPos lastHead = null;
        void copyFrom(MultiLightLocation other) { this.level = other.level; this.lastBase = other.lastBase; this.lastBody = other.lastBody; this.lastHead = other.lastHead; }
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