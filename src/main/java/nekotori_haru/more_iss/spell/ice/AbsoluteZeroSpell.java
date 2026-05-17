package nekotori_haru.more_iss.spell.ice;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes; // 💡 属性インポートを追記
import net.minecraft.world.entity.monster.Enemy; // 💡 敵判定用のインポートを追記
import net.minecraft.world.level.Level;

import java.util.List;

@AutoSpellConfig
public class AbsoluteZeroSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation("more_iss", "absolute_zero");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(150)
            .build();

    public AbsoluteZeroSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 35;
        this.spellPowerPerLevel = 0;
        this.castTime = 100;
        this.baseManaCost = 600;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", "20"),
                Component.translatable("ui.more_iss.absolute_zero.ice_count", "40")
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() { return this.defaultConfig; }
    @Override
    public CastType getCastType() { return CastType.LONG; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public AnimationHolder getCastStartAnimation() { return SpellAnimations.CHARGE_RAISED_HAND; }
    @Override
    public AnimationHolder getCastFinishAnimation() { return SpellAnimations.TOUCH_GROUND_ANIMATION; }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            float radius = 20.0f;

            MagicManager.spawnParticles(serverLevel, new BlastwaveParticleOptions(SchoolRegistry.ICE.get().getTargetingColor(), radius), entity.getX(), entity.getY() + .165f, entity.getZ(), 1, 0, 0, 0, 0, true);
            MagicManager.spawnParticles(serverLevel, ParticleRegistry.SNOWFLAKE_PARTICLE.get(), entity.getX(), entity.getY() + .3f, entity.getZ(), 120, radius * 0.5, 0.2, radius * 0.5, 0.5, true);

            float shatterDamage = (getSpellPower(spellLevel, entity) / 3.0f);

            // ─── 氷の影の同心円配置ロジック ───
            float[] circles = {5.0f, 10.0f, 15.0f, 20.0f};
            int[] countsPerCircle = {4, 8, 12, 16};

            for (int c = 0; c < circles.length; c++) {
                float currentRadius = circles[c];
                int count = countsPerCircle[c];

                for (int i = 0; i < count; i++) {
                    double theta = (i * 2 * Math.PI) / count;
                    theta += (c * Math.PI / 8.0);

                    double x = entity.getX() + currentRadius * Math.cos(theta);
                    double z = entity.getZ() + currentRadius * Math.sin(theta);

                    double y = entity.getY();
                    BlockPos targetPos = new BlockPos((int)x, (int)y, (int)z);
                    for (int dy = 4; dy >= -4; dy--) {
                        if (!serverLevel.getBlockState(targetPos.above(dy)).isAir() && serverLevel.getBlockState(targetPos.above(dy + 1)).isAir()) {
                            y = targetPos.above(dy + 1).getY();
                            break;
                        }
                    }

                    FrozenHumanoid shadow = new FrozenHumanoid(serverLevel, entity);
                    shadow.setShatterDamage(shatterDamage);

                    int baseTimer = 40 + (c * 10);
                    shadow.setDeathTimer(baseTimer + serverLevel.random.nextInt(15));

                    shadow.setPos(x, y, z);

                    // 💡 修正：認識範囲属性（FOLLOW_RANGE）を0から16に引き上げ、モブに「実在する標的」と認識させる
                    var attributeInstance = shadow.getAttribute(Attributes.FOLLOW_RANGE);
                    if (attributeInstance != null) {
                        attributeInstance.setBaseValue(16.0);
                    }

                    serverLevel.addFreshEntity(shadow);

                    // 💡 追加：出現した瞬間、周囲の狭い範囲（半径6ブロック）の敵のターゲットをこの影に強制変更する
                    // これにより、出現直後から強烈なデコイ（挑発）として機能します
                    final double fx = x;
                    final double fy = y;
                    final double fz = z;
                    serverLevel.getEntities(shadow, shadow.getBoundingBox().inflate(6.0, 4.0, 6.0), (target) -> target instanceof Enemy)
                            .forEach(enemy -> {
                                if (enemy instanceof net.minecraft.world.entity.Mob mob) {
                                    mob.setTarget(shadow);
                                }
                            });

                    MagicManager.spawnParticles(serverLevel, ParticleHelper.SNOWFLAKE, x, y + 1.0, z, 10, 0.3, 0.5, 0.3, 0.02, false);
                }
            }

            // ─── 敵への直接ダメージとデバフ処理 ───
            float waveDamage = getSpellPower(spellLevel, entity);

            level.getEntities(entity, entity.getBoundingBox().inflate(radius, 6, radius), (target) ->
                    !DamageSources.isFriendlyFireBetween(target, entity) && Utils.hasLineOfSight(level, entity, target, true)
            ).forEach(target -> {
                if (target instanceof LivingEntity livingEntity && livingEntity.distanceToSqr(entity) < radius * radius) {
                    livingEntity.hurt(entity.damageSources().indirectMagic(entity, entity), waveDamage);
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                    livingEntity.addEffect(new MobEffectInstance(MobEffectRegistry.CHILLED.get(), 200, 0));
                    MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, livingEntity.getX(), livingEntity.getY() + livingEntity.getBbHeight() * .5f, livingEntity.getZ(), 40, livingEntity.getBbWidth() * .5f, livingEntity.getBbHeight() * .5f, livingEntity.getBbWidth() * .5f, .03, false);
                }
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}