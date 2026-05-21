package nekotori_haru.more_iss.blockentity;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import nekotori_haru.more_iss.network.ModNetwork;
import nekotori_haru.more_iss.network.PacketSyncCraftingAnim;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipe;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ArcaneCraftingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int CRAFT_DURATION = 100; // 5秒
    public static final int CATALYST_MAX_STACK = 1024; // 触媒スロットの最大数

    // ⭕ アイテムデータの実体はこれ1本のみに一本化！
    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 9 ? CATALYST_MAX_STACK : super.getSlotLimit(slot);
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    private int craftingTick = 0;
    private boolean isCraftingActive = false;

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int id) { return id == 0 ? craftingTick : (isCraftingActive ? 1 : 0); }
        @Override public void set(int id, int val) { if (id == 0) craftingTick = val; else isCraftingActive = (val == 1); }
        @Override public int getCount() { return 2; }
    };

    public ArcaneCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(More_iss.ARCANE_CRAFTING_TABLE_BE.get(), pos, state);
    }

    /**
     * パケット側から呼び出されるアニメーション更新用のクライアント専用メソッド
     */
    public void setCraftingAnimFromPacket(int craftingTick, boolean active) {
        this.craftingTick = craftingTick;
        this.isCraftingActive = active;
    }

    /**
     * 毎tick実行されるクラフト主処理
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneCraftingTableBlockEntity be) {
        RecipeWrapper wrapper = new RecipeWrapper(be);
        Optional<ArcaneCraftingRecipe> recipeOpt = level.getRecipeManager()
                .getRecipeFor(ArcaneCraftingRecipeType.INSTANCE, wrapper, level);

        if (recipeOpt.isPresent()) {
            ArcaneCraftingRecipe recipe = recipeOpt.get();

            ItemStack outputSlot = be.itemHandler.getStackInSlot(8);
            ItemStack resultStack = recipe.getResultItem(level.registryAccess());

            if (outputSlot.isEmpty() || (ItemStack.isSameItemSameTags(outputSlot, resultStack) && outputSlot.getCount() + resultStack.getCount() <= outputSlot.getMaxStackSize())) {
                if (!be.isCraftingActive) {
                    be.isCraftingActive = true;
                    be.craftingTick = CRAFT_DURATION;
                    be.setChanged();

                    // ⭕ 修正: TRACKING_CHUNK を使い、引数を 3 つ完璧に揃える
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                            new PacketSyncCraftingAnim(pos, be.craftingTick, true)
                    );
                }

                if (be.craftingTick > 0) {
                    be.craftingTick--;
                    // 5tickに1回、クライアントへアニメーション進行度を同期
                    if (be.craftingTick % 5 == 0) {
                        ModNetwork.CHANNEL.send(
                                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                                new PacketSyncCraftingAnim(pos, be.craftingTick, true)
                        );
                    }
                    be.setChanged();
                }

                if (be.craftingTick <= 0) {
                    be.finishCrafting(recipe);
                }
            } else {
                be.stopCrafting(level, pos);
            }
        } else {
            be.stopCrafting(level, pos);
        }
    }

    /**
     * クラフト完了処理
     */
    private void finishCrafting(ArcaneCraftingRecipe recipe) {
        for (int i = 0; i < 8; i++) {
            itemHandler.extractItem(i, 1, false);
        }
        if (recipe.isCatalystConsumed()) {
            itemHandler.extractItem(9, 1, false);
        }
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(8);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(8, result);
        } else {
            currentOutput.grow(result.getCount());
            itemHandler.setStackInSlot(8, currentOutput);
        }

        isCraftingActive = false;
        craftingTick = 0;
        setChanged();

        // ⭕ 修正: TRACKING_CHUNK を使い、引数を 3 つ完璧に揃える
        if (level != null) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                    new PacketSyncCraftingAnim(worldPosition, 0, false)
            );
        }
    }

    /**
     * クラフト中断処理
     */
    private void stopCrafting(Level level, BlockPos pos) {
        if (isCraftingActive) {
            isCraftingActive = false;
            craftingTick = 0;
            setChanged();

            // ⭕ 修正: TRACKING_CHUNK を使い、引数を 3 つ完璧に揃える
            ModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                    new PacketSyncCraftingAnim(pos, 0, false)
            );
        }
    }

    // --- Container (WorldlyContainer) 実装（すべて内部の itemHandler へ一本化） ---
    @Override public int getContainerSize() { return 10; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < 10; i++) if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return itemHandler.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return itemHandler.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }
    @Override public void setItem(int slot, ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < 10; i++) itemHandler.setStackInSlot(i, ItemStack.EMPTY); }

    // --- NBT 処理 ---
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        this.craftingTick = tag.getInt("CraftingTick");
        this.isCraftingActive = tag.getBoolean("CraftingActive");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("CraftingTick", craftingTick);
        tag.putBoolean("CraftingActive", isCraftingActive);
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0,1,2,3,4,5,6,7,8,9}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) { return slot != 8; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) { return slot == 8; }

    @Override protected Component getDefaultName() { return Component.translatable("container.more_iss.arcane_crafting_table"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new ArcaneCraftingMenu(id, inv, this); }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // レシピ検証用ラッパー
    public static class RecipeWrapper implements Container {
        private final ArcaneCraftingTableBlockEntity be;
        public RecipeWrapper(ArcaneCraftingTableBlockEntity be) { this.be = be; }

        @Override public int getContainerSize() { return 10; }
        @Override public boolean isEmpty() { return be.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return be.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return be.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return be.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { be.setItem(slot, stack); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { be.clearContent(); }
        @Override public void setChanged() { be.setChanged(); }
    }
}