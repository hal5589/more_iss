package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import io.redspace.ironsspellbooks.util.ParticleHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

import java.util.ArrayList;
import java.util.List;

@AutoSpellConfig
public class InfernoStepSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            new ResourceLocation("more_iss", "inferno_step");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(40)
            .build();

    public InfernoStepSpell() {
        this.baseSpellPower     = 25;
        this.spellPowerPerLevel = 5;
        this.castTime           = 0;
        this.baseManaCost       = 100;
        this.manaCostPerLevel   = 75;
    }

    private static final int MOVE_STEPS = 10;          // 移動分割数
    private static final double ANCHOR_INTERVAL = 0.8; // 壁のアンカーポイント間隔（ブロック）

    // 本家 WallOfFireSpell と同じベースダメージ
    private static final float BASE_FIRE_DAMAGE = 4.0f;
    private static final float FIRE_DAMAGE_PER_LEVEL = 1.0f;

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    // リキャストは使わない（0回）
    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 0;
    }

    // ===== UI表示 =====
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(getDistance(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 1))
        );
    }

    // ===== 射程・ダメージ計算 =====
    private float getDistance(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 0.4f;  // レベル1:10, レベル3:14
    }

    // ダメージは本家 WallOfFire と完全に同じベース値を使用（装備補正なし）
    private float getDamage(int spellLevel, LivingEntity caster) {
        return BASE_FIRE_DAMAGE + (spellLevel - 1) * FIRE_DAMAGE_PER_LEVEL;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {

        if (level.isClientSide) return;

        // ターゲット位置を計算
        float distance = getDistance(spellLevel, caster);
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = start.add(look.scale(distance));

        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        Vec3 target = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().subtract(look.scale(0.5))
                : end;

        Vec3 oldPos = caster.position();

        // ===== 経路上のアンカーポイントを収集（地面に補正） =====
        List<Vec3> anchorPoints = new ArrayList<>();
        Vec3 dir = target.subtract(oldPos);
        double totalLen = dir.length();
        Vec3 stepVec = dir.normalize().scale(ANCHOR_INTERVAL);

        // 始点と終点を含めて一定間隔でポイントを取得
        for (double d = 0; d <= totalLen; d += ANCHOR_INTERVAL) {
            Vec3 point = oldPos.add(stepVec.scale(d / ANCHOR_INTERVAL));
            Vec3 groundPoint = setOnGround(point, level);
            anchorPoints.add(groundPoint);
        }
        // 終点を必ず含める
        Vec3 finalGround = setOnGround(target, level);
        if (!anchorPoints.contains(finalGround)) {
            anchorPoints.add(finalGround);
        }

        // ===== 高速移動（分割テレポート） =====
        Vec3 moveStep = target.subtract(oldPos).scale(1.0 / MOVE_STEPS);
        for (int i = 0; i < MOVE_STEPS; i++) {
            Vec3 newPos = oldPos.add(moveStep.scale(i + 1));
            caster.teleportTo(newPos.x, newPos.y, newPos.z);
            // 移動中の炎パーティクル
            if (level instanceof ServerLevel server) {
                spawnFireParticles(server, newPos);
            }
        }
        caster.teleportTo(target.x, target.y, target.z);
        caster.fallDistance = 0;

        // ===== 炎の壁を生成 =====
        if (anchorPoints.size() >= 2) {
            WallOfFireEntity wall = new WallOfFireEntity(level, caster, anchorPoints, getDamage(spellLevel, caster));
            // 壁の中心座標を設定（アンカーの平均）
            Vec3 center = Vec3.ZERO;
            for (Vec3 p : anchorPoints) center = center.add(p);
            center = center.scale(1.0 / anchorPoints.size());
            wall.setPos(center);
            level.addFreshEntity(wall);
        }

        // ===== 軌跡の炎パーティクル（ライン状） =====
        if (level instanceof ServerLevel server) {
            spawnFireTrail(server, oldPos, target);
        }

        super.onCast(level, spellLevel, caster, source, data);
    }

    // ---- 炎のパーティクル（移動ステップ用） ----
    private void spawnFireParticles(ServerLevel level, Vec3 pos) {
        MagicManager.spawnParticles(level,
                ParticleTypes.FLAME,
                pos.x, pos.y + 0.5, pos.z,
                5,
                0.2, 0.2, 0.2,
                0.05,
                false);
        MagicManager.spawnParticles(level,
                ParticleHelper.FIRE,
                pos.x, pos.y + 0.5, pos.z,
                3,
                0.15, 0.15, 0.15,
                0.02,
                false);
    }

    // ---- 軌跡の炎パーティクル（ライン状） ----
    private void spawnFireTrail(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        Vec3 step = dir.normalize().scale(0.3);

        for (double d = 0; d < len; d += 0.3) {
            Vec3 pos = start.add(step.scale(d / 0.3));
            level.addParticle(ParticleTypes.FLAME,
                    pos.x, pos.y + 0.3, pos.z,
                    0, 0, 0);
            MagicManager.spawnParticles(level,
                    ParticleTypes.FLAME,
                    pos.x, pos.y + 0.3, pos.z,
                    2,
                    0.05, 0.05, 0.05,
                    0.02,
                    false);
            MagicManager.spawnParticles(level,
                    ParticleHelper.FIRE,
                    pos.x, pos.y + 0.3, pos.z,
                    1,
                    0.1, 0.1, 0.1,
                    0.01,
                    false);
        }
    }

    // ---- 地面の高さに補正（WallOfFireSpellから流用） ----
    private Vec3 setOnGround(Vec3 in, Level level) {
        BlockPos pos = new BlockPos((int) in.x, (int) in.y, (int) in.z);
        // 少し上から下に向かって地面を探す
        if (level.getBlockState(pos.above()).isAir()) {
            for (int i = 0; i < 15; i++) {
                BlockPos check = pos.below(i);
                if (!level.getBlockState(check).isAir()) {
                    return new Vec3(in.x, check.getY() + 1, in.z);
                }
            }
            // 見つからなければ元の高さ
            return new Vec3(in.x, in.y - 15, in.z);
        } else {
            // 現在ブロック内なら、一番上の地面を取得
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            return new Vec3(in.x, y, in.z);
        }
    }
}