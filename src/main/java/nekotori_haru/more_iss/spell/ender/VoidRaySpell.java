package nekotori_haru.more_iss.spell.ender;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class VoidRaySpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "void_ray");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    public VoidRaySpell() {
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
        float maxRange = getRange(spellLevel, entity);

        // ブロックを透過（checkForBlocks: false）
        var hitResult = RaycastBuilder.begin(level, entity)
                .range(maxRange)
                .checkForBlocks(false)
                .bbInflation(.15f)
                .build();

        Vec3 startPos = entity.getEyePosition().subtract(0, 0.15, 0);
        Vec3 direction = entity.getLookAngle();

        // 🌟 射程60に合わせて、何も当たらなかった場合は最大60mまでビームを描画
        double distance = maxRange;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            distance = hitResult.getLocation().distanceTo(startPos);
        }

        if (!level.isClientSide) {
            BaseBeamVisualEntity visual = new BaseBeamVisualEntity(
                    ModEntities.BASE_BEAM_VISUAL.get(), level, entity,
                    startPos, direction, distance, BeamType.VOID);
            level.addFreshEntity(visual);
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            double x = hitResult.getLocation().x;
            double y = hitResult.getLocation().y;
            double z = hitResult.getLocation().z;

            MagicManager.spawnParticles(level, ParticleTypes.PORTAL, x, y, z, 25, 0.2, 0.2, 0.2, 0.5, false);
            MagicManager.spawnParticles(level, ParticleTypes.DRAGON_BREATH, x, y, z, 10, 0.15, 0.15, 0.15, 0.03, false);

            Entity target = ((EntityHitResult) hitResult).getEntity();
            if (target instanceof LivingEntity livingTarget) {
                DamageSources.applyDamage(target, getDamage(spellLevel, entity), getDamageSource(entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // 🌟 射程を 60.0f に拡張
    public static float getRange(int level, LivingEntity caster) { return 60.0f; }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 3 + getSpellPower(spellLevel, caster) * 1.5f;
    }
}