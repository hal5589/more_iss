package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class MarkOfDetonationProjectile extends FireboltProjectile {

    private boolean hasTriggered = false;

    public MarkOfDetonationProjectile(EntityType<? extends FireboltProjectile> type, Level level) {
        super(type, level);
    }

    public MarkOfDetonationProjectile(Level level, LivingEntity shooter) {
        super(level, shooter);
        this.setOwner(shooter);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (this.hasTriggered) return;

        if (!this.level().isClientSide && hitResult.getEntity() instanceof LivingEntity target) {
            this.hasTriggered = true;

            LivingEntity shooter = this.getOwner() instanceof LivingEntity le ? le : null;

            var currentEffect = target.getEffect(ModEffects.DATONATION.get());
            int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;


            if (currentAmp == 1) {
                target.removeEffect(ModEffects.DATONATION.get());
                triggerExplosion(target, shooter);
                this.discard();
                return;
            }


            int nextAmplifier = currentAmp + 1;
            target.addEffect(new MobEffectInstance(
                    ModEffects.DATONATION.get(),
                    140,
                    nextAmplifier,
                    false,
                    false,
                    true
            ));

            if (shooter != null) {
                target.setLastHurtByMob(shooter);
            }
            target.hurt(this.damageSources().indirectMagic(this, shooter), this.getDamage());
            this.discard();

        } else {
            if (!this.hasTriggered) {
                this.hasTriggered = true;
                super.onHitEntity(hitResult);
            }
        }
    }

    private void triggerExplosion(LivingEntity center, LivingEntity shooter) {
        Level world = center.level();
        if (!(world instanceof ServerLevel serverWorld)) return;

        float baseDamage = this.getDamage() * 2.5F;

        if (shooter instanceof Player player) {
            Attribute firePowerAttribute = ForgeRegistries.ATTRIBUTES.getValue(
                    new ResourceLocation("irons_spellbooks", "fire_spell_power"));
            if (firePowerAttribute != null) {
                baseDamage *= (float) player.getAttributeValue(firePowerAttribute);
            }
        }

        double damageRadius = 5.0D;
        AABB boundingBox = center.getBoundingBox().inflate(damageRadius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, boundingBox);

        for (LivingEntity target : targets) {
            double distance = target.distanceTo(center);
            if (distance <= damageRadius) {
                double exposure = 1.0D - (distance / damageRadius);
                if (exposure > 0) {
                    double dx = target.getX() - center.getX();
                    double dy = target.getY() + target.getBbHeight() / 2.0
                            - (center.getY() + center.getBbHeight() / 2.0);
                    double dz = target.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 0) {
                        target.setDeltaMovement(target.getDeltaMovement().add(
                                dx / dist * exposure,
                                dy / dist * exposure,
                                dz / dist * exposure
                        ));
                        target.hurtMarked = true;
                    }
                }
            }
        }

        for (LivingEntity target : targets) {
            double distance = target.distanceTo(center);
            if (distance <= damageRadius) {
                double exposure = 1.0D - (distance / damageRadius);
                if (exposure > 0) {
                    float finalDamage = (float) (baseDamage * exposure);
                    if (finalDamage > 0.5F) {
                        target.invulnerableTime = 0;
                        target.hurt(center.damageSources().explosion(shooter, shooter), finalDamage);                    }
                }
            }
        }

        serverWorld.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.getX(),
                center.getY() + center.getBbHeight() / 2.0F,
                center.getZ(),
                1, 0, 0, 0, 0
        );
        world.playSound(
                null,
                center.getX(), center.getY(), center.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS,
                4.0F,
                (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F
        );
    }
}