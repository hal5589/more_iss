package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.api.BeamType;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.ModEntities;
import nekotori_haru.more_iss.util.AllyUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import java.util.Random;

public class BeamWarningEntity extends Entity {

    private static final Random RANDOM = new Random();

    public static final int WARNING_DURATION = 20;

    // ⭐ Iron's Spells の SoundRegistry を使用
    private static final SoundEvent SOUND_FLAME = SoundRegistry.FIRE_CAST.get();
    private static final SoundEvent SOUND_HOLY = SoundRegistry.HOLY_CAST.get();
    private static final SoundEvent SOUND_SOLAR = SoundRegistry.SUNBEAM_IMPACT.get();
    private static final SoundEvent SOUND_SPECTRAL = SoundRegistry.ENDER_CAST.get();

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

    public BeamWarningEntity(EntityType<?> type, Level level, LivingEntity caster, Vec3 startPos,
                             Vec3 direction, double length, BeamType beamType, float damage, int spellLevel) {
        this(type, level);
        this.caster = caster;
        this.damage = damage;
        this.spellLevel = spellLevel;

        this.setPos(startPos.x, startPos.y, startPos.z);

        this.entityData.set(DATA_LENGTH, (float) length);
        this.entityData.set(DATA_BEAM_TYPE, beamType.name());
        this.entityData.set(DATA_DIR_X, (float) direction.x);
        this.entityData.set(DATA_DIR_Y, (float) direction.y);
        this.entityData.set(DATA_DIR_Z, (float) direction.z);
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
            return;
        }

        if (this.tickCount >= WARNING_DURATION && !this.firedBeam) {
            this.firedBeam = true;
            fireBeam();
            this.discard();
        }
    }

    public double getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public BeamType getBeamType() {
        try {
            return BeamType.valueOf(this.entityData.get(DATA_BEAM_TYPE));
        } catch (IllegalArgumentException e) {
            return BeamType.FLAME;
        }
    }

    public Vec3 getBeamDirection() {
        return new Vec3(
                this.entityData.get(DATA_DIR_X),
                this.entityData.get(DATA_DIR_Y),
                this.entityData.get(DATA_DIR_Z)
        );
    }

    private void fireBeam() {
        if (caster == null || !caster.isAlive()) return;
        Level level = this.level();

        Vec3 startPos = this.position();
        double length = this.getLength();
        Vec3 direction = this.getBeamDirection();
        Vec3 endPos = startPos.add(direction.scale(length));

        BeamType beamType = this.getBeamType();

        // ⭐ 属性に応じた音を再生
        playBeamSound(level, startPos, beamType);

        BaseBeamVisualEntity visual = new BaseBeamVisualEntity(
                ModEntities.BASE_BEAM_VISUAL.get(), level, caster, length, beamType);
        visual.setPos(startPos.x, startPos.y, startPos.z);
        visual.setXRot(90.0f);
        visual.setYRot(0.0f);
        level.addFreshEntity(visual);

        LivingEntity hitTarget = findFirstValidTarget(level, startPos, endPos);

        Vec3 particlePos = hitTarget != null
                ? hitTarget.position().add(0, hitTarget.getBbHeight() / 2, 0)
                : endPos;
        spawnImpactParticles(level, particlePos, beamType);

        if (hitTarget == null) return;

        DamageSource damageSource = caster.damageSources().magic();
        DamageSources.applyDamage(hitTarget, damage, damageSource);
        applyBeamHitEffect(level, hitTarget, beamType);
    }

    private void playBeamSound(Level level, Vec3 pos, BeamType beamType) {
        if (level.isClientSide) return;
        SoundEvent sound;
        float pitch = 0.8f + RANDOM.nextFloat() * 0.4f;

        switch (beamType) {
            case FLAME -> sound = SOUND_FLAME;
            case HOLY -> sound = SOUND_HOLY;
            case SOLAR -> sound = SOUND_SOLAR;
            case SPECTRAL -> sound = SOUND_SPECTRAL;
            default -> sound = SOUND_FLAME;
        }

        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 1.0f, pitch);
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

    private void spawnImpactParticles(Level level, Vec3 pos, BeamType beamType) {
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

    private void applyBeamHitEffect(Level level, LivingEntity target, BeamType beamType) {
        switch (beamType) {
            case FLAME -> target.setSecondsOnFire(3 + spellLevel / 2);
            case HOLY -> {
                if (target.isInvertedHealAndHarm()) {
                    target.hurt(target.damageSources().magic(), 6.0f + spellLevel * 2.0f);
                }
            }
            case SOLAR -> {
                var activeEffect = target.getEffect(ModEffects.MELTING.get());
                int amp = activeEffect != null ? activeEffect.getAmplifier() + 1 : 0;
                target.addEffect(new MobEffectInstance(ModEffects.MELTING.get(), 200, amp));
            }
            case SPECTRAL -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                double radius = 32.0;
                AABB area = target.getBoundingBox().inflate(radius, radius, radius);
                List<Mob> nearby = level.getEntitiesOfClass(Mob.class, area);
                for (Mob mob : nearby) {
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

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}