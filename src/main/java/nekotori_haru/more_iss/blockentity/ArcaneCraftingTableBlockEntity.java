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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ArcaneCraftingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandlerOpt = LazyOptional.of(() -> itemHandler);

    int craftingTick = 0;
    boolean isCraftingActive = false;

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int idx) {
            return switch (idx) {
                case 0 -> craftingTick;
                case 1 -> isCraftingActive ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int idx, int value) {
            switch (idx) {
                case 0 -> craftingTick = value;
                case 1 -> isCraftingActive = (value == 1);
            }
        }
        @Override public int getCount() { return 2; }
    };

    public ArcaneCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(More_iss.ARCANE_CRAFTING_TABLE_BE.get(), pos, state);
    }

    public void setCraftingAnimFromPacket(int craftingTick, boolean active) {
        this.craftingTick = craftingTick;
        this.isCraftingActive = active;
    }

    // ⭕ 【ロジック修正】サーバー側はクラフトの「開始」と「維持」だけを管理する
    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneCraftingTableBlockEntity be) {
        if (level.isClientSide) return;

        RecipeWrapper wrapper = new RecipeWrapper(be);
        Optional<ArcaneCraftingRecipe> recipeOpt = level.getRecipeManager().getRecipeFor(ArcaneCraftingRecipeType.INSTANCE, wrapper, level);

        if (recipeOpt.isPresent()) {
            if (!be.isCraftingActive) {
                be.isCraftingActive = true;
                be.craftingTick = 40; // クラフト開始フラグ
                be.setChanged();
                sendAnimPacket(level, pos, be, true);
            }

            // ⭕ 自動でカウントダウンして finishCrafting() を呼ぶ処理を削除。
            // 演出中のクラフト有効状態（isCraftingActive = true）を維持し続けます。
        } else {
            // レシピが崩されたら中断
            be.stopCrafting(level, pos);
        }
    }

    // ⭕ 【パケット受信時に呼ぶためのパブリックメソッド】
    // Screen側から「爆発したよ！」というパケット（sendCraftCompletionToServer）が届いた時、
    // サーバー側のネットワークハンドラ等からこのメソッドを呼び出すようにしてください。
    public void executeCraftCompletion() {
        if (this.level == null || this.level.isClientSide) return;

        RecipeWrapper wrapper = new RecipeWrapper(this);
        Optional<ArcaneCraftingRecipe> recipeOpt = this.level.getRecipeManager().getRecipeFor(ArcaneCraftingRecipeType.INSTANCE, wrapper, this.level);

        if (recipeOpt.isPresent()) {
            this.finishCrafting(recipeOpt.get());
        } else {
            this.stopCrafting(this.level, this.worldPosition);
        }
    }

    private void finishCrafting(@Nullable ArcaneCraftingRecipe recipe) {
        if (recipe == null || level == null || level.isClientSide) return;

        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();

        // 材料の消費（周囲8スロット）
        for (int i = 0; i < 8; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                itemHandler.setStackInSlot(i, stack);
            }
        }

        // 中央スロットの消費
        if (recipe.getIngredients().size() > 8 && !recipe.getIngredients().get(8).isEmpty()) {
            itemHandler.extractItem(8, 1, false);
        }

        // 触媒の消費
        if (recipe.isCatalystConsumed()) {
            itemHandler.extractItem(9, 1, false);
        }

        // 成果物を中央（スロット8）にセット
        itemHandler.setStackInSlot(8, result);

        isCraftingActive = false;
        craftingTick = 0;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                new PacketSyncCraftingAnim(worldPosition, 0, false)
        );
    }

    private void stopCrafting(Level level, BlockPos pos) {
        if (!isCraftingActive) return;
        isCraftingActive = false;
        craftingTick = 0;
        setChanged();
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new PacketSyncCraftingAnim(pos, 0, false)
        );
    }

    private static void sendAnimPacket(Level level, BlockPos pos, ArcaneCraftingTableBlockEntity be, boolean active) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new PacketSyncCraftingAnim(pos, be.craftingTick, active)
        );
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandlerOpt.invalidate(); }

    @Override public int getContainerSize() { return 10; }
    @Override public boolean isEmpty() { for (int i = 0; i < 10; i++) if (!itemHandler.getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return itemHandler.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int count) { return itemHandler.extractItem(slot, count, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack s = itemHandler.getStackInSlot(slot); itemHandler.setStackInSlot(slot, ItemStack.EMPTY); return s; }
    @Override public void setItem(int slot, ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < 10; i++) itemHandler.setStackInSlot(i, ItemStack.EMPTY); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0,1,2,3,4,5,6,7,8,9}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) { return slot != 8 || !isCraftingActive; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) { return slot != 8 || !isCraftingActive; }

    @Override public void load(CompoundTag tag) { super.load(tag); itemHandler.deserializeNBT(tag.getCompound("Inventory")); craftingTick = tag.getInt("CraftingTick"); isCraftingActive = tag.getBoolean("CraftingActive"); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Inventory", itemHandler.serializeNBT()); tag.putInt("CraftingTick", craftingTick); tag.putBoolean("CraftingActive", isCraftingActive); }
    @Override protected Component getDefaultName() { return Component.translatable("container.more_iss.arcane_crafting_table"); }
    @Override protected AbstractContainerMenu createMenu(int windowId, Inventory inv) { return new ArcaneCraftingMenu(windowId, inv, this); }

    public static class RecipeWrapper implements Container {
        private final ArcaneCraftingTableBlockEntity be;
        public RecipeWrapper(ArcaneCraftingTableBlockEntity be) { this.be = be; }
        @Override public int getContainerSize() { return 10; }
        @Override public boolean isEmpty() { return be.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return be.itemHandler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int s, int c) { return be.itemHandler.extractItem(s, c, false); }
        @Override public ItemStack removeItemNoUpdate(int s) { ItemStack st = be.itemHandler.getStackInSlot(s); be.itemHandler.setStackInSlot(s, ItemStack.EMPTY); return st; }
        @Override public void setItem(int s, ItemStack stack) { be.itemHandler.setStackInSlot(s, stack); }
        @Override public void setChanged() { be.setChanged(); }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public void clearContent() { be.clearContent(); }
    }
}