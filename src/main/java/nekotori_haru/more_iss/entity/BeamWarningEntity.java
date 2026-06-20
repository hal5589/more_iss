package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import nekotori_haru.more_iss.api.BeamType;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.ModEntities;
import nekotori_haru.more_iss.util.AllyUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 予告線エンティティ。
 *
 * 見た目は開始点(頭上の発射点)から固定方向(直下)へdistance分だけ伸びる細い棒。
 * WARNING_DURATION(20tick)経過すると、自身のtick()内で本体ビーム(BaseBeamVisualEntity
 * 表示 + ダメージ + ヒット効果)を発生させてから消滅する。
 *
 * 詠唱者やダメージ量、ビーム種別はコンストラクタで受け取り、自身の中で保持する。
 * これにより、外部の遅延実行APIに依存せず、エンティティのtickだけで
 * 「予告線表示 → 一定時間後に本体発生」の流れを完結させる。
 */
public class BeamWarningEntity extends Entity {

    // ⭐ 予告線の表示時間。経過後に本体ビームが発生する
    public static final int WARNING_DURATION = 20;

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(BeamWarningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_BEAM_TYPE =
            SynchedEntityData.defineId(BeamWarningEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(BeamWarningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Y =
            SynchedEntityData.defineId(BeamWarningEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(BeamWarningEntity.class, EntityDataSerializers.FLOAT);

    private double length = 0.0;
    private BeamType beamType = BeamType.FLAME;
    private Vec3 direction = new Vec3(0, -1, 0);

    // サーバー側のみで使う、本体発火に必要な情報(クライアントには同期しない)
    @Nullable
    private LivingEntity caster;
    private float damage = 0f;
    private int spellLevel = 1;
    private boolean firedBeam = false;

    public BeamWarningEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /**
     * @param startPos 予告線の開始点（本体レイの発射位置と一致させる）
     * @param direction 予告線が伸びる方向（正規化済みであることを想定。今回は常に真下 (0,-1,0)）
     * @param length 予告線の長さ（本体レイのdistanceと一致させる）
     * @param damage 本体発火時に適用するダメージ量
     * @param spellLevel ヒット効果の強さ計算に使うスペルレベル
     */
    public BeamWarningEntity(EntityType<?> type, Level level, LivingEntity caster, Vec3 startPos,
                             Vec3 direction, double length, BeamType beamType, float damage, int spellLevel) {
        this(type, level);
        this.caster = caster;
        this.length = length;
        this.beamType = beamType;
        this.direction = direction.normalize();
        this.damage = damage;
        this.spellLevel = spellLevel;

        this.setPos(startPos.x, startPos.y, startPos.z);

        this.entityData.set(DATA_LENGTH, (float) length);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());
        this.entityData.set(DATA_DIR_X, (float) this.direction.x);
        this.entityData.set(DATA_DIR_Y, (float) this.direction.y);
        this.entityData.set(DATA_DIR_Z, (float) this.direction.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LENGTH, 0.0f);
        this.entityData.define(DATA_BEAM_TYPE, BeamType.FLAME.name());
        this.entityData.define(DATA_DIR_X, 0.0f);
        this.entityData.define(DATA_DIR_Y, -1.0f);
        this.entityData.define(DATA_DIR_Z, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.length = this.entityData.get(DATA_LENGTH);
            try {
                this.beamType = BeamType.valueOf(this.entityData.get(DATA_BEAM_TYPE));
            } catch (IllegalArgumentException e) {
                this.beamType = BeamType.FLAME;
            }
            this.direction = new Vec3(
                    this.entityData.get(DATA_DIR_X),
                    this.entityData.get(DATA_DIR_Y),
                    this.entityData.get(DATA_DIR_Z)
            );
            return;
        }

        // サーバー側: 寿命が来たら本体ビームを発火してから消滅
        if (this.tickCount >= WARNING_DURATION && !this.firedBeam) {
            this.firedBeam = true;
            fireBeam();
            this.discard();
        }
    }

    /**
     * 本体ビームの発火処理。BaseBeamVisualEntityの生成、進路上の最初の有効な
     * ターゲットへのダメージ適用、ビーム種別ごとの専用ヒット効果の再現を行う。
     */
    private void fireBeam() {
        if (caster == null || !caster.isAlive()) return;
        Level level = this.level();

        Vec3 startPos = this.position();
        Vec3 endPos = startPos.add(direction.scale(length));

        BaseBeamVisualEntity visual = new BaseBeamVisualEntity(
                ModEntities.BASE_BEAM_VISUAL.get(), level, caster, length, beamType);
        visual.setPos(startPos.x, startPos.y, startPos.z);
        visual.setXRot(90.0f); // 真下を向くピッチ
        visual.setYRot(0.0f);
        level.addFreshEntity(visual);

        LivingEntity hitTarget = findFirstValidTarget(level, startPos, endPos);

        Vec3 particlePos = hitTarget != null
                ? hitTarget.position().add(0, hitTarget.getBbHeight() / 2, 0)
                : endPos;
        spawnImpactParticles(level, particlePos);

        if (hitTarget == null) return;

        DamageSource damageSource = caster.damageSources().magic();
        DamageSources.applyDamage(hitTarget, damage, damageSource);
        applyBeamHitEffect(level, hitTarget);
    }

    @Nullable
    private LivingEntity findFirstValidTarget(Level level, Vec3 startPos, Vec3 endPos) {
        AABB sweepBox = new AABB(startPos, endPos).inflate(0.6);

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity candidate : level.getEntities(this, sweepBox)) {
            if (!(candidate instanceof LivingEntity living)) continue;
            if (!living.isAlive()) continue;
            if (AllyUtils.isAlly(caster, living)) continue;

            AABB box = living.getBoundingBox().inflate(0.3);
            var hit = box.clip(startPos, endPos);
            if (hit.isPresent()) {
                double dist = hit.get().distanceToSqr(startPos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = living;
                }
            }
        }
        return closest;
    }

    private void spawnImpactParticles(Level level, Vec3 pos) {
        switch (beamType) {
            case FLAME -> {
                MagicManager.spawnParticles(level, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 12, 0.15, 0.15, 0.15, 0.3, false);
                MagicManager.spawnParticles(level, ParticleTypes.FLAME, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.1, false);
            }
            case HOLY -> {
                MagicManager.spawnParticles(level, ParticleTypes.INSTANT_EFFECT, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.05, false);
                MagicManager.spawnParticles(level, ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.2, false);
            }
            case SOLAR -> {
                MagicManager.spawnParticles(level, ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.1, false);
                MagicManager.spawnParticles(level, ParticleTypes.COMPOSTER, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.05, false);
            }
            case SPECTRAL -> {
                MagicManager.spawnParticles(level, ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 12, 0.15, 0.15, 0.15, 0.05, false);
                MagicManager.spawnParticles(level, ParticleTypes.CRIT, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.2, false);
            }
            default -> {}
        }
    }

    /**
     * 各ビーム元スペル(FlameRaySpell, HolyRaySpell, SolarRaySpell, SpectalRaySpell)の
     * onCast内ヒット処理を再現する。
     */
    private void applyBeamHitEffect(Level level, LivingEntity target) {
        switch (beamType) {
            case FLAME -> beamType.applyEffect(target, spellLevel); // 炎上付与 (BeamType.FLAMEのapplyEffectと同内容)

            case HOLY -> {
                if (target.isInvertedHealAndHarm()) { // アンデッド特攻
                    target.hurt(target.damageSources().magic(), 6.0f + (spellLevel * 2.0f));
                }
            }

            case SOLAR -> {
                var activeEffect = target.getEffect(ModEffects.MELTING.get());
                int currentAmplifier = 0;
                if (activeEffect != null) {
                    currentAmplifier = activeEffect.getAmplifier() + 1;
                }
                target.addEffect(new MobEffectInstance(ModEffects.MELTING.get(), 200, currentAmplifier));
            }

            case SPECTRAL -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));

                double radius = 32.0D;
                AABB area = target.getBoundingBox().inflate(radius, radius, radius);
                List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, area);

                for (Mob mob : nearbyMobs) {
                    if (mob instanceof OwnableEntity ownable && ownable.getOwner() == caster) {
                        mob.setTarget(target);
                    } else if (mob.getClass().getSimpleName().contains("Summon") ||
                            mob.getClass().getName().contains("irons_spellbooks")) {
                        mob.setTarget(target);
                    }
                }
            }

            default -> {}
        }
    }

    public double getLength() { return this.length; }
    public BeamType getBeamType() { return this.beamType; }
    public Vec3 getBeamDirection() { return this.direction; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}