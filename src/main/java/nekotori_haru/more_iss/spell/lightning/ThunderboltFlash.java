package nekotori_haru.more_iss.spell.lightning;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import nekotori_haru.more_iss.util.DelayedLanceScheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ThunderboltFlash extends AbstractSpell {

    // ───────── 定数 ─────────
    /** 円に並べるランスの本数 */
    private static final int LANCE_COUNT = 8;
    /** 円の半径（ブロック） */
    private static final float ORBIT_RADIUS = 2.5f;
    /** 空中静止するtick数（20tick = 1秒） */
    private static final int HOVER_TICKS = 20;
    /** 1本ずつ発射する間隔（tick） */
    private static final int FIRE_INTERVAL_TICKS = 3;

    // ───────── ID / Config ─────────
    private final ResourceLocation spellId =
            new ResourceLocation("more_iss", "raisen");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(40)
            .build();

    public ThunderboltFlash() {
        this.baseSpellPower     = 25;
        this.spellPowerPerLevel = 5;
        this.castTime           = 40;
        this.baseManaCost       = 100;
        this.manaCostPerLevel = 75;
    }

    // ───────── AbstractSpell オーバーライド ─────────

    @Override public CastType getCastType()            { return CastType.LONG; }
    @Override public DefaultConfig getDefaultConfig()  { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    /**
     * 詠唱前のターゲット指定。
     * Utils.preCastTargetHelper が TargetEntityCastData を MagicData にセットする。
     */
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel,
                                          LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 64, 0.15f);
    }

    /**
     * 詠唱完了時の処理。
     * サーバーサイドのみ: ランスの Blueprint を円形に並べ、
     * DelayedLanceScheduler に登録して静止演出→1本ずつ発射させる。
     */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity,
                       CastSource castSource, MagicData playerMagicData) {

        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {

            LivingEntity target = targetData.getTarget(serverLevel);
            buildCircleAndSchedule(serverLevel, spellLevel, entity, target);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // ───────── プライベートメソッド ─────────

    /**
     * LANCE_COUNT 本分の LanceBlueprint を縦向き半円状に生成し、
     * DelayedLanceScheduler に登録する。
     *
     * 配置イメージ（詠唱者の正面を向いて見た図）:
     *
     *      5  4  3
     *    6        2
     *    7        1
     *      8  9  0   ← 足元付近から上に向かって弧を描く
     *
     * 詠唱者の正面方向を法線とした平面上の半円（下から上へ）
     */
    private void buildCircleAndSchedule(ServerLevel level, int spellLevel,
                                        LivingEntity caster, LivingEntity target) {

        float damage = getSpellPower(spellLevel, caster);

        // 詠唱者の目線の中心点
        Vec3 center = caster.getEyePosition();

        // 詠唱者の正面方向（水平成分のみ、正規化）
        Vec3 forward = new Vec3(
                Math.cos(Math.toRadians(caster.getYRot() + 90)),
                0,
                Math.sin(Math.toRadians(caster.getYRot() + 90))
        ).normalize();

        // 正面に垂直な水平軸（右方向）
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        // 縦軸
        Vec3 up = new Vec3(0, 1, 0);

        List<DelayedLanceScheduler.LanceBlueprint> blueprints = new ArrayList<>();

        for (int i = 0; i < LANCE_COUNT; i++) {
            // 半円: 0°（右下）→ 180°（左下）を下から上へ
            // angleRad: 0 = 右側, π/2 = 真上, π = 左側
            double angleRad = Math.PI * i / (LANCE_COUNT - 1);

            // 右方向と上方向の合成で縦半円上の点を計算
            double rightComponent = Math.cos(angleRad) * ORBIT_RADIUS;   // 水平（右→左）
            double upComponent    = Math.sin(angleRad) * ORBIT_RADIUS;   // 垂直（下→上→下）

            Vec3 spawnPos = center
                    .add(right.scale(rightComponent))
                    .add(up.scale(upComponent));

            Vec3 fallback = caster.getLookAngle();
            blueprints.add(new DelayedLanceScheduler.LanceBlueprint(spawnPos, fallback));

            // 召喚時の初期パーティクル
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    10, 0.2, 0.2, 0.2, 0.05);
        }

        // 召喚音
        level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS,
                0.6f, 1.8f);

        // スケジューラに登録
        DelayedLanceScheduler.schedule(
                level, caster, blueprints, target,
                HOVER_TICKS, FIRE_INTERVAL_TICKS, damage);
    }
}