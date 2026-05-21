package nekotori_haru.more_iss.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;

import java.util.function.Supplier;

public class PacketSyncCraftingAnim {
    private final BlockPos pos;
    private final int craftingTick;
    private final boolean isActive;

    public PacketSyncCraftingAnim(BlockPos pos, int craftingTick, boolean isActive) {
        this.pos = pos;
        this.craftingTick = craftingTick;
        this.isActive = isActive;
    }

    public PacketSyncCraftingAnim(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.craftingTick = buf.readInt();
        this.isActive = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(craftingTick);
        buf.writeBoolean(isActive);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.level != null) {
                    var be = minecraft.level.getBlockEntity(pos);
                    if (be instanceof ArcaneCraftingTableBlockEntity table) {
                        table.setCraftingAnimFromPacket(craftingTick, isActive);
                    }
                }
            });
        });
        return true;
    }
}