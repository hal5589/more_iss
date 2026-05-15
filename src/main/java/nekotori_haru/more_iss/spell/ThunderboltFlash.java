package nekotori_haru.more_iss.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

@AutoSpellConfig
public class ThunderboltFlash extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("more_iss", "raisen");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(15)
            .build();

    public ThunderboltFlash() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 40; // 2秒の重厚な溜め
        this.baseManaCost = 150;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 8; // 全8発
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 64, .15f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            var recasts = playerMagicData.getPlayerRecasts();

            // 初回発動時：自動連射のリキャストインスタンスを登録
            if (!recasts.hasRecastForSpell(getSpellId())) {
                // 第4引数の "4" が連射間隔（4チック = 0.2秒間隔）
                recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), 4, castSource, targetData), playerMagicData);
            }

            // 実際の射出ロジック
            if (!level.isClientSide) {
                LivingEntity target = targetData.getTarget((ServerLevel) level);
                // ターゲットが生きていれば射出、死んでいれば詠唱者の向きに飛ばす
                shootOneLance(level, spellLevel, entity, target, playerMagicData);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void shootOneLance(Level level, int spellLevel, LivingEntity entity, LivingEntity target, MagicData playerMagicData) {
        var recastInstance = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
        if (recastInstance == null) return;

        int total = getRecastCount(spellLevel, entity);
        int remaining = recastInstance.getRemainingRecasts();
        int currentIndex = total - remaining;

        // 円形配置の計算
        double radian = Math.toRadians((360.0 / total) * currentIndex);
        double radius = 2.5;
        // プレイヤーの背後に円形に並べる
        Vec3 offset = new Vec3(Math.cos(radian) * radius, Math.sin(radian) * radius, -1.0)
                .yRot((float) Math.toRadians(-entity.getYRot()));
        Vec3 spawnPos = entity.getEyePosition().add(offset);

        LightningLanceProjectile lance = new LightningLanceProjectile(level, entity);
        lance.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        lance.setDamage(getSpellPower(spellLevel, entity));

        // ターゲットへの方向
        Vec3 direction;
        if (target != null && target.isAlive()) {
            direction = target.getBoundingBox().getCenter().subtract(spawnPos).normalize();
            lance.setHomingTarget(target);
        } else {
            direction = entity.getLookAngle();
        }

        lance.shoot(direction.scale(1.2)); // 弾速を少し遅くして「迫り来る」感を出す
        level.addFreshEntity(lance);

        // 演出：射出時の電気パーティクルと音
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, spawnPos.x, spawnPos.y, spawnPos.z, 8, 0.2, 0.2, 0.2, 0.1);
            // 軽い雷の音を鳴らす
            level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.world.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }

    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
}