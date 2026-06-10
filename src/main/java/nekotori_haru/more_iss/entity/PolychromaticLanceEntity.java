package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.phys.HitResult;

public class PolychromaticLanceEntity extends AbstractArrow {
    private int effectType; // 0:防具貫通, 1:凍結, 2:火炎
    private boolean boundingBoxSet = false;
    private boolean hasHit = false;

    public PolychromaticLanceEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setNoPhysics(false);
        this.pickup = Pickup.DISALLOWED;
    }

    public PolychromaticLanceEntity(Level level, LivingEntity owner, float damage, int effectType) {
        super(ModEntities.POLYCHROMATIC_LANCE.get(), level);
        this.effectType = effectType;
        this.setBaseDamage(damage);
        this.setKnockback(0);
        this.setNoGravity(true);
        this.setNoPhysics(false);
        this.setOwner(owner);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        if (!boundingBoxSet && !this.level().isClientSide) {
            this.setBoundingBox(this.getBoundingBox().inflate(0.6, 0.6, 0.6));
            boundingBoxSet = true;
        }

        super.tick();

        // 軌跡パーティクル (LightningLance方式)
        if (this.level().isClientSide && this.tickCount % 2 == 0 && !hasHit) {
            Vec3 vec3 = this.position().subtract(this.getDeltaMovement());
            if (effectType == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, vec3.x, vec3.y, vec3.z, 0, 0, 0);
            } else if (effectType == 1) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, vec3.x, vec3.y, vec3.z, 0, 0, 0);
            } else {
                this.level().addParticle(ParticleTypes.FLAME, vec3.x, vec3.y, vec3.z, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (hasHit) return;
        hasHit = true;

        if (result.getEntity() instanceof LivingEntity target) {
            float damage = (float) this.getBaseDamage();
            float finalDamage = damage;
            LivingEntity owner = (LivingEntity) this.getOwner();

            DamageSource normalDamageSource = this.damageSources().arrow(this, owner);
            target.hurt(normalDamageSource, 0.1f);

            if (effectType == 0) {
                finalDamage = damage * 1.3f;
                target.hurt(this.damageSources().magic(), finalDamage);
            } else if (effectType == 1) {
                target.hurt(this.damageSources().freeze(), finalDamage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                target.setTicksFrozen(40);
            } else if (effectType == 2) {
                target.hurt(this.damageSources().onFire(), finalDamage);
                target.setSecondsOnFire(3);
            }
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (hasHit) return;
        hasHit = true;
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        // ⭐ LightningLance方式のパーティクル発生
        if (!this.level().isClientSide) {
            Vec3 pos = result.getLocation();

            // 魔法パーティクル (ENCHANT)
            MagicManager.spawnParticles(this.level(), ParticleTypes.ENCHANT,
                    pos.x, pos.y + 0.5, pos.z, 40, 0.5, 0.5, 0.5, 0.5, true);

            // 炎パーティクル (SOUL_FIRE_FLAME)
            MagicManager.spawnParticles(this.level(), ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x, pos.y + 0.5, pos.z, 30, 0.4, 0.4, 0.4, 0.4, true);

            // 氷パーティクル (SNOWFLAKE)
            MagicManager.spawnParticles(this.level(), ParticleTypes.SNOWFLAKE,
                    pos.x, pos.y + 0.5, pos.z, 30, 0.4, 0.4, 0.4, 0.4, true);

            // 光パーティクル (END_ROD)
            MagicManager.spawnParticles(this.level(), ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.3, true);

            // 爆発パーティクル
            MagicManager.spawnParticles(this.level(), ParticleTypes.EXPLOSION,
                    pos.x, pos.y + 0.5, pos.z, 1, 0.5, 0.5, 0.5, 0, true);
        }

        super.onHit(result);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}