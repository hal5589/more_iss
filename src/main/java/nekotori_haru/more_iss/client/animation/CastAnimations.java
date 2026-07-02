package nekotori_haru.more_iss.client.animation;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import nekotori_haru.more_iss.More_iss;

import java.util.Map;
import java.util.WeakHashMap;

public class CastAnimations {

    public static final ResourceLocation CRYO_CONVERGENCE_CAST =
            ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "cryo_convergence_cast");

    // プレイヤーごとに専用レイヤーを一つだけ保持し、使い回す
    private static final Map<AbstractClientPlayer, ModifierLayer<IAnimation>> LAYERS = new WeakHashMap<>();

    private static ModifierLayer<IAnimation> getOrCreateLayer(AbstractClientPlayer player) {
        return LAYERS.computeIfAbsent(player, p -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(p);
            stack.addAnimLayer(1000, layer); // 優先度はお好みで調整
            return layer;
        });
    }

    /**
     * spellのクライアント側キャスト開始タイミングで呼び出す。
     */
    public static void playCryoConvergenceCast(AbstractClientPlayer player) {
        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(CRYO_CONVERGENCE_CAST);
        if (animation == null) {
            return; // 未登録 or リソース未読み込み
        }

        ModifierLayer<IAnimation> layer = getOrCreateLayer(player);
        layer.setAnimation(new KeyframeAnimationPlayer(animation));
    }

    /**
     * 詠唱がキャンセル・完了した際に呼び出す。
     */
    public static void stopCast(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = getOrCreateLayer(player);
        layer.setAnimation(null);
    }
}