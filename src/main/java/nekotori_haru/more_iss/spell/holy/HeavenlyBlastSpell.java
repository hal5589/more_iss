package nekotori_haru.more_iss.spell.holy;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.sunbeam.SunbeamEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HeavenlyBlastSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath("more_iss", "heavenly_blast");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int totalBeams = getTotalBeamCount(spellLevel, caster);
        float radius = getRadius(spellLevel, caster);

        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.more_iss.heavenly_blast.beams", totalBeams).withStyle(ChatFormatting.YELLOW),
                Component.translatable("ui.more_iss.heavenly_blast.radius", Utils.stringTruncation(radius, 1)).withStyle(ChatFormatting.GREEN)
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public HeavenlyBlastSpell() {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 48, .5f, false);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 spawn = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
            spawn = castTargetingData.getTargetPosition((ServerLevel) level);
        }

        if (spawn == null) {
            HitResult raycast = Utils.raycastForEntity(level, entity, 48, true);
            if (raycast.getType() == HitResult.Type.ENTITY) {
                spawn = ((EntityHitResult) raycast).getEntity().position();
            } else {
                spawn = Utils.moveToRelativeGroundLevel(level, raycast.getLocation().subtract(entity.getForward().normalize()).add(0, 2, 0), 3, 18);
            }
        }

        float damage = getDamage(spellLevel, entity);
        float radius = getRadius(spellLevel, entity);
        int totalBeams = getTotalBeamCount(spellLevel, entity);

        if (!level.isClientSide) {
            // ⭕ AreaEffectCloud を継承した独自のタイマーエンティティを作って召喚する
            final Vec3 centerPos = spawn;
            AreaEffectCloud timerEntity = new AreaEffectCloud(level, centerPos.x, centerPos.y, centerPos.z) {
                private int beamsSpawned = 0;

                @Override
                public void tick() {
                    super.tick(); // 本来のエンティティとしての時間経過を走らせる

                    // 2ティック（0.1秒）ごとに処理、かつ目標の総本数に達するまで
                    if (this.tickCount % 2 == 0 && beamsSpawned < totalBeams) {
                        if (entity.isAlive()) {
                            // 半径内のランダム位置を算出
                            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                            double distance = entity.getRandom().nextDouble() * radius;
                            double offsetX = Math.cos(angle) * distance;
                            double offsetZ = Math.sin(angle) * distance;

                            Vec3 strikePos = new Vec3(centerPos.x + offsetX, centerPos.y, centerPos.z + offsetZ);
                            strikePos = Utils.moveToRelativeGroundLevel(this.level(), strikePos, 4, 10);

                            // サンビーム召喚
                            SunbeamEntity sunbeam = new SunbeamEntity(this.level());
                            sunbeam.setOwner(entity);
                            sunbeam.moveTo(strikePos);
                            sunbeam.setDamage(damage);
                            this.level().addFreshEntity(sunbeam);

                            // 演出音
                            this.level().playSound(null, sunbeam.blockPosition(), SoundRegistry.SUNBEAM_WINDUP.get(), SoundSource.NEUTRAL, 1.2f, 1.1f + entity.getRandom().nextFloat() * 0.2f);

                            beamsSpawned++;
                        }
                    }

                    // すべて落としきったらタイマーエンティティ自身を消滅させる
                    if (beamsSpawned >= totalBeams) {
                        this.discard();
                    }
                }
            };

            timerEntity.setRadius(0f); // パーティクル等のエフェクトは不要なので0に
            timerEntity.setDuration((totalBeams * 2) + 10); // 余裕を持たせた生存時間を設定
            timerEntity.setWaitTime(0);
            level.addFreshEntity(timerEntity);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return this.getSpellPower(spellLevel, entity) * 0.5f;
    }

    private float getRadius(int spellLevel, LivingEntity entity) {
        float baseRadius = 5.0f + (spellLevel - 1) * 0.5f;
        float powerModifier = this.getSpellPower(spellLevel, entity) / (float) (this.baseSpellPower + (spellLevel * this.spellPowerPerLevel));
        return baseRadius * Math.max(1.0f, powerModifier);
    }

    private int getDurationTicks(int spellLevel, LivingEntity entity) {
        int baseDurationTicks = 10 + (spellLevel - 1) * 4;
        float powerModifier = this.getSpellPower(spellLevel, entity) / (float) (this.baseSpellPower + (spellLevel * this.spellPowerPerLevel));
        return (int) (baseDurationTicks * Math.max(1.0f, powerModifier));
    }

    private int getTotalBeamCount(int spellLevel, LivingEntity entity) {
        int duration = getDurationTicks(spellLevel, entity);
        return Math.max(1, duration / 2);
    }
}