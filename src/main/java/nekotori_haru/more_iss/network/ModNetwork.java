package nekotori_haru.more_iss.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("more_iss", "arcane_crafting"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++,
                PacketSyncCraftingAnim.class,
                PacketSyncCraftingAnim::encode,
                PacketSyncCraftingAnim::decode,
                PacketSyncCraftingAnim::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    /** 指定座標の半径range内にいる全プレイヤーへパケット送信 */
    public static void sendToNearbyPlayers(Object packet, Level level, BlockPos pos, double range) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // パケット配信用インポートの代わりに完全修飾名か、手動で import net.minecraftforge.network.PacketDistributor; を追加してください
        for (ServerPlayer player : serverLevel.players()) {
            double dx = player.getX() - pos.getX();
            double dy = player.getY() - pos.getY();
            double dz = player.getZ() - pos.getZ();
            if (dx*dx + dy*dy + dz*dz < range * range) {
                // PacketDistributor.PLAYER.with(() -> player) を使用して送信
                CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
