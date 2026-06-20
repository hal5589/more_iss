package nekotori_haru.more_iss.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * 味方判定の共通ヘルパー。
 * 「七色の檻」のように、特定のターゲットを選ばず範囲にダメージを振りまく
 * スペルで、詠唱者自身・チームメイト・詠唱者が所有する召喚モブを
 * 誤って攻撃しないようにするために使う。
 */
public final class AllyUtils {

    private AllyUtils() {}

    /**
     * casterの視点で、targetが「味方」（=ダメージを与えるべきでない対象）かどうかを判定する。
     *
     * - target が caster 自身 → 味方
     * - 両者が Player で、同じチーム（PlayerTeam）に所属している → 味方
     * - target が OwnableEntity（召喚モブ等）で、その所有者が caster である → 味方
     * - target が OwnableEntity で、その所有者が caster と同チームの Player である → 味方
     *
     * @param caster 詠唱者（攻撃の発生源）
     * @param target ダメージ対象の候補
     * @return 味方であればtrue（ダメージを与えてはいけない）
     */
    public static boolean isAlly(LivingEntity caster, Entity target) {
        if (caster == null || target == null) return false;
        if (target == caster) return true;

        // Player同士のチーム判定
        if (caster instanceof Player casterPlayer && target instanceof Player targetPlayer) {
            Team casterTeam = casterPlayer.getTeam();
            if (casterTeam != null && casterTeam.equals(targetPlayer.getTeam())) {
                return true;
            }
        }

        // 召喚モブ等(OwnableEntity)の所有者判定
        if (target instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                if (owner == caster) return true;

                // 所有者がcasterとチームメイトのPlayerである場合も味方扱い
                if (caster instanceof Player casterPlayer && owner instanceof Player ownerPlayer) {
                    Team casterTeam = casterPlayer.getTeam();
                    if (casterTeam != null && casterTeam.equals(ownerPlayer.getTeam())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * isAllyの否定。"このtargetにダメージを与えてよいか"を直接表す。
     */
    public static boolean isValidTarget(LivingEntity caster, Entity target) {
        return !isAlly(caster, target);
    }
}