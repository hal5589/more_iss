package nekotori_haru.more_iss.network;

import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCraftComplete {
    private final BlockPos pos;

    public PacketCraftComplete(BlockPos pos) {
        this.pos = pos;
    }

    public PacketCraftComplete(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // サーバー側での処理
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof ArcaneCraftingTableBlockEntity acbe) {
                    // 先ほどBlockEntityに追加した完了メソッドを実行！
                    acbe.executeCraftCompletion();
                }
            }
        });
        return true;
    }
}