package nekotori_haru.more_iss.entity;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import nekotori_haru.more_iss.More_iss;
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

import java.util.List;

public class NapalmBombEntity extends Entity {

    private LivingEntity owner;
    private float damage = 5.0f;
    private float explosionRadius = 3.0f;
    private int fuseTimer = 0;
    private static final int FUSE_TICKS = 60;

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

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.99, motion.y - 0.04, motion.z * 0.99);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
            fuseTimer++;
        }

        if (this.level().isClientSide) {
            if (fuseTimer == 0) {
                // 飛翔中：MagicFireballのtrailParticles移植
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
                // 着地後：くすぶり
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

        if (!this.level().isClientSide && fuseTimer >= FUSE_TICKS) {
            explode();
        }

        if (!this.level().isClientSide && this.tickCount > 200) {
            this.discard();
        }
    }

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

        // ノックバック先行
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

        // ダメージ＋着火
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

        // 🌟 【修正の核心：確実に存在する安定の spawnParticles でオリジナル爆発を作る】
        // 存在しない独自メソッドを完全に廃止し、ISS公式の安定メソッド「spawnParticles」を重ねて叩きます。
        // これにより、ISS特有の魔法粒子とバニラの爆発・炎が合わさり、ナパームらしい超ド派手な炎上爆発になります！
        double x = this.getX();
        double y = this.getY() + 0.15;
        double z = this.getZ();

        // 1. ISSの魔法粒子（EMBERS）を広範囲に散らす（ナパームの飛び散る火の粉を再現）
        MagicManager.spawnParticles(serverWorld, ParticleHelper.EMBERS,
                x, y, z, 50, radius * 0.5, radius * 0.5, radius * 0.5, 0.15D, false);

        // 2. バニラの大きな爆発の煙（視覚的なインパクト）
        MagicManager.spawnParticles(serverWorld, ParticleTypes.EXPLOSION,
                x, y, z, 5, radius * 0.3, radius * 0.3, radius * 0.3, 0.0D, false);

        // 3. 激しい炎の粒子（ナパームの燃え盛る演出）
        MagicManager.spawnParticles(serverWorld, ParticleTypes.FLAME,
                x, y, z, 40, radius * 0.4, radius * 0.4, radius * 0.4, 0.1D, false);


        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS,
                3.0f, (1.0f + (world.random.nextFloat() - world.random.nextFloat()) * 0.2f) * 0.7f);

        this.discard();
    }

    @Override protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.explosionRadius = tag.getFloat("ExplosionRadius");
        this.fuseTimer = tag.getInt("FuseTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putFloat("ExplosionRadius", this.explosionRadius);
        tag.putInt("FuseTimer", this.fuseTimer);
    }
}