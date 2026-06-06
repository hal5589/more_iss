package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.MarkOfDetonationProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class MarkOfDetonationSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation(More_iss.MODID, "mark_of_detonation");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(5)
            .setAllowCrafting(true)
            .build();

    public MarkOfDetonationSpell() {
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.manaCostPerLevel = 20;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(level, caster), 1))
        );
    }

    private float getDamage(int level, LivingEntity caster) {
        return this.getSpellPower(level, caster);
    }

    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public void onCast(Level world, int level, LivingEntity entity, CastSource source, MagicData data) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            MarkOfDetonationProjectile projectile = new MarkOfDetonationProjectile(serverWorld, entity);
            projectile.setPos(entity.getEyePosition().subtract(0, 0.1, 0));
            projectile.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 1.5F, 0.0F);
            projectile.setDamage(getDamage(level, entity));
            world.addFreshEntity(projectile);
        }
        super.onCast(world, level, entity, source, data);
    }
}