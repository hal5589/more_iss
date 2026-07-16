package nekotori_haru.more_iss.item;

import nekotori_haru.more_iss.menu.ManaFurnaceContainer;
import nekotori_haru.more_iss.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

public class RingOfManaFurnaceItem extends Item implements ICurioItem {

    // ★ スロット数は1個に変更
    public static final int SLOT_COUNT = 1;

    public static final int MAX_CHARGE = 10000;
    public static final int BUFF_DURATION_TICKS = 10 * 20;

    private static final String TAG_CHARGE       = "ManaCharge";
    private static final String TAG_ACTIVE        = "FurnaceActive";
    private static final String TAG_BUFF_READY    = "BuffReady";

    public RingOfManaFurnaceItem(Properties properties) {
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
    //  右クリック：Shiftで起動トグル、通常でGUI開く
    // ============================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                boolean current = isActive(stack);
                setActive(stack, !current);
                player.displayClientMessage(
                        Component.translatable(
                                !current
                                        ? "item.more_iss.ring_of_mana_furnace.activated"
                                        : "item.more_iss.ring_of_mana_furnace.deactivated"
                        ).withStyle(!current ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                    ItemStackHandler handler = RingOfManaFurnaceItem.getInventory(stack);
                    return new ManaFurnaceContainer(id, inv, handler, stack);
                }
            }, buf -> buf.writeItem(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ============================================================
    //  Curios
    // ============================================================

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (isActive(stack) && getCharge(stack) >= MAX_CHARGE) {
            setBuffReady(stack, true);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        setBuffReady(stack, false);
    }

    // ============================================================
    //  内部インベントリ（スロット1個）
    // ============================================================

    public static ItemStackHandler getInventory(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                saveInventory(stack, this);
            }
        };
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }
        return handler;
    }

    public static void saveInventory(ItemStack stack, ItemStackHandler handler) {
        stack.getOrCreateTag().put("Inventory", handler.serializeNBT());
    }

    // ============================================================
    //  回路数カウント（スロット1個なので簡略化）
    // ============================================================

    public static int getCircuitCount(ItemStack stack) {
        ItemStackHandler handler = getInventory(stack);
        ItemStack slotStack = handler.getStackInSlot(0);
        if (slotStack.getItem() == ModItems.MANA_CIRCUIT.get()) {
            return slotStack.getCount();
        }
        return 0;
    }

    // ============================================================
    //  NBT ゲッター / セッター
    // ============================================================

    public static int getCharge(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_CHARGE);
    }

    public static void setCharge(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TAG_CHARGE, Math.max(0, Math.min(MAX_CHARGE, value)));
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVE, active);
    }

    public static boolean isBuffReady(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_BUFF_READY);
    }

    public static void setBuffReady(ItemStack stack, boolean ready) {
        stack.getOrCreateTag().putBoolean(TAG_BUFF_READY, ready);
    }

    // ============================================================
    //  ツールチップ
    // ============================================================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("item.more_iss.ring_of_mana_furnace.desc")
                .withStyle(ChatFormatting.GRAY));

        int charge = getCharge(stack);
        int circuits = getCircuitCount(stack);
        boolean active = isActive(stack);

        tooltip.add(Component.translatable(
                "item.more_iss.ring_of_mana_furnace.charge", charge, MAX_CHARGE
        ).withStyle(charge >= MAX_CHARGE ? ChatFormatting.GOLD : ChatFormatting.AQUA));

        tooltip.add(Component.translatable(
                "item.more_iss.ring_of_mana_furnace.circuits", circuits
        ).withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable(
                active
                        ? "item.more_iss.ring_of_mana_furnace.status_on"
                        : "item.more_iss.ring_of_mana_furnace.status_off"
        ).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));

        if (charge >= MAX_CHARGE) {
            tooltip.add(Component.translatable("item.more_iss.ring_of_mana_furnace.ready")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
    }

    // ============================================================
    //  内部クラス
    // ============================================================

    private static class ItemStackHandlerProvider implements ICapabilityProvider {

        private final LazyOptional<ItemStackHandler> inventory;

        public ItemStackHandlerProvider(ItemStack stack) {
            this.inventory = LazyOptional.of(() -> RingOfManaFurnaceItem.getInventory(stack));
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