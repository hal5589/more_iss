package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class PolychromaticLanceEntity extends Projectile {
    private int effectType;
    private float damage;
    private int lifeTicks = 0;
    private static final int MAX_LIFE = 600;
    private LivingEntity ownerCache;

    public PolychromaticLanceEntity(EntityType<? extends PolychromaticLanceEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PolychromaticLanceEntity(Level level, LivingEntity owner, float damage, int effectType) {
        super(ModEntities.POLYCHROMATIC_LANCE.get(), level);
        this.damage = damage;
        this.effectType = effectType;
        this.ownerCache = owner;
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        lifeTicks++;
        if (lifeTicks >= MAX_LIFE) {
            this.discard();
            return;
        }

        this.setNoGravity(true);

        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement());
        }

        Vec3 start = this.position();
        Vec3 delta = this.getDeltaMovement();
        Vec3 end = start.add(delta);

        if (!this.level().isClientSide) {
            Optional<EntityHitResult> entityHit = this.findHitEntity(start, end);
            if (entityHit.isPresent()) {
                this.onHitEntity(entityHit.get());
                return;
            }

            BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    start, end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this
            ));
            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHitBlock(blockHit);
                this.discard();
                return;
            }
        }

        this.setPos(end.x, end.y, end.z);

        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            Vec3 pos = this.position();
            if (effectType == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 0, 0, 0);
            } else if (effectType == 1) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, 0, 0);
            } else {
                this.level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    private Optional<EntityHitResult> findHitEntity(Vec3 start, Vec3 end) {
        AABB searchBox = this.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        var entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != this.getOwner() && e.isAlive());

        EntityHitResult closestHit = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity target : entities) {
            AABB targetBox = target.getBoundingBox().inflate(0.3);
            Optional<Vec3> hitPos = targetBox.clip(start, end);
            if (hitPos.isPresent()) {
                double dist = start.distanceToSqr(hitPos.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestHit = new EntityHitResult(target, hitPos.get());
                }
            }
        }
        return Optional.ofNullable(closestHit);
    }

    public void shoot(double x, double y, double z, float speed, float divergence) {
        Vec3 vec3 = (new Vec3(x, y, z)).normalize();
        this.setDeltaMovement(vec3.scale(speed));
        this.setYRot((float) Math.toDegrees(Math.atan2(x, z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z))));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;

        if (result.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = this.ownerCache;
            if (owner == null && this.getOwner() instanceof LivingEntity) {
                owner = (LivingEntity) this.getOwner();
            }

            // ★ 微量ダメージを削除（0.1ダメージは与えない）
            // 代わりに、ダメージソースにプレイヤー（owner）を直接指定する

            float finalDamage = damage;

            // ★ すべてのケースでプレイヤーソースの魔法ダメージを使用
            DamageSource source = this.damageSources().indirectMagic(this, owner);

            if (effectType == 0) {
                finalDamage = damage * 1.3f;
                target.hurt(source, finalDamage);
            } else if (effectType == 1) {
                target.hurt(source, finalDamage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                target.setTicksFrozen(40);
            } else if (effectType == 2) {
                target.hurt(source, finalDamage);
                target.setSecondsOnFire(3);
            }

            // パーティクル（サーバー側でMagicManagerを使用）
            MagicManager.spawnParticles(this.level(), ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.8, 0.8, 0.8, 0.2, true);
            MagicManager.spawnParticles(this.level(), ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.6, 0.6, 0.6, 0.15, true);
            MagicManager.spawnParticles(this.level(), ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.6, 0.6, 0.6, 0.15, true);
            MagicManager.spawnParticles(this.level(), ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.5, 0.5, 0.5, 0.1, true);
            for (int i = 0; i < 360; i += 45) {
                double rad = Math.toRadians(i);
                double px = target.getX() + Math.cos(rad) * 1.5;
                double pz = target.getZ() + Math.sin(rad) * 1.5;
                MagicManager.spawnParticles(this.level(), ParticleTypes.FIREWORK,
                        px, target.getY() + 0.5, pz, 1, 0, 0.1, 0, 0, true);
            }
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide) return;

        Vec3 pos = result.getLocation();
        MagicManager.spawnParticles(this.level(), ParticleTypes.ENCHANT,
                pos.x, pos.y + 0.5, pos.z, 40, 0.5, 0.5, 0.5, 0.5, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y + 0.5, pos.z, 30, 0.4, 0.4, 0.4, 0.4, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.SNOWFLAKE,
                pos.x, pos.y + 0.5, pos.z, 30, 0.4, 0.4, 0.4, 0.4, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.END_ROD,
                pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.3, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.EXPLOSION,
                pos.x, pos.y + 0.5, pos.z, 1, 0.5, 0.5, 0.5, 0, true);
        this.discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != this.getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EffectType", effectType);
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.effectType = tag.getInt("EffectType");
        this.damage = tag.getFloat("Damage");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}