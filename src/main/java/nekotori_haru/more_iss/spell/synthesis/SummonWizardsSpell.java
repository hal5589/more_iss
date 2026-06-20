package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.summoned.*;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SummonWizardsSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "summon_wizards");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(300) // 5分
            .setAllowCrafting(true)
            .build();

    public SummonWizardsSpell() {
        this.manaCostPerLevel = 100;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 80; // 長めの詠唱
        this.baseManaCost = 400;
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
        return Optional.of(SoundRegistry.ENDER_CAST.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int count = getSummonCount(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", count),
                Component.translatable("ui.more_iss.summon_wizards.desc")
        );
    }

    public int getSummonCount(int spellLevel, LivingEntity caster) {
        return 5; // 常に5体
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

    private void applySummonStats(LivingEntity summon, LivingEntity caster, float powerMultiplier, float healthBoost, float attackBoost) {
        // 体力強化
        float baseHealth = summon.getMaxHealth();
        float boostedHealth = baseHealth * powerMultiplier * healthBoost;
        summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(boostedHealth);
        summon.setHealth(boostedHealth);

        // 攻撃力強化
        AttributeInstance attackAttr = summon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            float baseAttack = 3.0f;
            attackAttr.setBaseValue(baseAttack * powerMultiplier * attackBoost);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) return;

        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)) {
            SummonedEntitiesCastData summonedEntitiesCastData = new SummonedEntitiesCastData();
            int summonTime = 20 * 60 * 10; // 10分

            float powerMultiplier = getPowerMultiplier(entity, spellLevel);
            float healthBoost = 1.0f + (spellLevel * 0.1f);
            float attackBoost = 1.0f + (spellLevel * 0.1f);

            // 召喚位置のリスト（半径2.5ブロックの円上に配置）
            Vec3 center = entity.position();
            float radius = 2.5f;
            float yOffset = 0.5f;

            // 5体の魔術師を召喚
            List<LivingEntity> summons = new ArrayList<>();

            // 1. 紅蓮術師 (Pyromancer)
            SummonedPyromancer pyromancer = new SummonedPyromancer(level, entity);
            Vec3 pos1 = center.add(radius * 1.0, yOffset, 0);
            pyromancer.setPos(pos1);
            applySummonStats(pyromancer, entity, powerMultiplier, healthBoost, attackBoost);
            summons.add(pyromancer);

            // 2. 氷魔術師 (Cryomancer)
            SummonedCryomancer cryomancer = new SummonedCryomancer(level, entity);
            Vec3 pos2 = center.add(-radius * 0.809f, yOffset, radius * 0.588f);
            cryomancer.setPos(pos2);
            applySummonStats(cryomancer, entity, powerMultiplier, healthBoost, attackBoost);
            summons.add(cryomancer);

            // 3. 司祭 (Priest)
            SummonedPriest priest = new SummonedPriest(level, entity);
            Vec3 pos3 = center.add(-radius * 0.809f, yOffset, -radius * 0.588f);
            priest.setPos(pos3);
            applySummonStats(priest, entity, powerMultiplier, healthBoost, attackBoost);
            summons.add(priest);

            // 4. アーキヴォーカー (Archevoker)
            SummonedArchevoker archevoker = new SummonedArchevoker(level, entity);
            Vec3 pos4 = center.add(radius * 0.309f, yOffset, radius * 0.951f);
            archevoker.setPos(pos4);
            applySummonStats(archevoker, entity, powerMultiplier, healthBoost, attackBoost);
            summons.add(archevoker);

            // 5. 薬剤師 (Apothecarist)
            SummonedApothecarist apothecarist = new SummonedApothecarist(level, entity);
            Vec3 pos5 = center.add(radius * 0.309f, yOffset, -radius * 0.951f);
            apothecarist.setPos(pos5);
            applySummonStats(apothecarist, entity, powerMultiplier, healthBoost, attackBoost);
            summons.add(apothecarist);

            // 全ての召喚物をワールドに追加
            for (LivingEntity summon : summons) {
                level.addFreshEntity(summon);
                SummonManager.initSummon(entity, summon, summonTime, summonedEntitiesCastData);
            }

            // 再詠唱データを登録
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