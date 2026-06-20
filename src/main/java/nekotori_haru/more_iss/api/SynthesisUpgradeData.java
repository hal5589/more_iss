package nekotori_haru.more_iss.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class SynthesisUpgradeData {
    // ⭐ Iron's Spells のアップグレードデータタグに合わせる
    private static final String UPGRADE_DATA_TAG = "irons_spellbooks:upgrade_data";
    private static final String UPGRADES_KEY = "upgrades";
    private static final String SYNTHESIS_POWER_KEY = "more_iss:synthesis_power";

    private static final String TAG_UPGRADED = "SynthesisUpgraded";
    private static final String TAG_UPGRADE_COUNT = "SynthesisUpgradeCount";
    private static final int MAX_UPGRADES = 3;
    private static final double BONUS_PER_UPGRADE = 0.05; // +5%

    /**
     * Iron's Spells のアップグレードデータをチェックして、
     * 合成アップグレードが含まれているかを判定する
     */
    public static boolean isUpgraded(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();

        // 方法1: Iron's Spells のアップグレードデータをチェック
        if (tag.contains(UPGRADE_DATA_TAG)) {
            CompoundTag upgradeData = tag.getCompound(UPGRADE_DATA_TAG);
            if (upgradeData.contains(UPGRADES_KEY)) {
                CompoundTag upgrades = upgradeData.getCompound(UPGRADES_KEY);
                if (upgrades.contains(SYNTHESIS_POWER_KEY)) {
                    return true;
                }
            }
        }

        // 方法2: 従来の独自タグもチェック（後方互換性）
        return tag.getBoolean(TAG_UPGRADED);
    }

    public static int getUpgradeCount(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        CompoundTag tag = stack.getTag();

        // Iron's Spells のアップグレードデータからカウントを取得
        if (tag.contains(UPGRADE_DATA_TAG)) {
            CompoundTag upgradeData = tag.getCompound(UPGRADE_DATA_TAG);
            if (upgradeData.contains(UPGRADES_KEY)) {
                CompoundTag upgrades = upgradeData.getCompound(UPGRADES_KEY);
                if (upgrades.contains(SYNTHESIS_POWER_KEY)) {
                    return upgrades.getInt(SYNTHESIS_POWER_KEY);
                }
            }
        }

        // 従来の独自タグ
        return tag.getInt(TAG_UPGRADE_COUNT);
    }

    public static boolean canUpgrade(ItemStack stack) {
        return getUpgradeCount(stack) < MAX_UPGRADES;
    }

    public static void applyUpgrade(ItemStack stack) {
        // このメソッドは Iron's Spells のアップグレードシステムを使用する場合、
        // 実際のアップグレード適用は AnvilSynthesisHandler 側で行うため、
        // ここでは従来の独自タグも設定する（後方互換用）
        CompoundTag tag = stack.getOrCreateTag();
        int current = tag.getInt(TAG_UPGRADE_COUNT);
        tag.putInt(TAG_UPGRADE_COUNT, current + 1);
        tag.putBoolean(TAG_UPGRADED, true);
    }

    public static double getTotalBonus(ItemStack stack) {
        return getUpgradeCount(stack) * BONUS_PER_UPGRADE;
    }
}