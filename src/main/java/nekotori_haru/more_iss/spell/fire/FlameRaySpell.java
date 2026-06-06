package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.api.BeamType;
import nekotori_haru.more_iss.entity.BaseBeamVisualEntity;
import nekotori_haru.more_iss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class FlameRaySpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "flame_ray");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    public FlameRaySpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var hitResult = RaycastBuilder.begin(level, entity)
                .range(getRange(spellLevel, entity))
                .checkForBlocks(true)
                .bbInflation(.15f)
                .build();

        double distance = hitResult.getLocation().distanceTo(entity.getEyePosition());

        if (!level.isClientSide) {
            BaseBeamVisualEntity visual = new BaseBeamVisualEntity(ModEntities.BASE_BEAM_VISUAL.get(), level, entity, distance, BeamType.FLAME);
            visual.setPos(entity.getX(), entity.getEyeY() - 0.2, entity.getZ());
            level.addFreshEntity(visual);
        }

        if (hitResult.getType() != HitResult.Type.MISS) {
            double x = hitResult.getLocation().x;
            double y = hitResult.getLocation().y;
            double z = hitResult.getLocation().z;

            MagicManager.spawnParticles(level, ParticleTypes.LAVA, x, y, z, 25, 0.15, 0.15, 0.15, 0.3, false);
            MagicManager.spawnParticles(level, ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.2, 0.2, 0.2, 0.05, false);
            MagicManager.spawnParticles(level, ParticleTypes.FLAME, x, y, z, 15, 0.2, 0.2, 0.2, 0.1, false);

            if (hitResult.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hitResult).getEntity();
                if (target instanceof LivingEntity livingTarget) {
                    DamageSources.applyDamage(target, getDamage(spellLevel, entity), getDamageSource(entity));
                    BeamType.FLAME.applyEffect(livingTarget, spellLevel);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static float getRange(int level, LivingEntity caster) { return 30.0f; }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 3 + getSpellPower(spellLevel, caster) * 1.5f;
    }
}