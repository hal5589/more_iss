package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.util.DisintegrationState;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;

public class DisintegrationSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(More_iss.MODID, "disintegration");

    public static final ResourceKey<DamageType> DISINTEGRATION_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(More_iss.MODID, "disintegration"));

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60)
            .setAllowCrafting(false)
            .build();

    // 詠唱開始時の多重実行を防ぐための内部フラグ
    private boolean hasStartedCasting = false;

    public DisintegrationSpell() {
        this.baseManaCost = Integer.MAX_VALUE;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 0;
        this.castTime = 60 * 20; // 60秒 * 20tick (超大魔法のタメ)
    }

    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public DefaultConfig getDefaultConfig() { return defaultConfig; }

    @Override
    public CastType getCastType() { return CastType.LONG; }

    /**
     * ISS純正ターゲットロックシステム
     */
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 15, 0.35f);
    }

    /**
     * 詠唱（チャージ）中、サーバーサイドで毎tick実行されるメソッド。
     * フラグ管理により、ISSのバージョンアップによる引数変更の影響を100%回避します。
     */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);

        if (level.isClientSide()) return;

        // まだ詠唱開始のフリーズ処理を走らせていない（最初の1tick目）場合のみ実行
        if (!hasStartedCasting) {
            hasStartedCasting = true; // 即座にフラグを立てて、2tick目以降の重複呼び出しをガード

            // 詠唱開始時にクロスヘアで捉えていたターゲットを取得
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                LivingEntity target = castTargetingData.getTarget((ServerLevel) level);

                if (target != null && target.isAlive()) {
                    // 【詠唱フェーズ開始】あなた(entity)を中心に、半径10マスのモブを完全固定＋首固定＋じわじわ1マスの高さへ浮遊
                    DisintegrationState.startCastingPhase(entity, target, 10.0D);

                    if (entity instanceof Player player) {
                        player.sendSystemMessage(Component.literal("§5[崩壊の予兆] §d周囲の因果を固定、重力を剥奪します..."));
                    }
                }
            }
        }
    }

    /**
     * 詠唱フェーズが終了した際（指を離して中断された、または無事発動して完了した）に自動で呼ばれる。
     */
    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean wasInterrupted) {
        if (!level.isClientSide()) {
            DisintegrationState.stopCastingPhase(); // 固定・浮遊を全面解除して通常重力に戻す
            this.hasStartedCasting = false;        // フラグをリセットし、次回の詠唱に備える
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, wasInterrupted);
    }

    /**
     * 60秒の詠唱を耐えきり、ついに魔法が完成・発動した瞬間
     */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide()) return;

        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
            LivingEntity target = castTargetingData.getTarget((ServerLevel) level);

            if (target != null && target.isAlive()) {
                // 【本番開始】対象を中心に、30秒間（600tick）の崩壊ダメージフェーズ（指数関数True Damage）を執行
                DisintegrationState.startDamagePhase(target.getUUID(), entity.getUUID(), 600, 5.0f, (ServerLevel) level);

                if (entity instanceof Player player) {
                    player.sendSystemMessage(Component.literal("§c§l[事象崩壊] §d理の消失を執行。消滅を待ちなさい。"));
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}