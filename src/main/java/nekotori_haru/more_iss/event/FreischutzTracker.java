package nekotori_haru.more_iss.event;

// 💡 独自に作成した全耐性・イベント貫通ユーティリティをインポート
import nekotori_haru.more_iss.util.DisintegrationDamageUtil;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = More_iss.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FreischutzTracker {

    // 現在「報い」デバフを受けているプレイヤーのUUIDを監視するリスト
    private static final Set<UUID> sinners = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // サーバー側のStartフェーズのみで処理を行う（二重処理とクライアント側での誤作動を防止）
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide) return;

        LivingEntity entity = event.player;
        UUID uuid = entity.getUUID();

        // 1. プレイヤーが「報い」デバフを持っている場合
        if (entity.hasEffect(ModEffects.RETRIBUTION.get())) {
            MobEffectInstance effect = entity.getEffect(ModEffects.RETRIBUTION.get());

            // 💡 効果時間が残り1Tick（実質0秒）になったら自然消滅の合図
            // 即死の誤発動を防ぐため、事前にリストから安全に除外して無罪放免にする
            if (effect != null && effect.getDuration() <= 1) {
                sinners.remove(uuid);
            } else {
                // 通常のデバフ継続中は監視リストに登録・維持
                sinners.add(uuid);
            }
        }
        // 2. プレイヤーが「報い」デバフを持っていない場合
        else {
            // リストに名前が残っているのにデバフがない ＝ まだ時間があったのに不正解除（牛乳・コマンド等）された証拠
            if (sinners.contains(uuid)) {
                sinners.remove(uuid);

                // 対象が生きているなら、問答無用で悪魔の制裁を執行
                if (entity.isAlive()) {
                    executeAbsoluteDeath(entity);
                }
            }
        }
    }

    /**
     * 悪魔の制裁（新システム：DisintegrationDamageUtilによる完全データ書き換え執行）
     */
    private static void executeAbsoluteDeath(LivingEntity entity) {
        // 💡 召喚した最強の貫通ユーティリティを発動！
        // 対象プレイヤーに、全耐性・全軽減をスキップするダメージ（Float.MAX_VALUE）を無敵無視（true）で叩き込む
        DisintegrationDamageUtil.dealTrueDamage(
                entity,
                entity.level().damageSources().fellOutOfWorld(), // ダメージソースは奈落（身代わり系防止）
                Float.MAX_VALUE,
                true
        );

        // プレイヤーに規約違反ペナルティメッセージを送信
        entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.illegal_remove_penalty"));
    }
}