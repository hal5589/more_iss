package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import nekotori_haru.more_iss.api.BeamType;
import nekotori_haru.more_iss.entity.BeamWarningEntity;
import nekotori_haru.more_iss.registry.ModEntities;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 七色の檻 (Seven Colored Cage)
 *
 * 詠唱者を中心とした半径10ブロックの円、詠唱者の頭上+5ブロックの高さの平面上に
 * ランダムな点を取り、そこから真下に向けて自作レイ(FLAME/HOLY/SOLAR/SPECTRAL)を
 * ランダムにばらまく弾幕スペル。VoidRayは射程や貫通の仕様が大きく異なるため対象外。
 *
 * - CONTINUOUS(詠唱中ずっとonServerCastTickが呼ばれる)型
 * - 詠唱時間はレベルに応じて lv1=20tick 〜 max lv=60tick まで連続的に増加
 * - 詠唱中、2tickごとに5本の予告線(BeamWarningEntity)を同時召喚する
 * - 各予告線は自身の寿命(20tick)が切れた時点で、自分自身の中で本体ビーム
 *   (BaseBeamVisualEntity表示 + ダメージ + ヒット効果)を発生させる
 * - 各ビームのヒット効果は元のスペル(FlameRaySpell等)のものをそのまま再現する
 *   (実際の再現処理はBeamWarningEntity側に実装している)
 * - 詠唱者・チームメイト・詠唱者が所有する召喚モブにはダメージが入らない
 *   (AllyUtilsによる判定。実際の判定はBeamWarningEntity側で行う)
 */
@AutoSpellConfig
public class SevenColoredCageSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("more_iss", "seven_colored_cage");

    private static final Random RANDOM = new Random();

    private static final double SPAWN_RADIUS = 10.0;       // 詠唱者中心の円の半径
    private static final double SPAWN_HEIGHT_OFFSET = 5.0; // 詠唱者からの頭上の高さ
    private static final double BEAM_MAX_DISTANCE = 64.0;  // レイの距離上限(地面に当たらなかった場合の保険)
    private static final int BEAM_INTERVAL_TICKS = 2;       // 何tickごとに5本セットを出すか
    private static final int BEAMS_PER_SET = 5;

    private static final int MIN_CAST_TIME = 20; // lv1
    private static final int MAX_CAST_TIME = 60; // max level

    // 使用するビーム種別(VoidRayは除外)
    private static final BeamType[] USABLE_BEAM_TYPES = {
            BeamType.FLAME, BeamType.HOLY, BeamType.SOLAR, BeamType.SPECTRAL
    };

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
                .setMaxLevel(1)
                .setCooldownSeconds(60)
                .build();
    }

    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    /**
     * レベルに応じて詠唱時間を lv1=20tick 〜 maxLevel=60tick の間で線形補間する。
     */
    @Override
    public int getCastTime(int spellLevel) {
        int maxLevel = Math.max(1, getMaxLevel());
        if (maxLevel <= 1) return MIN_CAST_TIME;

        int clampedLevel = Math.max(1, Math.min(spellLevel, maxLevel));
        float t = (clampedLevel - 1) / (float) (maxLevel - 1);
        return Math.round(MIN_CAST_TIME + (MAX_CAST_TIME - MIN_CAST_TIME) * t);
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
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData == null) return;

        // 2tickごとに5本セットを召喚する(ワールドの絶対tickで間引くことで安定して2tick周期にする)
        if (level.getGameTime() % BEAM_INTERVAL_TICKS != 0) return;

        for (int i = 0; i < BEAMS_PER_SET; i++) {
            spawnBeamWarning(level, spellLevel, entity);
        }
    }

    /**
     * 1本分の予告線を生成する。本体ビームの発火・ダメージ・ヒット効果は
     * BeamWarningEntity自身が20tick後に行う。
     */
    private void spawnBeamWarning(Level level, int spellLevel, LivingEntity entity) {
        if (level.isClientSide) return;

        // 1. 発射開始点(頭上の平面上のランダムな点)を決定。円内に均一分布させる
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(RANDOM.nextDouble()) * SPAWN_RADIUS;

        double startX = entity.getX() + Math.cos(angle) * r;
        double startZ = entity.getZ() + Math.sin(angle) * r;
        double startY = entity.getY() + SPAWN_HEIGHT_OFFSET;
        Vec3 startPos = new Vec3(startX, startY, startZ);

        Vec3 direction = new Vec3(0, -1, 0); // 常に真下固定。一度決めたら追従しない

        // 2. ブロックに当たる地点までの距離を計算(財産破壊なし・距離上限あり)
        Vec3 maxEndPos = startPos.add(direction.scale(BEAM_MAX_DISTANCE));
        HitResult blockHit = level.clip(new ClipContext(
                startPos, maxEndPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        double distance = blockHit.getLocation().distanceTo(startPos);
        if (distance <= 0.01 || distance > BEAM_MAX_DISTANCE) distance = BEAM_MAX_DISTANCE;

        // 3. ビーム種別をランダムに選択(VoidRay除外)
        BeamType beamType = USABLE_BEAM_TYPES[RANDOM.nextInt(USABLE_BEAM_TYPES.length)];

        // 4. ダメージ量を確定(各レイの既存ダメージ計算式 3 + spellPower*1.5 をそのまま使用)
        float damage = 3 + getSpellPower(spellLevel, entity) * 1.5f;

        // 5. 予告線を生成。20tick後にBeamWarningEntity自身が本体ビームを発火する
        BeamWarningEntity warning = new BeamWarningEntity(
                ModEntities.BEAM_WARNING.get(), level, entity, startPos, direction, distance,
                beamType, damage, spellLevel);
        level.addFreshEntity(warning);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = 3 + getSpellPower(spellLevel, caster) * 1.5f;
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage, 1))
        );
    }
}
