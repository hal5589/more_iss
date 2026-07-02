package nekotori_haru.more_iss.spell;

import io.redspace.ironsspellbooks.api.util.AnimationHolder;

/**
 * more_iss 独自の詠唱モーション定義。
 * AnimationHolder(path, playOnce, animatesLegs) の path は
 * "modid:ファイル名" で指定し、以下のファイルを参照する:
 *   assets/more_iss/player_animator/<ファイル名>.json  ← プレイヤー用(PlayerAnimator)
 *   assets/more_iss/animations/<ファイル名>.json        ← モブ用(GeckoLib、必要な場合)
 */
public class CustomAnimations {

    /**
     * CryoConvergenceSpell 用: 両手前方平行 → 2秒で90度の弧（震え付き）→ 右上/左下解放。
     * playOnce=false: 最終フレーム（解放ポーズ）で静止し続ける。
     * animatesLegs=false: 足は動かさない。
     */
    public static final AnimationHolder CRYO_CONVERGENCE_ARC =
            new AnimationHolder("more_iss:cryo_convergence_cast", false, false);
}