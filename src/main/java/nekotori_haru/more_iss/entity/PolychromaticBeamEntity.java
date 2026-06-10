package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import nekotori_haru.more_iss.registry.ModEntities;
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
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class PolychromaticBeamEntity extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> COLOR_OUTER =
            SynchedEntityData.defineId(PolychromaticBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_INNER =
            SynchedEntityData.defineId(PolychromaticBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(PolychromaticBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(PolychromaticBeamEntity.class, EntityDataSerializers.FLOAT);

    private LivingEntity owner;
    private float damage;
    private int effectType;
    private boolean spellActive = true;
    private final Random random = new Random();
    private int hitCooldown = 0;

    public PolychromaticBeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public PolychromaticBeamEntity(Level level, LivingEntity owner, float damage, int effectType, float maxRange) {
        super(ModEntities.POLYCHROMATIC_BEAM.get(), level);
        this.owner = owner;
        this.damage = damage;
        this.effectType = effectType;
        this.noPhysics = true;
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR_OUTER, 0xCCAAFF);
        entityData.define(COLOR_INNER, 0xEECCFF);
        entityData.define(LENGTH, 60.0f);
        entityData.define(RADIUS, 0.25f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!spellActive) {
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }
        } else {
            if (owner == null) return;
            if (!owner.isAlive()) {
                this.discard();
                return;
            }
        }

        if (owner != null) {
            this.setPos(owner.getEyePosition());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }

        if (this.level().isClientSide) {
            Vec3 tip = getBeamTip();
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 0, 0, 0);
            }
        }

        if (!this.level().isClientSide && owner != null) {
            if (hitCooldown <= 0) {
                damageHitTarget();
                hitCooldown = 5;
            } else {
                hitCooldown--;
            }
        }
    }

    public void updateLength(float maxLength, Level level) {
        if (owner == null) return;
        Vec3 start = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 end = start.add(lookVec.scale(maxLength));

        var hitResult = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        float length = (float) start.distanceTo(hitResult.getLocation());
        if (length < 0.5f) length = 0.5f;
        this.entityData.set(LENGTH, length);
    }

    private void damageHitTarget() {
        if (owner == null) return;

        Vec3 start = owner.getEyePosition();
        Vec3 lookVec = owner.getLookAngle();
        float maxRange = this.getLength();

        // ⭐ 範囲を拡大（最大範囲も大きく）
        float range = Math.max(maxRange, 3.0f);

        // ⭐ サーチボックスを大きく（視野角も拡大）
        double searchRadius = range * 1.5;
        AABB searchBox = owner.getBoundingBox().inflate(searchRadius, searchRadius, searchRadius);
        var entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != owner && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDistance = range;

        // ⭐ 視野角を広げる (0.85 → 0.6 で約53度以内)
        double dotThreshold = 0.6;

        for (LivingEntity target : entities) {
            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 toTarget = targetPos.subtract(start).normalize();
            double dot = toTarget.dot(lookVec);
            if (dot < dotThreshold) continue;

            double distance = start.distanceTo(targetPos);
            // ⭐ 距離判定をより寛容に（最大範囲まで）
            if (distance <= range) {
                // 最も近い敵を選択（複数ではなく一番近い1体）
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
        }

        if (closestTarget != null) {
            applyDamage(closestTarget);
        }
    }

    private void applyDamage(LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 毎回ランダムな効果タイプを決定
        int currentEffect = random.nextInt(3);
        float finalDamage = damage;

        // ⭐ PolychromaticLanceEntity と同じ方式（矢のダメージソース + 効果タイプ別）
        DamageSource normalDamageSource = this.damageSources().indirectMagic(this, owner);
        // キルクレジット用の微量ダメージ
        target.hurt(normalDamageSource, 0.1f);

        if (currentEffect == 0) {
            // 防具貫通：魔法ダメージ + ダメージ増加
            finalDamage = damage * 1.3f;
            target.hurt(this.damageSources().magic(), finalDamage);

            for (int i = 0; i < 5; i++) {
                this.level().addParticle(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        0, 0, 0);
            }
        } else if (currentEffect == 1) {
            // 凍結
            target.hurt(this.damageSources().freeze(), finalDamage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            target.setTicksFrozen(40);

            for (int i = 0; i < 8; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        (random.nextDouble() - 0.5) * 0.5,
                        random.nextDouble() * 0.3,
                        (random.nextDouble() - 0.5) * 0.5);
            }
        } else {
            // 火炎
            target.hurt(this.damageSources().onFire(), finalDamage);
            target.setSecondsOnFire(3);

            for (int i = 0; i < 8; i++) {
                this.level().addParticle(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        (random.nextDouble() - 0.5) * 0.5,
                        random.nextDouble() * 0.3,
                        (random.nextDouble() - 0.5) * 0.5);
            }
        }

        // 共通のヒットエフェクト
        MagicManager.spawnParticles(this.level(), ParticleTypes.ENCHANT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                10, 0.5, 0.5, 0.5, 0.1, true);

        this.effectType = currentEffect;
    }

    private Vec3 getBeamTip() {
        return this.position().add(this.getLookAngle().scale(this.getLength()));
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setup(int outerColor, int innerColor, float length, float radius) {
        this.entityData.set(COLOR_OUTER, outerColor);
        this.entityData.set(COLOR_INNER, innerColor);
        this.entityData.set(LENGTH, length);
        this.entityData.set(RADIUS, radius);
    }

    public float getLength() { return this.entityData.get(LENGTH); }
    public float getRadius() { return this.entityData.get(RADIUS); }
    public int getOuterColor() { return this.entityData.get(COLOR_OUTER); }
    public int getInnerColor() { return this.entityData.get(COLOR_INNER); }
    public int getEffectType() { return effectType; }

    public void setSpellActive(boolean active) {
        this.spellActive = active;
        if (!active && (owner == null || !owner.isAlive())) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getFloat("Damage");
        effectType = tag.getInt("EffectType");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", damage);
        tag.putInt("EffectType", effectType);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float length = this.getLength();
        float radius = this.getRadius();
        Vec3 dir = this.getLookAngle().normalize();
        Vec3 start = this.position();
        Vec3 end = start.add(dir.scale(length));
        return new AABB(start, end).inflate(radius);
    }
}