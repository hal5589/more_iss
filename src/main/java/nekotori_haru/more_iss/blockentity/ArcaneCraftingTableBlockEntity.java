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
import net.minecraft.world.inventory.SimpleContainerData;
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

public class ArcaneCraftingTableBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer {

    // スロット 0-7 : 周囲材料
    // スロット 8   : 中央（入力兼出力）← プレイヤーがここに入力素材を置ける
    // スロット 9   : 触媒
    public final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandlerOpt =
            LazyOptional.of(() -> itemHandler);

    int     craftingTick     = 0;
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
                case 0 -> craftingTick     = value;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ArcaneCraftingTableBlockEntity be) {
        if (level.isClientSide) return;

        RecipeWrapper wrapper = new RecipeWrapper(be);

        Optional<ArcaneCraftingRecipe> recipeOpt =
                level.getRecipeManager().getRecipeFor(
                        ArcaneCraftingRecipeType.INSTANCE, wrapper, level);

        if (recipeOpt.isPresent()) {
            ArcaneCraftingRecipe recipe = recipeOpt.get();
            ItemStack result = recipe.assemble(wrapper, level.registryAccess());

            if (result.isEmpty()) {
                be.stopCrafting(level, pos);
                return;
            }

            if (!be.isCraftingActive) {
                be.isCraftingActive = true;
                be.craftingTick     = 60;
                be.setChanged();
                sendAnimPacket(level, pos, be, true);
            }

            if (be.craftingTick > 0) {
                be.craftingTick--;
                if (be.craftingTick % 5 == 0) {
                    sendAnimPacket(level, pos, be, true);
                }
                be.setChanged();
            }

            if (be.craftingTick <= 0) {
                be.finishCrafting(recipe);
            }

        } else {
            be.stopCrafting(level, pos);
        }
    }

    private void finishCrafting(ArcaneCraftingRecipe recipe) {
        for (int i = 0; i < 8; i++) {
            itemHandler.extractItem(i, 1, false);
        }

        var ings = recipe.getIngredients();
        if (ings.size() > 8 && !ings.get(8).isEmpty()) {
            itemHandler.extractItem(8, 1, false); // 中央のベース素材を1個消費
        }

        if (recipe.isCatalystConsumed()) {
            itemHandler.extractItem(9, 1, false);
        }

        // 完成品を中央（スロット8）に上書きセット
        ItemStack result = recipe.assemble(new RecipeWrapper(this), this.level.registryAccess()).copy();
        itemHandler.setStackInSlot(8, result);

        isCraftingActive = false;
        craftingTick     = 0;
        setChanged();

        if (this.level != null) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with(
                            () -> this.level.getChunkAt(this.worldPosition)),
                    new PacketSyncCraftingAnim(this.worldPosition, 0, false));
        }
    }

    private void stopCrafting(Level level, BlockPos pos) {
        if (!isCraftingActive) return;
        isCraftingActive = false;
        craftingTick     = 0;
        setChanged();
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(
                        () -> level.getChunkAt(pos)),
                new PacketSyncCraftingAnim(pos, 0, false));
    }

    private static void sendAnimPacket(Level level, BlockPos pos,
                                       ArcaneCraftingTableBlockEntity be,
                                       boolean active) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(
                        () -> level.getChunkAt(pos)),
                new PacketSyncCraftingAnim(pos, be.craftingTick, active));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerOpt.invalidate();
    }

    @Override public int getContainerSize()                      { return 10; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < 10; i++)
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot)                 { return itemHandler.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int count)   { return itemHandler.extractItem(slot, count, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return s;
    }
    @Override public void setItem(int slot, ItemStack stack)     { itemHandler.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player)           { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() {
        for (int i = 0; i < 10; i++) itemHandler.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override public int[] getSlotsForFace(Direction side)       { return new int[]{0,1,2,3,4,5,6,7,8,9}; }

    // ─── 自動搬入出の制限（クラフト中でなければ中央への出し入れも可能にする） ───
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 8) return !this.isCraftingActive; // クラフト中でなければ中央にパイプでベースを搬入可能
        return true;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (slot == 8) return !this.isCraftingActive; // クラフト中でなければ中央の成果物をパイプで搬出可能
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        craftingTick     = tag.getInt("CraftingTick");
        isCraftingActive = tag.getBoolean("CraftingActive");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory",         itemHandler.serializeNBT());
        tag.putInt("CraftingTick",   craftingTick);
        tag.putBoolean("CraftingActive", isCraftingActive);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.more_iss.arcane_crafting_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int windowId, Inventory inv) {
        return new ArcaneCraftingMenu(windowId, inv, this);
    }

    public static class RecipeWrapper implements Container {
        private final ArcaneCraftingTableBlockEntity be;
        public RecipeWrapper(ArcaneCraftingTableBlockEntity be) { this.be = be; }

        @Override public int getContainerSize()                   { return 10; }
        @Override public boolean isEmpty()                        { return be.isEmpty(); }
        @Override public ItemStack getItem(int slot)              { return be.itemHandler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int s, int c)       { return be.itemHandler.extractItem(s, c, false); }
        @Override public ItemStack removeItemNoUpdate(int s) {
            ItemStack st = be.itemHandler.getStackInSlot(s);
            be.itemHandler.setStackInSlot(s, ItemStack.EMPTY);
            return st;
        }
        @Override public void setItem(int s, ItemStack stack)     { be.itemHandler.setStackInSlot(s, stack); }
        @Override public void setChanged()                        { be.setChanged(); }
        @Override public boolean stillValid(Player p)             { return true; }
        @Override public void clearContent()                      { be.clearContent(); }
    }
}