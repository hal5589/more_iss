package nekotori_haru.more_iss.blockentity;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.menu.ArcaneCraftingMenu;
import nekotori_haru.more_iss.network.ModNetwork;
import nekotori_haru.more_iss.network.PacketSyncCraftingAnim;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipe;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class ArcaneCraftingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int CRAFT_DURATION = 40;
    public static final int CATALYST_MAX_STACK = 1024;
    private static final int TOTAL_SLOTS = 10;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int craftingTick = 0;
    private boolean craftingActive = false;

    // メニュー同期用のデータアクセス窓口
    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? ArcaneCraftingTableBlockEntity.this.craftingTick : (ArcaneCraftingTableBlockEntity.this.craftingActive ? 1 : 0);
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                ArcaneCraftingTableBlockEntity.this.craftingTick = value;
            } else {
                ArcaneCraftingTableBlockEntity.this.craftingActive = (value == 1);
            }
        }

        @Override
        public int getCount() { return 2; }
    };

    // Forge Capability用ハンドラー（ホッパーや外部搬入でのレシピ自動チェック用）
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() ->
            new ItemStackHandler(TOTAL_SLOTS) {
                @Override
                public int getSlotLimit(int slot) {
                    return slot == 9 ? CATALYST_MAX_STACK : 64;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            });

    public ArcaneCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(More_iss.ARCANE_CRAFTING_TABLE_BE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // タイマー処理（サーバーTick）
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneCraftingTableBlockEntity be) {
        // クラフトが有効でない場合は、常にレシピが揃ったか監視し続ける
        if (!be.craftingActive) {
            be.checkForRecipe();
            return;
        }

        // クラフト進行中
        be.craftingTick--;
        ModNetwork.sendToNearbyPlayers(
                new PacketSyncCraftingAnim(pos, be.craftingTick, true),
                level, pos, 32
        );

        if (be.craftingTick <= 0) {
            be.finishCrafting(level);
        }
    }

    /** 素材が揃っているか確認し、揃っていればクラフト演出を開始 */
    public void checkForRecipe() {
        if (this.level == null || this.level.isClientSide) return;
        if (this.craftingActive) return;
        if (!this.items.get(8).isEmpty()) return; // 出力スロットにアイテムがある間は開始しない

        Optional<ArcaneCraftingRecipe> recipe = this.level.getRecipeManager()
                .getRecipeFor(ArcaneCraftingRecipeType.INSTANCE, new RecipeWrapper(this), this.level);

        if (recipe.isPresent()) {
            this.craftingActive = true;
            this.craftingTick = CRAFT_DURATION;
            this.setChanged();
            ModNetwork.sendToNearbyPlayers(
                    new PacketSyncCraftingAnim(this.worldPosition, this.craftingTick, true),
                    this.level, this.worldPosition, 32
            );
        }
    }

    /** クラフト完成処理 */
    private void finishCrafting(Level level) {
        Optional<ArcaneCraftingRecipe> recipeOpt = level.getRecipeManager()
                .getRecipeFor(ArcaneCraftingRecipeType.INSTANCE, new RecipeWrapper(this), level);

        recipeOpt.ifPresent(recipe -> {
            for (int i = 0; i < 8; i++) {
                this.items.get(i).shrink(1);
            }
            if (recipe.isCatalystConsumed() && !this.items.get(9).isEmpty()) {
                this.items.get(9).shrink(1);
            }
            this.items.set(8, recipe.assemble(new RecipeWrapper(this), level.registryAccess()));
        });

        this.craftingActive = false;
        this.craftingTick = 0;
        this.setChanged();

        ModNetwork.sendToNearbyPlayers(
                new PacketSyncCraftingAnim(this.worldPosition, 0, false),
                level, this.worldPosition, 32
        );
    }

    public void setCraftingAnimFromPacket(int tick, boolean active) {
        this.craftingTick = tick;
        this.craftingActive = active;
    }

    // -------------------------------------------------------------------------
    // Container 実装（GUI同期用イベントフックの強化）
    // -------------------------------------------------------------------------

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
            this.checkForRecipe(); // ★追加: アイテムが減ったときもレシピを再チェック
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(items, slot);
        this.checkForRecipe();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        this.setChanged();
        this.checkForRecipe(); // ★アイテムが置かれたらチェック
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // ブロックデータが変わるたび、安全のため常にレシピを即時評価するフック
        if (this.level != null && !this.level.isClientSide) {
            this.checkForRecipe();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        this.setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.more_iss.arcane_crafting_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new ArcaneCraftingMenu(id, inv, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) { return index != 8; }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) { return index == 8; }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        craftingTick = tag.getInt("CraftingTick");
        craftingActive = tag.getBoolean("CraftingActive");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("CraftingTick", craftingTick);
        tag.putBoolean("CraftingActive", craftingActive);
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    // レシピラッパー
    public static class RecipeWrapper implements Container {
        private final ArcaneCraftingTableBlockEntity be;
        public RecipeWrapper(ArcaneCraftingTableBlockEntity be) { this.be = be; }

        @Override public int getContainerSize() { return 10; }
        @Override public boolean isEmpty() { return be.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return be.items.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return be.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return be.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { be.setItem(slot, stack); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { be.clearContent(); }
        @Override public void setChanged() { be.setChanged(); }
    }
}