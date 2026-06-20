package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.StarEntity;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class StarlightSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "starlight");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(true)
            .build();

    public StarlightSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.manaCostPerLevel = 0;
        this.baseManaCost = 100;
        this.castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getStarCount(spellLevel, caster))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override
    public CastType getCastType() { return CastType.LONG; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            int count = getStarCount(spellLevel, caster);
            float damage = getDamage(spellLevel, caster);
            float radius = getRadius(spellLevel, caster);

            for (int i = 0; i < count; i++) {
                // 【修正】水平方向をメインに、わずかに下方向へも散布
                double angle = world.random.nextDouble() * Math.PI * 2;

                // 水平速度: メイン
                double horizontalSpeed = 0.3 + world.random.nextDouble() * 0.6;

                // 垂直速度: 0～-0.3 (下方向へ)
                double verticalSpeed = -(world.random.nextDouble() * 0.3);

                Vec3 velocity = new Vec3(
                        Math.cos(angle) * horizontalSpeed,
                        verticalSpeed,
                        Math.sin(angle) * horizontalSpeed
                );

                StarEntity star = new StarEntity(world, caster);

                // キャスターの足元付近～少し上に配置
                double offsetX = (world.random.nextFloat() - 0.5f) * 1.5;
                double offsetZ = (world.random.nextFloat() - 0.5f) * 1.5;
                double offsetY = 0.5 + world.random.nextFloat() * 0.5; // キャスターの足元から腰あたり

                star.setPos(
                        caster.getX() + offsetX,
                        caster.getY() + offsetY,
                        caster.getZ() + offsetZ
                );
                star.setDeltaMovement(velocity);
                star.setDamage(damage);
                star.setExplosionRadius(radius);
                world.addFreshEntity(star);
            }

            // 詠唱時のエフェクト
            MagicManager.spawnParticles(serverWorld, ParticleTypes.GLOW,
                    caster.getX(), caster.getY() + 0.5, caster.getZ(),
                    30, 2.0, 0.5, 2.0, 0.1D, false);

            MagicManager.spawnParticles(serverWorld, ParticleTypes.END_ROD,
                    caster.getX(), caster.getY() + 0.5, caster.getZ(),
                    20, 1.5, 0.3, 1.5, 0.1D, false);
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 8.0f + 4.0f * getSpellPower(spellLevel, caster);
    }

    public float getRadius(int spellLevel, LivingEntity caster) {
        return 4.0f + getSpellPower(spellLevel, caster) * 0.5f;
    }

    public int getStarCount(int spellLevel, LivingEntity caster) {
        return 6 + (int) (getSpellPower(spellLevel, caster) * 2);
    }
}