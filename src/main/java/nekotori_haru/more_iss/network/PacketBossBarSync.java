package nekotori_haru.more_iss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import nekotori_haru.more_iss.client.ClientBossBarOverlay;

import java.util.function.Supplier;

/**
 * 専用ボスバー（虹色グラデーション）の進捗同期パケット (サーバー -> クライアント)
 *
 * entityId をキーにクライアント側で保持し、一定時間更新が来なければ
 * クライアント側のタイムアウト処理で自動的に消える設計（削除パケット不要）。
 *
 * ⭐ isHit フラグを追加: 殴られた時点で演出を発動させる
 */
public class PacketBossBarSync {
    private final int entityId;
    private final float progress; // 0.0〜1.0
    private final Component name;
    private final boolean isHit;  // ⭐ 殴られたフラグ

    // 通常コンストラクタ（isHit = false）
    public PacketBossBarSync(int entityId, float progress, Component name) {
        this(entityId, progress, name, false);
    }

    // ⭐ isHit 指定コンストラクタ
    public PacketBossBarSync(int entityId, float progress, Component name, boolean isHit) {
        this.entityId = entityId;
        this.progress = progress;
        this.name = name;
        this.isHit = isHit;
    }

    // デコードコンストラクタ
    public PacketBossBarSync(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.progress = buf.readFloat();
        this.name = buf.readComponent();
        this.isHit = buf.readBoolean();
    }

    // エンコード
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(progress);
        buf.writeComponent(name);
        buf.writeBoolean(isHit);
    }

    // ハンドラ
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientBossBarOverlay.updateBar(entityId, progress, name, isHit);
            });
        });
        return true;
    }
}