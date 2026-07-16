package nekotori_haru.more_iss.spell.nature.yaezakura;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nekotori_haru.more_iss.registry.ModSounds;

import java.util.Optional;

@AutoSpellConfig
public class YaezakuraSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "yaezakura");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(new ResourceLocation("irons_spellbooks", "nature"))
            .setMaxLevel(7)
            .setCooldownSeconds(12)
            .build();

    public YaezakuraSpell() {
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 10;
        this.baseManaCost = 15;
        this.manaCostPerLevel = 5;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
        return 0;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.YAEZAKURA_SET.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(ModSounds.YAEZAKURA_ATTACK.get());
    }

    private float getDistance(int spellLevel, LivingEntity caster) {
        return 7f + (spellLevel - 1) * 2f + getSpellPower(spellLevel, caster) * 0.05f;
    }

    private float getDamagePerHit(int spellLevel, LivingEntity caster) {
        float totalDamage = (7f + (spellLevel - 1) * 2f) + getSpellPower(spellLevel, caster) * 0.3f;
        return totalDamage / 5f;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData playerMagicData) {
        if (level.isClientSide) return;

        // 移動先計算
        float distance = getDistance(spellLevel, caster);
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = start.add(look.scale(distance));

        BlockHitResult hit = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            end = hit.getLocation().subtract(look.scale(0.5));
        }
        end = new Vec3(end.x, caster.getY(), end.z);

        // 分割テレポート
        Vec3 oldPos = caster.position();
        final int MOVE_STEPS = 5;
        Vec3 stepVec = end.subtract(oldPos).scale(1.0 / MOVE_STEPS);

        for (int i = 0; i < MOVE_STEPS; i++) {
            Vec3 newPos = oldPos.add(stepVec.scale(i + 1));
            caster.teleportTo(newPos.x, newPos.y, newPos.z);
            if (level instanceof ServerLevel server) {
                spawnStepParticles(server, newPos);
            }
        }
        caster.teleportTo(end.x, end.y, end.z);
        caster.fallDistance = 0;

        // ★ ダメージ範囲を 1.5 ブロックに縮小
        double radius = 1.0;
        AABB damageBox = new AABB(
                Math.min(oldPos.x, end.x) - radius,
                Math.min(oldPos.y, end.y) - radius,
                Math.min(oldPos.z, end.z) - radius,
                Math.max(oldPos.x, end.x) + radius,
                Math.max(oldPos.y, end.y) + radius,
                Math.max(oldPos.z, end.z) + radius
        );

        float damagePerHit = getDamagePerHit(spellLevel, caster);
        YaezakuraDashHandler.scheduleDelayedDamage(
                caster,
                damageBox,
                damagePerHit,
                20,   // 初回遅延
                1,    // 間隔
                5,    // ヒット回数
                oldPos, // ★ 開始位置
                end     // ★ 終了位置
        );

        if (level instanceof ServerLevel server) {
            spawnPathParticles(server, oldPos, end);
        }

        super.onCast(level, spellLevel, caster, source, playerMagicData);
    }

    // ---- パーティクル ----

    private void spawnStepParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.CHERRY_LEAVES,
                pos.x, pos.y + 0.3, pos.z,
                2,
                0.1, 0.1, 0.1,
                0.05
        );
    }

    private void spawnPathParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        Vec3 step = dir.normalize().scale(0.4);

        for (double d = 0; d < len; d += 0.4) {
            Vec3 pos = start.add(step.scale(d / 0.4));
            level.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    pos.x, pos.y + 0.3, pos.z,
                    3,
                    0.2, 0.1, 0.2,
                    0.1
            );
        }
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }
}