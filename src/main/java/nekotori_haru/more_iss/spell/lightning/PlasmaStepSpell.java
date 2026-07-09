package nekotori_haru.more_iss.spell.lightning;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.List;

public class PlasmaStepSpell extends AbstractSpell {

    private final ResourceLocation spellId =
            new ResourceLocation("more_iss", "plasma_step");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(40)   // 全体クールダウン
            .build();

    public PlasmaStepSpell() {
        this.baseSpellPower     = 25;
        this.spellPowerPerLevel = 5;
        this.castTime           = 0;
        this.baseManaCost       = 100;
        this.manaCostPerLevel   = 75;
    }

    private static final float RADIUS = 1.5f;
    private static final int MAX_RECAST = 3;
    private static final int MOVE_STEPS = 10;  // 分割数（多いほど滑らか）

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

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return MAX_RECAST;
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

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 0.3f;  // レベル1:7.5, レベル3:10.5
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {

        if (level.isClientSide) return;

        // Recast 処理（持続時間40tick = 2秒）
        var recasts = data.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(getSpellId())) {
            recasts.addRecast(new RecastInstance(
                    getSpellId(),
                    spellLevel,
                    getRecastCount(spellLevel, caster),
                    40,
                    source,
                    null
            ), data);
        }

        // 射程を動的に計算
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

        // ===== 高速移動（分割テレポート） =====
        Vec3 stepVec = target.subtract(oldPos).scale(1.0 / MOVE_STEPS);
        for (int i = 0; i < MOVE_STEPS; i++) {
            Vec3 newPos = oldPos.add(stepVec.scale(i + 1));
            caster.teleportTo(newPos.x, newPos.y, newPos.z);
            // 各ステップでパーティクルを発生
            if (level instanceof ServerLevel server) {
                spawnStepParticles(server, newPos);
            }
        }
        // 最終位置を確実に合わせる
        caster.teleportTo(target.x, target.y, target.z);
        caster.fallDistance = 0;

        // ===== 経路ダメージ =====
        dealPathDamage(level, caster, oldPos, target, spellLevel);

        // 全体の軌跡パーティクル（追加でライン状に）
        if (level instanceof ServerLevel server) {
            spawnLightning(server, oldPos, target);
        }

        super.onCast(level, spellLevel, caster, source, data);
    }

    // ステップごとのパーティクル（単発）
    private void spawnStepParticles(ServerLevel level, Vec3 pos) {
        MagicManager.spawnParticles(level,
                ParticleHelper.ELECTRICITY,
                pos.x, pos.y, pos.z,
                3,
                0.1, 0.1, 0.1,
                0.2,
                false);
        MagicManager.spawnParticles(level,
                ParticleHelper.ELECTRIC_SPARKS,
                pos.x, pos.y, pos.z,
                2,
                0.15, 0.15, 0.15,
                0.1,
                false);
    }

    // 既存の軌跡パーティクル（ライン状）
    private void spawnLightning(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        Vec3 step = dir.normalize().scale(0.3);

        for (double d = 0; d < len; d += 0.3) {
            Vec3 pos = start.add(step.scale(d / 0.3));
            level.addParticle(ParticleHelper.ELECTRICITY,
                    pos.x, pos.y, pos.z,
                    0, 0, 0);
            MagicManager.spawnParticles(level,
                    ParticleHelper.ELECTRICITY,
                    pos.x, pos.y, pos.z,
                    3,
                    0.05, 0.05, 0.05,
                    0.2,
                    false);
            MagicManager.spawnParticles(level,
                    ParticleHelper.ELECTRIC_SPARKS,
                    pos.x, pos.y, pos.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.1,
                    false);
        }
    }

    // 経路ダメージ（ノックバックなし）
    private void dealPathDamage(Level level, LivingEntity caster, Vec3 start, Vec3 end, int spellLevel) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        Vec3 step = dir.normalize().scale(0.5);
        float damage = getDamage(spellLevel, caster);

        for (double d = 0; d < len; d += 0.5) {
            Vec3 pos = start.add(step.scale(d / 0.5));

            AABB box = new AABB(
                    pos.x - RADIUS, pos.y - RADIUS, pos.z - RADIUS,
                    pos.x + RADIUS, pos.y + RADIUS, pos.z + RADIUS
            );

            List<LivingEntity> list = level.getEntitiesOfClass(
                    LivingEntity.class,
                    box,
                    e -> e != caster
            );

            for (LivingEntity e : list) {
                if (caster instanceof Player player) {
                    e.hurt(caster.damageSources().playerAttack(player), damage);
                } else {
                    e.hurt(caster.damageSources().mobAttack(caster), damage);
                }

                // ノックバックなし

                // 着弾パーティクル
                if (level instanceof ServerLevel server) {
                    Vec3 entityPos = e.position();
                    double yOffset = e.getBbHeight() * 0.5;
                    MagicManager.spawnParticles(server,
                            ParticleHelper.ELECTRICITY,
                            entityPos.x, entityPos.y + yOffset, entityPos.z,
                            6,
                            0.2, 0.2, 0.2,
                            0.3,
                            false);
                    MagicManager.spawnParticles(server,
                            ParticleHelper.ELECTRIC_SPARKS,
                            entityPos.x, entityPos.y + yOffset, entityPos.z,
                            4,
                            0.3, 0.3, 0.3,
                            0.2,
                            false);
                }
            }
        }
    }
}