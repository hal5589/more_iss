package nekotori_haru.more_iss.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import nekotori_haru.more_iss.More_iss;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(More_iss.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 既存のパケット (サーバー -> クライアント)
        CHANNEL.messageBuilder(PacketSyncCraftingAnim.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncCraftingAnim::new)
                .encoder(PacketSyncCraftingAnim::toBytes)
                .consumerMainThread(PacketSyncCraftingAnim::handle)
                .add();

        // ⭕ 追加：クラフト完了パケット (クライアント -> サーバー)
        CHANNEL.messageBuilder(PacketCraftComplete.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketCraftComplete::new)
                .encoder(PacketCraftComplete::toBytes)
                .consumerMainThread(PacketCraftComplete::handle)
                .add();

        // ⭕ 追加：ボスバー進捗同期パケット (サーバー -> クライアント)
        CHANNEL.messageBuilder(PacketBossBarSync.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketBossBarSync::new)
                .encoder(PacketBossBarSync::toBytes)
                .consumerMainThread(PacketBossBarSync::handle)
                .add();
    }
}