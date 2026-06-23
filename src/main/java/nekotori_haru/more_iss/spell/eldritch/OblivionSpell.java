package nekotori_haru.more_iss.spell.eldritch;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OblivionSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "oblivion");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE) // ※スクール仮置き、要確認
            .setMaxLevel(6) // BlackHoleSpell（本体Legendary）を参考に仮設定
            .setCooldownSeconds(60)
            .setAllowCrafting(false)
            .build();

    // ターゲット捕捉のレイキャスト範囲
    private static final int TARGET_RANGE = 40;
    private static final float TARGET_HITBOX_INFLATION = 0.35f;

    // 持続時間係数（仮値・要バランス調整）
    private static final int BASE_DURATION_TICKS = 60;          // Lv1基本値 = 3秒
    private static final int DURATION_TICKS_PER_LEVEL = 20;     // レベル毎 +1秒
    private static final float DURATION_POWER_MULTIPLIER = 2f;  // 魔力(spellPower) 1につき +0.1秒

    public OblivionSpell() {
        this.manaCostPerLevel = 80;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 40; // 4秒詠唱
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int durationTicks = getDuration(spellLevel, caster);
        String secondsString = String.valueOf(durationTicks / 20.0);

        return List.of(
                Component.translatable("ui.more_iss.oblivion.duration", secondsString)
        );
    }

    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // 詠唱開始時にレイキャストでターゲットを確定し、TargetEntityCastDataとして保持
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, TARGET_RANGE, TARGET_HITBOX_INFLATION);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            LivingEntity target = null;

            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetEntityCastData) {
                target = targetEntityCastData.getTarget(serverLevel);
            }

            if (target != null && !target.isDeadOrDying() && ModEffects.OBLIVION != null) {
                int durationTicks = getDuration(spellLevel, entity);
                target.addEffect(new MobEffectInstance(
                        ModEffects.OBLIVION.get(),
                        durationTicks,
                        spellLevel - 1,
                        false,
                        true,
                        true
                ));
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * 持続時間 = レベル基本値 + 詠唱者の魔力(spellPower)補正
     * ※係数は仮値。実プレイでのバランス調整が前提。
     */
    public int getDuration(int spellLevel, @Nullable LivingEntity entity) {
        int baseTicks = BASE_DURATION_TICKS + (spellLevel - 1) * DURATION_TICKS_PER_LEVEL;
        float powerBonus = (entity != null) ? getSpellPower(spellLevel, entity) * DURATION_POWER_MULTIPLIER : 0f;
        return baseTicks + (int) powerBonus;
    }
}