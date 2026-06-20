package nekotori_haru.more_iss.spell.evocation;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.summoned.SummonedArchevoker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SummonArchevokerSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "summon_archevoker");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(180)
            .setAllowCrafting(true)
            .build();

    public SummonArchevokerSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 40;
        this.baseManaCost = 80;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.EVOCATION_CAST.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float powerMultiplier = getPowerMultiplier(caster, spellLevel);
        float healthBoost = getHealthBoost(spellLevel);
        float attackBoost = getAttackBoost(spellLevel);
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", getSummonCount(spellLevel, caster)),
                Component.translatable("ui.more_iss.summon_power", String.format("%.0f", powerMultiplier * 100))
        );
    }

    public int getSummonCount(int spellLevel, LivingEntity caster) {
        return 1;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    private float getPowerMultiplier(LivingEntity caster, int spellLevel) {
        if (caster == null) return 1.0f;
        float spellPower = getSpellPower(spellLevel, caster);
        float levelBonus = 1.0f + (spellLevel * 0.1f);
        float baseMultiplier = 1.0f + (spellPower - 1.0f) * 0.5f;
        return baseMultiplier * levelBonus;
    }

    private float getHealthBoost(int spellLevel) {
        return 1.0f + (spellLevel * 0.1f);
    }

    private float getAttackBoost(int spellLevel) {
        return 1.0f + (spellLevel * 0.1f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)) {
            SummonedEntitiesCastData summonedEntitiesCastData = new SummonedEntitiesCastData();
            int summonTime = 20 * 60 * 10;

            Vec3 spawn = entity.position();
            Vec3 forward = entity.getForward().normalize().scale(1.5f);
            spawn = spawn.add(forward.x, 0.15f, forward.z);

            SummonedArchevoker archevoker = new SummonedArchevoker(level, entity);
            archevoker.setPos(spawn);

            float powerMultiplier = getPowerMultiplier(entity, spellLevel);
            float healthBoost = getHealthBoost(spellLevel);
            float attackBoost = getAttackBoost(spellLevel);

            float baseHealth = archevoker.getMaxHealth();
            float boostedHealth = baseHealth * powerMultiplier * healthBoost;
            archevoker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(boostedHealth);
            archevoker.setHealth(boostedHealth);

            AttributeInstance attackAttr = archevoker.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                float baseAttack = 3.0f;
                attackAttr.setBaseValue(baseAttack * powerMultiplier * attackBoost);
            }

            AttributeInstance spellPower = archevoker.getAttribute(AttributeRegistry.SPELL_POWER.get());
            if (spellPower != null) {
                float baseSpellPower = 1.0f;
                spellPower.setBaseValue(baseSpellPower * powerMultiplier);
            }

            level.addFreshEntity(archevoker);
            SummonManager.initSummon(entity, archevoker, summonTime, summonedEntitiesCastData);

            RecastInstance recastInstance = new RecastInstance(
                    this.getSpellId(),
                    spellLevel,
                    getRecastCount(spellLevel, entity),
                    summonTime,
                    castSource,
                    summonedEntitiesCastData
            );
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}