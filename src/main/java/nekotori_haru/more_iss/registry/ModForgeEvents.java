package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.More_iss;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent; // ⭕ インポート確認
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = More_iss.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModForgeEvents {

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) { // ⭕ LivingUpdateEventからLivingTickEventに修正
        // サーバー側のプレイヤーインスタンスのみを対象に個別に処理
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {

            // 負荷対策として5チック（0.25秒）に1回だけ実行
            if (player.tickCount % 5 != 0) {
                return;
            }

            // 1. 各個別属性（RegistryObject）から .get() を用いてAttributeを取り出し、バフ増加量を合計
            double totalPowerBonus = 0.0;
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.FIRE_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.ICE_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.LIGHTNING_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.HOLY_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.BLOOD_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.EVOCATION_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.ENDER_SPELL_POWER.get());
            totalPowerBonus += getPowerOffset(player, AttributeRegistry.NATURE_SPELL_POWER.get());

            // 2. プレイヤー自身の「融合魔法威力」のベース値に、基本値(1.0) + 合計ボーナスを同期
            AttributeInstance synthesisPowerInstance = player.getAttribute(ModAttributes.SYNTHESIS_SPELL_POWER.get());
            if (synthesisPowerInstance != null) {
                synthesisPowerInstance.setBaseValue(1.0D + totalPowerBonus);
            }

            // 3. 耐性（レジスト）側も同様に .get() を通して合計して同期
            double totalResistBonus = 0.0;
            totalResistBonus += getResistOffset(player, AttributeRegistry.FIRE_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.ICE_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.LIGHTNING_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.HOLY_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.BLOOD_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.EVOCATION_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.ENDER_MAGIC_RESIST.get());
            totalResistBonus += getResistOffset(player, AttributeRegistry.NATURE_MAGIC_RESIST.get());

            AttributeInstance synthesisResistInstance = player.getAttribute(ModAttributes.SYNTHESIS_MAGIC_RESIST.get());
            if (synthesisResistInstance != null) {
                synthesisResistInstance.setBaseValue(0.0D + totalResistBonus);
            }
        }
    }

    // 属性の「現在の実数値」が「基本値」からどれだけ増えているかを算出
    private static double getPowerOffset(Player player, Attribute attribute) {
        if (attribute == null) return 0.0D;
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null ? (instance.getValue() - instance.getBaseValue()) : 0.0D;
    }

    // 耐性用のヘルパー
    private static double getResistOffset(Player player, Attribute attribute) {
        if (attribute == null) return 0.0D;
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null ? (instance.getValue() - instance.getBaseValue()) : 0.0D;
    }
}