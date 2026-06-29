package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class NapalmBombEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity owner;
    private float damage = 5.0f;
    private float explosionRadius = 3.0f;
    private int fuseTimer = 0;
    private static final int FUSE_TICKS = 60;

    // ★ レンダリング用：前フレームの速度（着地直前の速度を保存するため）
    public Vec3 prevMotion = Vec3.ZERO;

    // ★ 着地検出用：前回のtickでの接地状態
    private boolean prevOnGround = false;

    // ★ 着地時の角度を保存
    private float savedPitch = 0f;
    private float savedYaw = 0f;
    private boolean angleSaved = false;

    public NapalmBombEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public NapalmBombEntity(Level level, LivingEntity owner) {
        super(ModEntities.NAPALM_BOMB.get(), level);
        this.owner = owner;
        this.noPhysics = false;
    }

    public void setDamage(float damage) { this.damage = damage; }
    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }
    public boolean isGrounded() { return fuseTimer > 0; }

    public float getSavedPitch() { return savedPitch; }
    public float getSavedYaw() { return savedYaw; }
    public boolean isAngleSaved() { return angleSaved; }

    @Override
    public void tick() {
        super.tick();

        // ★ ① まず現在の速度を保存（着地直前の速度として後で使う）
        prevMotion = this.getDeltaMovement();

        // ② 重力と減速（放物線運動）
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.99, motion.y - 0.04, motion.z * 0.99);
        this.move(MoverType.SELF, this.getDeltaMovement());

        // ★ ③ 着地検出：「今接地した」＝「現在接地している」かつ「前回は接地していなかった」
        if (this.onGround() && !prevOnGround && !angleSaved) {
            // 着地直前の速度（prevMotion）を使って角度を計算して保存
            Vec3 vel = prevMotion;
            if (vel.lengthSqr() > 0.0001) {
                float pitch = ((float) (Mth.atan2(vel.horizontalDistance(), vel.y) * (180F / Math.PI)) - 90.0F);
                float yaw = -((float) (Mth.atan2(vel.z, vel.x) * (180F / Math.PI)) - 90.0F);
                this.savedPitch = pitch;
                this.savedYaw = yaw;
            } else {
                // 速度がほぼゼロ（めったにない）の場合：現在の向きをそのまま保存
                this.savedPitch = 0f;
                this.savedYaw = 0f;
            }
            this.angleSaved = true;
        }

        // ④ 接地中の処理（速度ゼロ、フューズ進行）
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
            fuseTimer++;
        }

        // ★ ⑤ 次フレームのために、今回の接地状態を保存
        prevOnGround = this.onGround();

        // ----- パーティクル処理（元の処理をそのまま維持） -----
        if (this.level().isClientSide) {
            if (fuseTimer == 0) {
                Vec3 vec3 = this.getDeltaMovement();
                double d0 = this.getX() - vec3.x;
                double d1 = this.getY() - vec3.y;
                double d2 = this.getZ() - vec3.z;
                int count = Mth.clamp((int) (vec3.lengthSqr() * 4), 1, 4);
                for (int i = 0; i < count; i++) {
                    Vec3 random = Utils.getRandomVec3(0.25);
                    float f = i / ((float) count);
                    double x = Mth.lerp(f, d0, this.getX());
                    double y = Mth.lerp(f, d1, this.getY());
                    double z = Mth.lerp(f, d2, this.getZ());
                    this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                            x - random.x, y + 0.5 - random.y, z - random.z,
                            random.x * 0.5f, random.y * 0.5f, random.z * 0.5f);
                    this.level().addParticle(ParticleHelper.EMBERS,
                            x - random.x, y + 0.5 - random.y, z - random.z,
                            random.x * 0.5f, random.y * 0.5f, random.z * 0.5f);
                }
            } else {
                this.level().addParticle(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.2, this.getZ(),
                        0, 0.02, 0);
                if (fuseTimer % 5 == 0) {
                    this.level().addParticle(ParticleTypes.SMALL_FLAME,
                            this.getX() + (this.random.nextFloat() - 0.5f) * 0.3f,
                            this.getY() + 0.1f,
                            this.getZ() + (this.random.nextFloat() - 0.5f) * 0.3f,
                            0, 0.02, 0);
                }
            }
        }

        // 爆発処理
        if (!this.level().isClientSide && fuseTimer >= FUSE_TICKS) {
            explode();
        }

        // タイムアウト（安全装置）
        if (!this.level().isClientSide && this.tickCount > 200) {
            this.discard();
        }
    }

    // ----- 爆発処理（元の処理をそのまま維持） -----
    private void explode() {
        Level world = this.level();
        if (!(world instanceof ServerLevel serverWorld)) return;

        float baseDamage = this.damage * 2.5f;

        if (owner instanceof Player player) {
            Attribute firePowerAttribute = ForgeRegistries.ATTRIBUTES.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_spell_power"));
            if (firePowerAttribute != null) {
                baseDamage *= (float) player.getAttributeValue(firePowerAttribute);
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
                            dx / dist * exposure,
                            dy / dist * exposure,
                            dz / dist * exposure
                    ));
                    target.hurtMarked = true;
                }
            }
        }

        // ダメージ＆着火
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
                        target.setSecondsOnFire(5 + (int) (exposure * 5));
                    }
                }
            }
        }

        // パーティクル＆音
        double x = this.getX();
        double y = this.getY() + 0.15;
        double z = this.getZ();

        MagicManager.spawnParticles(serverWorld, ParticleHelper.EMBERS,
                x, y, z, 50, radius * 0.5, radius * 0.5, radius * 0.5, 0.15D, false);
        MagicManager.spawnParticles(serverWorld, ParticleTypes.EXPLOSION,
                x, y, z, 5, radius * 0.3, radius * 0.3, radius * 0.3, 0.0D, false);
        MagicManager.spawnParticles(serverWorld, ParticleTypes.FLAME,
                x, y, z, 40, radius * 0.4, radius * 0.4, radius * 0.4, 0.1D, false);

        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS,
                3.0f, (1.0f + (world.random.nextFloat() - world.random.nextFloat()) * 0.2f) * 0.7f);

        this.discard();
    }

    // ----- NBT（角度も保存） -----
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.explosionRadius = tag.getFloat("ExplosionRadius");
        this.fuseTimer = tag.getInt("FuseTimer");
        this.savedPitch = tag.getFloat("SavedPitch");
        this.savedYaw = tag.getFloat("SavedYaw");
        this.angleSaved = tag.getBoolean("AngleSaved");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putInt("FuseTimer", this.fuseTimer);
        tag.putFloat("SavedPitch", this.savedPitch);
        tag.putFloat("SavedYaw", this.savedYaw);
        tag.putBoolean("AngleSaved", this.angleSaved);
    }

    // ----- GeoEntity 実装 -----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "dummy", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}