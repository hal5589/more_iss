package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.eldritch_blast.EldritchBlastVisualEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

@AutoSpellConfig
public class FunnelSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("more_iss", "funnel");

    private static final int CAST_DURATION_TICKS = 60;
    private static final float FUNNEL_RADIUS = 4.0f;
    private static final float SPREAD = 0.8f;
    private static final float HIT_RADIUS = 2.0f;
    private static final Random RANDOM = new Random();

    public FunnelSpell() {
        this.manaCostPerLevel = 25;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.baseManaCost = 100;
    }

    @Override
    public CastType getCastType() { return CastType.CONTINUOUS; }

    @Override
    public DefaultConfig getDefaultConfig() {
        return new DefaultConfig()
                .setMinRarity(SpellRarity.LEGENDARY)
                .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
                .setMaxLevel(5)
                .setCooldownSeconds(24)
                .build();
    }

    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public int getCastTime(int spellLevel) { return CAST_DURATION_TICKS; }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        applyAscensionEffects(level, spellLevel, entity);

        Vec3 motion = entity.getLookAngle().multiply(1, 0, 1).normalize().add(0, 5, 0).scale(.125);
        entity.setDeltaMovement(entity.getDeltaMovement().add(motion));
        entity.hasImpulse = true;

        if (playerMagicData != null) {
            playerMagicData.setAdditionalCastData(new ImpulseCastData(
                    (float) motion.x, (float) motion.y, (float) motion.z, true));
        }

        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onClientPreCast(Level level, int spellLevel, LivingEntity entity, InteractionHand hand, @Nullable MagicData playerMagicData) {
        Vec3 motion = entity.getLookAngle().multiply(1, 0, 1).normalize().add(0, 5, 0).scale(.125);
        entity.setDeltaMovement(entity.getDeltaMovement().add(motion));
        entity.hasImpulse = true;

        super.onClientPreCast(level, spellLevel, entity, hand, playerMagicData);
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        super.onClientCast(level, spellLevel, entity, castData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData == null) return;
        spawnFunnelBlast(level, spellLevel, entity);
    }

    private void applyAscensionEffects(Level level, int spellLevel, LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                MobEffectRegistry.ASCENSION.get(), CAST_DURATION_TICKS + 20, 0, false, false, true));

        Vec3 vec = entity.position();
        for (int i = 0; i < 32; i++) {
            if (!level.getBlockState(BlockPos.containing(vec).below()).isAir()) break;
            vec = vec.subtract(0, 1, 0);
        }
        Vec3 strikePos = vec;

        LightningBolt lb = EntityType.LIGHTNING_BOLT.create(level);
        if (lb != null) {
            lb.setVisualOnly(true);
            lb.setPos(strikePos);
            level.addFreshEntity(lb);
        }
        final LightningBolt finalLb = lb;

        float radius = 5;
        level.getEntities(entity, entity.getBoundingBox().inflate(radius)).forEach(target -> {
            double distance = target.distanceToSqr(strikePos);
            if (distance < radius * radius) {
                float finalDamage = (float) (getSpellPower(spellLevel, entity) * 0.5f * (1 - distance / (radius * radius)));
                DamageSources.applyDamage(target, finalDamage, getDamageSource(entity));
                if (level instanceof ServerLevel serverLevel && finalLb != null && target instanceof Creeper creeper) {
                    creeper.thunderHit(serverLevel, finalLb);
                }
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.knockback(0.25f + finalDamage / 10f,
                            entity.getX() - livingTarget.getX(),
                            entity.getZ() - livingTarget.getZ());
                }
            }
        });
    }

    private void spawnFunnelBlast(Level level, int spellLevel, LivingEntity entity) {
        // 発射位置: ランダムな円周上 (ここをベースにします)
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        Vec3 lookX = entity.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 lookZ = new Vec3(-lookX.z, 0, lookX.x);

        // 基点となる目の高さを取得
        Vec3 eyeCenterPos = entity.getEyePosition().add(0, 1.5, 0);

        Vec3 spawnPos = eyeCenterPos
                .add(lookX.scale(Math.cos(angle) * FUNNEL_RADIUS))
                .add(lookZ.scale(Math.sin(angle) * FUNNEL_RADIUS));

        // ─── 🛠️ 【修正】視線の先（eyeTarget）を完全に排除 ───

        // ① プレイヤーの前方ベクトル（LookAngle）そのものをベースの射出向きにする
        Vec3 shotDir = entity.getLookAngle().normalize();

        // ② その「前方向き」に対して、設定されたSPREAD（ばらつき）によるややランダムな向きを直接加える
        shotDir = shotDir.add(
                (RANDOM.nextDouble() - 0.5) * SPREAD * 0.1,
                (RANDOM.nextDouble() - 0.5) * SPREAD * 0.1,
                (RANDOM.nextDouble() - 0.5) * SPREAD * 0.1
        ).normalize();

        // ─── 🛠️ ここまで ───

        Vec3 shotEnd = spawnPos.add(shotDir.scale(40f));

        // ブロック着弾判定
        HitResult blockHit = level.clip(new ClipContext(
                spawnPos, shotEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        Vec3 blockHitPos = blockHit.getLocation();
        double blockDist = blockHitPos.distanceToSqr(spawnPos);

        // エンティティ着弾判定
        LivingEntity hitEntity = null;
        double closestDist = blockDist;
        for (var candidate : level.getEntities(entity, new AABB(spawnPos, shotEnd).inflate(1.0))) {
            if (!(candidate instanceof LivingEntity living)) continue;
            AABB box = living.getBoundingBox().inflate(0.3);
            var entityHit = box.clip(spawnPos, shotEnd);
            if (entityHit.isPresent()) {
                double dist = entityHit.get().distanceToSqr(spawnPos);
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = living;
                }
            }
        }

        // 実際の着弾点（手前に敵がいなければ、各砲撃の射線がぶつかったブロックの表面座標になる）
        final LivingEntity finalHitEntity = hitEntity;
        Vec3 actualHitPos = (finalHitEntity != null)
                ? finalHitEntity.position().add(0, finalHitEntity.getBbHeight() / 2, 0)
                : blockHitPos;

        // ビジュアルエフェクト（各砲撃位置から、計算されたそれぞれの衝突位置へビームを伸ばす）
        level.addFreshEntity(new EldritchBlastVisualEntity(level, spawnPos, actualHitPos, entity));

        if (!level.isClientSide) {
            // 発射音
            level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                    SoundRegistry.ELDRITCH_BLAST.get(),
                    SoundSource.PLAYERS,
                    1.5f,
                    0.8f + RANDOM.nextFloat() * 0.4f);

            // 直撃ダメージ
            if (finalHitEntity != null) {
                DamageSources.applyDamage(finalHitEntity, getSpellPower(spellLevel, entity) * 0.3f, getDamageSource(entity));
            }

            // 着弾点付近の範囲ダメージ（爆風）
            level.getEntities(entity, new AABB(
                    actualHitPos.x - HIT_RADIUS, actualHitPos.y - HIT_RADIUS, actualHitPos.z - HIT_RADIUS,
                    actualHitPos.x + HIT_RADIUS, actualHitPos.y + HIT_RADIUS, actualHitPos.z + HIT_RADIUS
            )).forEach(target -> {
                if (target instanceof LivingEntity livingTarget && target != finalHitEntity) {
                    double dist = livingTarget.distanceToSqr(actualHitPos.x, actualHitPos.y, actualHitPos.z);
                    if (dist < HIT_RADIUS * HIT_RADIUS) {
                        DamageSources.applyDamage(livingTarget, getSpellPower(spellLevel, entity) * 0.15f, getDamageSource(entity));
                    }
                }
            });

            // 💥 【完全一致】それぞれの砲撃の射線がブロックや敵に遮られたその衝突点（actualHitPos）で爆発
            float explosionStrength = getSpellPower(spellLevel, entity) * 0.05f;
            level.explode(
                    entity,
                    getDamageSource(entity),
                    null,
                    actualHitPos.x, actualHitPos.y, actualHitPos.z,
                    explosionStrength,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            // パーティクルを着弾点に出す
            MagicManager.spawnParticles(level, ParticleHelper.UNSTABLE_ENDER,
                    actualHitPos.x, actualHitPos.y, actualHitPos.z,
                    20, HIT_RADIUS * 0.5, HIT_RADIUS * 0.5, HIT_RADIUS * 0.5, .05, false);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getSpellPower(spellLevel, caster) * 0.3f, 1))
        );
    }
}