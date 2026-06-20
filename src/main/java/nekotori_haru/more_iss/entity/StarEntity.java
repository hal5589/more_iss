package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class StarEntity extends Entity {

    private LivingEntity owner;
    private float damage = 8.0f;
    private float explosionRadius = 4.0f;
    private int lifeTimer = 0;
    private static final int MAX_LIFE = 80; // 4秒

    public StarEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public StarEntity(Level level, LivingEntity owner) {
        super(ModEntities.STAR.get(), level);
        this.owner = owner;
        this.noPhysics = true;
    }

    public void setDamage(float damage) { this.damage = damage; }
    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }

    @Override
    public void tick() {
        super.tick();

        // 減速処理
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 0.001) {
            this.setDeltaMovement(motion.scale(0.95));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        // 位置更新 (重力なし)
        this.move(MoverType.SELF, this.getDeltaMovement());

        // パーティクル (クライアント側)
        if (this.level().isClientSide) {
            // 白い輝き - 常時発光
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX() + (this.random.nextFloat() - 0.5f) * 0.3f,
                        this.getY() + (this.random.nextFloat() - 0.5f) * 0.3f,
                        this.getZ() + (this.random.nextFloat() - 0.5f) * 0.3f,
                        (this.random.nextFloat() - 0.5f) * 0.02,
                        (this.random.nextFloat() - 0.5f) * 0.02,
                        (this.random.nextFloat() - 0.5f) * 0.02
                );
            }

            // グローエフェクト (白い光)
            if (this.tickCount % 4 == 0) {
                for (int i = 0; i < 2; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double radius = 0.3 + this.random.nextDouble() * 0.2;
                    this.level().addParticle(ParticleTypes.GLOW,
                            this.getX() + Math.cos(angle) * radius,
                            this.getY() + Math.sin(angle) * radius * 0.5,
                            this.getZ() + Math.sin(angle) * radius,
                            0, 0, 0
                    );
                }
            }

            // きらめき (エレクトリックスパーク)
            if (this.tickCount % 10 == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (this.random.nextFloat() - 0.5f) * 0.5f,
                        this.getY() + (this.random.nextFloat() - 0.5f) * 0.5f,
                        this.getZ() + (this.random.nextFloat() - 0.5f) * 0.5f,
                        (this.random.nextFloat() - 0.5f) * 0.1,
                        (this.random.nextFloat() - 0.5f) * 0.1,
                        (this.random.nextFloat() - 0.5f) * 0.1
                );
            }
        }

        // ライフタイマー更新 (サーバー側)
        if (!this.level().isClientSide) {
            lifeTimer++;

            if (lifeTimer >= MAX_LIFE) {
                explode();
            }
        }

        // タイムアウト
        if (!this.level().isClientSide && this.tickCount > 300) {
            this.discard();
        }
    }

    private void explode() {
        Level world = this.level();
        if (!(world instanceof ServerLevel serverWorld)) return;

        float baseDamage = this.damage;

        // synthesis_spell_power を適用
        if (owner instanceof Player player) {
            Attribute synthesisPowerAttribute = ForgeRegistries.ATTRIBUTES.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "synthesis_spell_power"));
            if (synthesisPowerAttribute != null) {
                baseDamage *= (float) player.getAttributeValue(synthesisPowerAttribute);
            }
        }

        double radius = this.explosionRadius;
        AABB boundingBox = this.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, boundingBox);

        // ノックバック
        for (LivingEntity target : targets) {
            if (target == owner) continue;
            double distance = target.distanceTo(this);
            if (distance <= radius) {
                double exposure = 1.0 - (distance / radius);
                double dx = target.getX() - this.getX();
                double dy = target.getY() + target.getBbHeight() / 2.0 - this.getY();
                double dz = target.getZ() - this.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0 && exposure > 0) {
                    target.setDeltaMovement(target.getDeltaMovement().add(
                            dx / dist * exposure * 1.5,
                            dy / dist * exposure * 1.5 + 0.3,
                            dz / dist * exposure * 1.5
                    ));
                    target.hurtMarked = true;
                }
            }
        }

        // ダメージ
        for (LivingEntity target : targets) {
            if (target == owner) continue;
            double distance = target.distanceTo(this);
            if (distance <= radius) {
                double exposure = 1.0 - (distance / radius);
                if (exposure > 0) {
                    float finalDamage = (float) (baseDamage * exposure);
                    if (finalDamage > 0.5f) {
                        target.invulnerableTime = 0;
                        target.hurt(this.damageSources().explosion(this, owner), finalDamage);
                    }
                }
            }
        }

        double x = this.getX();
        double y = this.getY() + 0.15;
        double z = this.getZ();

        // 白い光の爆発エフェクト
        MagicManager.spawnParticles(serverWorld, ParticleTypes.GLOW,
                x, y, z, 100, radius * 0.6, radius * 0.6, radius * 0.6, 0.15D, false);

        MagicManager.spawnParticles(serverWorld, ParticleTypes.END_ROD,
                x, y, z, 60, radius * 0.5, radius * 0.5, radius * 0.5, 0.2D, false);

        MagicManager.spawnParticles(serverWorld, ParticleTypes.EXPLOSION,
                x, y, z, 8, radius * 0.3, radius * 0.3, radius * 0.3, 0.0D, false);

        // 白い星型の拡散パーティクル
        for (int i = 0; i < 80; i++) {
            double angle1 = world.random.nextDouble() * Math.PI * 2;
            double angle2 = world.random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + world.random.nextDouble() * 0.3;
            Vec3 vel = new Vec3(
                    Math.sin(angle1) * Math.cos(angle2) * speed,
                    Math.sin(angle2) * speed,
                    Math.cos(angle1) * Math.cos(angle2) * speed
            );
            serverWorld.sendParticles(ParticleTypes.END_ROD,
                    x, y, z,
                    1, vel.x, vel.y, vel.z, 0.0D
            );
        }

        // 白い閃光
        MagicManager.spawnParticles(serverWorld, ParticleTypes.FLASH,
                x, y, z, 1, 0, 0, 0, 0.0D, false);

        // 衝撃波リング
        for (int i = 0; i < 36; i++) {
            double angle = (i / 36.0) * Math.PI * 2;
            double r = radius * 0.3;
            serverWorld.sendParticles(ParticleTypes.GLOW,
                    x + Math.cos(angle) * r,
                    y + 0.1,
                    z + Math.sin(angle) * r,
                    1, 0, 0.05, 0, 0.0D
            );
        }

        // 音
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                3.0f, 0.8f + world.random.nextFloat() * 0.4f);

        this.discard();
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.explosionRadius = tag.getFloat("ExplosionRadius");
        this.lifeTimer = tag.getInt("LifeTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putInt("LifeTimer", this.lifeTimer);
    }

    private static class MoverType {
        public static final net.minecraft.world.entity.MoverType SELF = net.minecraft.world.entity.MoverType.SELF;
    }
}