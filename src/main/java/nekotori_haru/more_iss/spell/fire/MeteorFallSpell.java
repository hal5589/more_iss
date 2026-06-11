package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class MeteorFallSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "meteor_fall");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(60)
            .setAllowCrafting(true)
            .build();

    public MeteorFallSpell() {
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.manaCostPerLevel = 15;
        this.baseManaCost = 30;
        this.castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", getDurationSeconds(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.aoe", Utils.stringTruncation(getEffectRadius(spellLevel, caster), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.GENERIC_EXPLODE);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof MeteorFallCastData)) {
            Vec3 targetArea = Utils.moveToRelativeGroundLevel(world, RaycastBuilder.begin(world, entity)
                    .range(40)
                    .checkForBlocks(true)
                    .build()
                    .getLocation(), 8);
            // 継続時間を計算して保存
            int duration = getCalculatedDuration(spellLevel, entity);
            playerMagicData.setAdditionalCastData(new MeteorFallCastData(targetArea, duration));
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData == null || !(playerMagicData.getAdditionalCastData() instanceof MeteorFallCastData castData)) {
            return;
        }

        float radius = getEffectRadius(spellLevel, entity);
        int elapsed = castData.getElapsedTicks();
        castData.setElapsedTicks(elapsed + 1);

        // 継続時間を超えたら終了
        if (elapsed >= castData.getDuration()) {
            playerMagicData.setAdditionalCastData(null);
            return;
        }

        int tick = playerMagicData.getCastDurationRemaining() - 1;

        // 定期的にターゲットを更新
        if (tick % 40 == 0) {
            castData.updateTrackedEntities(level.getEntities(entity,
                    AABB.ofSize(castData.center, radius * 2, radius, radius * 2),
                    e -> e instanceof LivingEntity && !DamageSources.isFriendlyFireBetween(entity, e)));
        }

        // メテオ召喚（tick % 4 == 0）
        if (tick % 4 == 0) {
            Vec3 center = castData.center;
            Vec3 weightedArea = Vec3.ZERO;

            if (!castData.trackedEntities.isEmpty()) {
                for (Entity target : castData.trackedEntities) {
                    weightedArea = weightedArea.add(target.position().subtract(center).scale(1f / castData.trackedEntities.size()));
                }
            }

            // メテオ数: 2レベルごとに+1（レベル1:1個, レベル2:1個, レベル3:2個）
            int meteorCount = 1 + (spellLevel - 1) / 2;

            for (int i = 0; i < meteorCount; i++) {
                spawnMeteor(level, spellLevel, entity, castData, radius);
            }
        }
    }

    private void spawnMeteor(Level level, int spellLevel, LivingEntity entity, MeteorFallCastData castData, float radius) {
        Vec3 center = castData.center;

        Vec3 weightedArea = Vec3.ZERO;
        if (!castData.trackedEntities.isEmpty()) {
            for (Entity target : castData.trackedEntities) {
                weightedArea = weightedArea.add(target.position().subtract(center).scale(1f / castData.trackedEntities.size()));
            }
        }

        float spawnRadius = radius * (0.5f + entity.getRandom().nextFloat() * 0.5f);
        double angle = entity.getRandom().nextDouble() * 2 * Math.PI;
        double offsetX = Math.cos(angle) * spawnRadius * entity.getRandom().nextDouble();
        double offsetZ = Math.sin(angle) * spawnRadius * entity.getRandom().nextDouble();

        // 高さ30～40ブロックから落下
        Vec3 spawnPos = center.add(offsetX + weightedArea.x * 0.5, 0, offsetZ + weightedArea.z * 0.5);
        spawnPos = Utils.moveToRelativeGroundLevel(level, spawnPos, 6).add(0, 30 + entity.getRandom().nextDouble() * 10, 0);

        Vec3 targetPos = Utils.moveToRelativeGroundLevel(level, spawnPos, 2);
        Vec3 trajectory = targetPos.subtract(spawnPos).normalize();

        MagicFireball fireball = new MagicFireball(level, entity);
        fireball.setPos(spawnPos);
        fireball.setDamage(getDamage(spellLevel, entity));
        fireball.setExplosionRadius(getRadius(spellLevel, entity));
        fireball.shoot(trajectory);
        fireball.setDeltaMovement(trajectory.scale(1.5));
        level.addFreshEntity(fireball);

        MagicManager.spawnParticles(level, ParticleTypes.FLAME, spawnPos.x, spawnPos.y, spawnPos.z, 20, 0.8, 0.8, 0.8, 0.1, false);
        MagicManager.spawnParticles(level, ParticleHelper.FIRE, spawnPos.x, spawnPos.y, spawnPos.z, 30, 1.0, 1.0, 1.0, 0.15, true);

        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                2.5f, 0.6f + entity.getRandom().nextFloat() * 0.3f);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 5 + (spellLevel * 2) + getSpellPower(spellLevel, caster) * 0.5f;
    }

    private float getRadius(int spellLevel, LivingEntity caster) {
        return 3.0f + spellLevel * 0.5f;
    }

    private float getEffectRadius(int spellLevel, LivingEntity caster) {
        return 12.0f + spellLevel * 3.0f;
    }

    private int getDurationSeconds(int spellLevel, LivingEntity caster) {
        return getCalculatedDuration(spellLevel, caster) / 20;
    }

    private int getCalculatedDuration(int spellLevel, LivingEntity caster) {
        // 基本100tick + レベル×20tick + スペルパワー×10tick
        int levelBonus = spellLevel * 20;
        int spellPowerBonus = (int) (getSpellPower(spellLevel, caster) * 10);
        return this.castTime + levelBonus + spellPowerBonus;
    }

    // キャストデータクラス
    public static class MeteorFallCastData implements ICastData {
        private final Vec3 center;
        private final List<Entity> trackedEntities = new ArrayList<>();
        private final int duration;
        private int elapsedTicks = 0;

        public MeteorFallCastData(Vec3 center, int duration) {
            this.center = center;
            this.duration = duration;
        }

        @Override
        public void reset() {
            trackedEntities.clear();
        }

        public void updateTrackedEntities(List<Entity> entities) {
            trackedEntities.clear();
            trackedEntities.addAll(entities);
        }

        public int getElapsedTicks() {
            return elapsedTicks;
        }

        public void setElapsedTicks(int ticks) {
            this.elapsedTicks = ticks;
        }

        public int getDuration() {
            return duration;
        }
    }
}