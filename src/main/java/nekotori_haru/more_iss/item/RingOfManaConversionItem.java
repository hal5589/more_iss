package nekotori_haru.more_iss.item;

import nekotori_haru.more_iss.menu.RingOfManaConversionContainer;
import nekotori_haru.more_iss.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class RingOfManaConversionItem extends Item implements ICurioItem {

    public static final int SLOT_COUNT = 16;
    public static final int MAX_STACK_PER_SLOT = 16;

    // ★ クールダウン時間（秒）は ManaConversionHandler と共有するため public static に
    public static final int COOLDOWN_SECONDS = 30;

    public RingOfManaConversionItem(Properties properties) {
        super(properties);
    }

    // ============================================================
    //  Capability プロバイダ
    // ============================================================

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ItemStackHandlerProvider(stack);
    }

    // ============================================================
    //  右クリックでGUIを開く（クールダウン設定は削除）
    // ============================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // ★ クールダウンはここでは設定しない（変換成功時に ManaConversionHandler で設定）
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                    ItemStackHandler handler = RingOfManaConversionItem.getInventory(stack);
                    return new RingOfManaConversionContainer(id, inv, handler, stack);
                }
            }, buf -> buf.writeItem(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ============================================================
    //  内部インベントリ操作
    // ============================================================

    public static ItemStackHandler getInventory(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                saveInventory(stack, this);
                updateCircuitCount(stack);
            }
        };
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }
        return handler;
    }

    public static void saveInventory(ItemStack stack, ItemStackHandler handler) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("Inventory", handler.serializeNBT());
    }

    // ============================================================
    //  NBT 回路数管理
    // ============================================================

    public static void updateCircuitCount(ItemStack stack) {
        ItemStackHandler handler = getInventory(stack);
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.getItem() == ModItems.MANA_CIRCUIT.get()) {
                total += s.getCount();
            }
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("CircuitCount", total);
    }

    // ★ 再起動後も正しく回路数を取得するため、NBTに依存せずインベントリを直接スキャン
    public static int getCircuitCount(ItemStack stack) {
        ItemStackHandler handler = getInventory(stack);
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.getItem() == ModItems.MANA_CIRCUIT.get()) {
                total += s.getCount();
            }
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("CircuitCount", total);
        return total;
    }

    // ============================================================
    //  ツールチップ
    // ============================================================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.more_iss.ring_of_mana_conversion.desc").withStyle(ChatFormatting.GRAY));

        int totalCircuits = getCircuitCount(stack);
        tooltip.add(Component.translatable("item.more_iss.ring_of_mana_conversion.circuits", totalCircuits).withStyle(ChatFormatting.AQUA));
    }

    // ============================================================
    //  Curios インターフェース実装
    // ============================================================

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // 何もしない
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // 何もしない
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    // ============================================================
    //  内部クラス：Capability プロバイダ
    // ============================================================

    private static class ItemStackHandlerProvider implements ICapabilityProvider {

        private final LazyOptional<ItemStackHandler> inventory;

        public ItemStackHandlerProvider(ItemStack stack) {
            this.inventory = LazyOptional.of(() -> RingOfManaConversionItem.getInventory(stack));
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return inventory.cast();
            }
            return LazyOptional.empty();
        }
    }
}