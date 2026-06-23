package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.api.BeamType;
import nekotori_haru.more_iss.entity.BeamWarningEntity;
import nekotori_haru.more_iss.registry.ModEntities;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import nekotori_haru.more_iss.util.AllyUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class SevenColoredCageSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("more_iss", "seven_colored_cage");

    private static final Random RANDOM = new Random();

    private static final double SPAWN_RADIUS = 10.0;
    private static final double SPAWN_HEIGHT_OFFSET = 10.0;
    private static final double BEAM_MAX_DISTANCE = 64.0;
    private static final int BEAM_INTERVAL_TICKS = 2;
    private static final int BEAMS_PER_SET = 5;

    private static final int BASE_CAST_TIME = 20;
    private static final int CAST_TIME_PER_LEVEL = 4;

    // ⭐ 周囲のentity(味方以外)の頭上から発生させる確率
    private static final float ENTITY_TARGET_CHANCE = 0.05f;

    // ⭐ 詠唱開始時のみ使用
    private static final SoundEvent SOUND_WINDUP = SoundRegistry.SUNBEAM_WINDUP.get();

    private static final BeamType[] USABLE_BEAM_TYPES = {
            BeamType.FLAME, BeamType.HOLY, BeamType.SOLAR, BeamType.SPECTRAL
    };

    private boolean hasPlayedSound = false;

    public SevenColoredCageSpell() {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 100;
    }

    @Override
    public CastType getCastType() { return CastType.CONTINUOUS; }

    @Override
    public DefaultConfig getDefaultConfig() {
        return new DefaultConfig()
                .setMinRarity(SpellRarity.LEGENDARY)
                .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
                .setMaxLevel(10)
                .setCooldownSeconds(60)
                .build();
    }

    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public int getCastTime(int spellLevel) {
        int maxLevel = getMaxLevel();
        int clampedLevel = Math.max(1, Math.min(spellLevel, maxLevel));
        return BASE_CAST_TIME + (clampedLevel - 1) * CAST_TIME_PER_LEVEL;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
        // ⭐ 詠唱開始時に1回だけ音を鳴らす
        if (!level.isClientSide) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SOUND_WINDUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        hasPlayedSound = false;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData == null) return;

        if (level.getGameTime() % BEAM_INTERVAL_TICKS != 0) return;

        for (int i = 0; i < BEAMS_PER_SET; i++) {
            spawnBeamWarning(level, spellLevel, entity);
        }
    }

    private void spawnBeamWarning(Level level, int spellLevel, LivingEntity entity) {
        if (level.isClientSide) return;

        double startX;
        double startZ;

        // ⭐ 5%の確率で、周囲(SPAWN_RADIUS以内)の味方以外のentityの頭上から発生させる
        LivingEntity targetedEntity = null;
        if (RANDOM.nextFloat() < ENTITY_TARGET_CHANCE) {
            targetedEntity = pickRandomNearbyTarget(level, entity);
        }

        if (targetedEntity != null) {
            startX = targetedEntity.getX();
            startZ = targetedEntity.getZ();
        } else {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(RANDOM.nextDouble()) * SPAWN_RADIUS;
            startX = entity.getX() + Math.cos(angle) * r;
            startZ = entity.getZ() + Math.sin(angle) * r;
        }

        double startY = entity.getY() + SPAWN_HEIGHT_OFFSET;
        Vec3 startPos = new Vec3(startX, startY, startZ);

        Vec3 direction = new Vec3(0, -1, 0);

        Vec3 maxEndPos = startPos.add(direction.scale(BEAM_MAX_DISTANCE));
        HitResult blockHit = level.clip(new ClipContext(
                startPos, maxEndPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        double distance = blockHit.getLocation().distanceTo(startPos);
        if (distance <= 0.01 || distance > BEAM_MAX_DISTANCE) distance = BEAM_MAX_DISTANCE;

        BeamType beamType = USABLE_BEAM_TYPES[RANDOM.nextInt(USABLE_BEAM_TYPES.length)];

        float damage = 3 + getSpellPower(spellLevel, entity) * 1.5f;

        BeamWarningEntity warning = new BeamWarningEntity(
                ModEntities.BEAM_WARNING.get(), level, entity, startPos, direction, distance,
                beamType, damage, spellLevel);
        level.addFreshEntity(warning);
    }

    /**
     * SPAWN_RADIUS以内にいる、詠唱者の味方ではないLivingEntityをランダムに1体選ぶ。
     * 該当するentityがいない場合はnullを返す。
     */
    @Nullable
    private LivingEntity pickRandomNearbyTarget(Level level, LivingEntity caster) {
        AABB searchBox = caster.getBoundingBox().inflate(SPAWN_RADIUS);
        List<LivingEntity> candidates = new java.util.ArrayList<>();

        for (Entity candidate : level.getEntities(caster, searchBox)) {
            if (!(candidate instanceof LivingEntity living)) continue;
            if (!living.isAlive()) continue;
            if (AllyUtils.isAlly(caster, living)) continue;
            if (caster.distanceTo(living) > SPAWN_RADIUS) continue;
            candidates.add(living);
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = 3 + getSpellPower(spellLevel, caster) * 1.5f;
        int castTime = getCastTime(spellLevel);
        float castSeconds = castTime / 20.0f;
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage, 1)),
                Component.literal("詠唱時間: " + String.format("%.1f", castSeconds) + "秒")
        );
    }
}