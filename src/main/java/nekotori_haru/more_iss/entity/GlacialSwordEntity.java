package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlacialSwordEntity extends net.minecraft.world.entity.Entity {

    private LivingEntity owner;
    private float damage = 20.0f;
    private float targetXRot = 0.0F;

    private final Set<UUID> damagedEntities = new HashSet<>();
    private boolean hasDamaged = false;
    private Vec3 spawnPos = null;

    public GlacialSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public GlacialSwordEntity(Level level, LivingEntity owner) {
        this(ModEntities.GLACIAL_SWORD.get(), level);
        this.owner = owner;
    }

    public void setDamage(float damage) {
        this.damage = damage;
        More_iss.LOGGER.info("GlacialSwordEntity - Damage set to: {}", this.damage);
    }

    public void setTargetXRot(float rot) {
        this.targetXRot = rot;
        this.setXRot(rot);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);

        int age = this.tickCount;

        // 湧いた位置を固定（最初の1tickで記録）
        if (spawnPos == null && owner != null && age == 1) {
            spawnPos = new Vec3(owner.getX(), owner.getEyeY() + 1.5, owner.getZ());
            this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            this.setYRot(owner.getYRot());
            this.setXRot(this.targetXRot);
        }

        if (age <= 20) {
            // 待機
            if (this.level().isClientSide) {
                spawnLightParticles();
            }
        } else if (age <= 30) {
            // 落下フェーズ（2ブロック）
            int fallTick = age - 20;
            float progress = (float) fallTick / 10.0F;
            float t = Math.min(1.0f, progress);
            float easeT = t * t;

            if (spawnPos != null) {
                double newY = spawnPos.y - (easeT * 2.0);
                this.setPos(this.getX(), newY, this.getZ());
            }

            if (this.level().isClientSide) {
                spawnFallParticles();
            }
        } else if (age <= 50) {
            int swingTick = age - 30;
            float progress = (float) swingTick / 20.0F;
            float t = Math.min(1.0f, progress);

            // 加速度的に速くなるイージング（t^2）
            float easeT = t * t;

            // 0度（垂直）→ 90度（水平）まで回転
            float newRot = 0.0F + (easeT * 90.0F);
            this.targetXRot = newRot;
            this.setXRot(newRot);

            if (this.level().isClientSide) {
                spawnSwingParticles();
            }

            // 爆発は age 50 で発生
            if (age == 50) {
                this.impact();
                // 爆発したら即座に消滅
                this.discard();
            }
        } else if (age > 55) {
            if (!this.level().isClientSide) {
                this.discard();
            }
        }
    }

    private void impact() {
        if (!(this.level() instanceof ServerLevel serverWorld)) return;
        if (hasDamaged) return;
        hasDamaged = true;

        More_iss.LOGGER.info("GlacialSwordEntity - Impact! Damage: {}", this.damage);

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();

        float yaw = this.getYRot();
        double radYaw = Math.toRadians(yaw);
        double dirX = Math.sin(radYaw);
        double dirZ = Math.cos(radYaw);

        // 範囲: 奥行き20ブロック、幅9ブロック、高さ 上5ブロック・下3ブロック
        double forwardLength = 20.0;
        double halfWidth = 4.5;
        double upHeight = 5.0;
        double downHeight = 3.0;

        // 範囲の開始位置を剣の位置から前方にする
        double startOffset = -1.0;

        double[][] corners = {
                {-halfWidth, -downHeight, startOffset},
                { halfWidth, -downHeight, startOffset},
                {-halfWidth,  upHeight, startOffset},
                { halfWidth,  upHeight, startOffset},
                {-halfWidth, -downHeight, forwardLength},
                { halfWidth, -downHeight, forwardLength},
                {-halfWidth,  upHeight, forwardLength},
                { halfWidth,  upHeight, forwardLength}
        };

        double minX = centerX, minY = centerY, minZ = centerZ;
        double maxX = centerX, maxY = centerY, maxZ = centerZ;

        for (double[] corner : corners) {
            double localX = corner[0];
            double localY = corner[1];
            double localZ = corner[2];

            double worldX = centerX + localX * Math.cos(radYaw) - localZ * Math.sin(radYaw);
            double worldZ = centerZ + localX * Math.sin(radYaw) + localZ * Math.cos(radYaw);
            double worldY = centerY + localY;

            minX = Math.min(minX, worldX);
            minY = Math.min(minY, worldY);
            minZ = Math.min(minZ, worldZ);
            maxX = Math.max(maxX, worldX);
            maxY = Math.max(maxY, worldY);
            maxZ = Math.max(maxZ, worldZ);
        }

        AABB explosionBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // 範囲内のパーティクルを大量に召喚
        int particleCount = 300;

        for (int i = 0; i < particleCount; i++) {
            double randomX = minX + (maxX - minX) * random.nextDouble();
            double randomY = minY + (maxY - minY) * random.nextDouble();
            double randomZ = minZ + (maxZ - minZ) * random.nextDouble();

            serverWorld.sendParticles(ParticleTypes.SNOWFLAKE,
                    randomX, randomY, randomZ,
                    1, 0, 0, 0, 0.05);
        }

        MagicManager.spawnParticles(serverWorld, ParticleTypes.SNOWFLAKE, centerX, centerY, centerZ, 200, 8.0, 3.0, 8.0, 0.25D, false);
        MagicManager.spawnParticles(serverWorld, ParticleTypes.POOF, centerX, centerY, centerZ, 80, 5.0, 2.0, 5.0, 0.1D, false);
        MagicManager.spawnParticles(serverWorld,
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                centerX, centerY, centerZ, 120, 6.0, 2.0, 6.0, 0.3D, false);

        // 範囲内の全エンティティにダメージ
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, explosionBox);

        for (LivingEntity target : targets) {
            if (target == this.owner) continue;
            if (damagedEntities.contains(target.getUUID())) continue;

            float finalDamage = this.damage;

            More_iss.LOGGER.info("GlacialSwordEntity - Dealing {} damage to {}", finalDamage, target.getName().getString());

            target.invulnerableTime = 0;
            target.hurt(this.damageSources().indirectMagic(this, this.owner), finalDamage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));

            damagedEntities.add(target.getUUID());

            serverWorld.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }

        this.level().playSound(null, centerX, centerY, centerZ, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 5.0f, 0.4f);
        this.level().playSound(null, centerX, centerY, centerZ, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0f, 0.5f);
    }

    private void spawnLightParticles() {
        Level level = this.level();
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    this.getX() + (random.nextDouble() - 0.5) * 0.5,
                    this.getY() + (random.nextDouble() - 0.5) * 0.5,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0, -0.01, 0);
        }
    }

    private void spawnFallParticles() {
        Level level = this.level();
        for (int i = 0; i < 5; i++) {
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    this.getX() + (random.nextDouble() - 0.5) * 0.8,
                    this.getY() + random.nextDouble() * 0.5,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.8,
                    (random.nextDouble() - 0.5) * 0.1,
                    -0.05,
                    (random.nextDouble() - 0.5) * 0.1);
        }
    }

    private void spawnSwingParticles() {
        Level level = this.level();
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    this.getX() + (random.nextDouble() - 0.5) * 1.0,
                    this.getY() + (random.nextDouble() - 0.5) * 1.0,
                    this.getZ() + (random.nextDouble() - 0.5) * 1.0,
                    (random.nextDouble() - 0.5) * 0.15,
                    random.nextDouble() * 0.1,
                    (random.nextDouble() - 0.5) * 0.15);
        }
    }

    public float getTargetXRot() { return this.targetXRot; }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { this.damage = tag.getFloat("Damage"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putFloat("Damage", this.damage); }
}