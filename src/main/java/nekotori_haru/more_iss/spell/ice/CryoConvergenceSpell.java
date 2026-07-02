package nekotori_haru.more_iss.spell.ice;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.spell.CustomAnimations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class CryoConvergenceSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "cryo_convergence");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .setAllowCrafting(true)
            .build();

    public CryoConvergenceSpell() {
        this.baseSpellPower = 15;
        this.spellPowerPerLevel = 3;
        this.manaCostPerLevel = 15;
        this.baseManaCost = 50;
        this.castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.range", Utils.stringTruncation(getRange(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.knockback", Utils.stringTruncation(getKnockback(spellLevel, caster), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ICE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.GLASS_BREAK);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return CustomAnimations.CRYO_CONVERGENCE_ARC;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    // ========== 詠唱完了時のダメージ処理 ==========
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            float damage = getDamage(spellLevel, entity);
            float range = getRange(spellLevel, entity);
            float knockback = getKnockback(spellLevel, entity);

            Vec3 origin = entity.getEyePosition();
            Vec3 lookVec = entity.getLookAngle();
            float halfAngle = (float) Math.toRadians(30);

            AABB searchBox = entity.getBoundingBox().inflate(range);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e != entity && e.isAlive() && e.position().distanceTo(origin) <= range);

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.position().subtract(origin).normalize();
                double dot = lookVec.dot(toTarget);
                double angle = Math.acos(dot);
                if (angle <= halfAngle) {
                    target.invulnerableTime = 0;

                    // 通常の物理ダメージ（mobAttack）
                    DamageSource damageSource = level.damageSources().mobAttack(entity);
                    target.hurt(damageSource, damage);

                    Vec3 knockbackVec = toTarget.add(0, 0.2, 0).normalize();
                    target.setDeltaMovement(knockbackVec.scale(knockback * 1.5));
                    target.hurtMarked = true;

                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    target.setTicksFrozen(60);
                }
            }

            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_BREAK, entity.getSoundSource(), 2.0f, 0.5f);

            spawnWaveParticles(serverLevel, origin, lookVec, range);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // ========== パーティクル補助メソッド ==========
    private static final double PARTICLE_SPEED_MIN = 1.6;
    private static final double PARTICLE_SPEED_MAX = 2.6;

    private void spawnWaveParticles(ServerLevel level, Vec3 origin, Vec3 lookVec, float range) {
        for (int i = 0; i < 200; i++) {
            double angleYaw = (Math.random() - 0.5) * Math.toRadians(60);
            double anglePitch = (Math.random() - 0.5) * Math.toRadians(20);
            double dist = Math.random() * range;

            Vec3 up = new Vec3(0, 1, 0);
            Vec3 right = lookVec.cross(up).normalize();
            Vec3 upRot = right.cross(lookVec).normalize();

            Vec3 dir = lookVec.scale(Math.cos(angleYaw) * Math.cos(anglePitch))
                    .add(right.scale(Math.sin(angleYaw) * Math.cos(anglePitch)))
                    .add(upRot.scale(Math.sin(anglePitch)))
                    .normalize();

            Vec3 pos = origin.add(dir.scale(dist));
            double speed = PARTICLE_SPEED_MIN + Math.random() * (PARTICLE_SPEED_MAX - PARTICLE_SPEED_MIN);
            Vec3 velocity = dir.scale(speed);

            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0,
                    velocity.x, velocity.y - 0.05, velocity.z, 1.0);
            if (Math.random() < 0.15) {
                level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 0,
                        velocity.x, velocity.y, velocity.z, 1.0);
            }
        }

        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(lookVec).normalize();

            Vec3 dir = right.scale(Math.sin(rad)).add(up.scale(Math.cos(rad))).normalize();
            Vec3 pos = origin.add(dir.scale(range));
            Vec3 velocity = dir.scale(1.2);

            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0,
                    velocity.x, velocity.y + 0.1, velocity.z, 1.0);
        }
    }

    // ========== 数値計算メソッド ==========
    public float getDamage(int spellLevel, LivingEntity caster) {
        return this.baseSpellPower + (spellLevel * this.spellPowerPerLevel) + getSpellPower(spellLevel, caster) * 0.5f;
    }

    public float getRange(int spellLevel, LivingEntity caster) {
        return 15.0f + getSpellPower(spellLevel, caster) * 0.5f;
    }

    public float getKnockback(int spellLevel, LivingEntity caster) {
        return 2.0f + getSpellPower(spellLevel, caster) * 0.1f;
    }
}