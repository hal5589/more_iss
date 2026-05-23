package nekotori_haru.more_iss.recipe;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class ArcaneCraftingRecipe
        implements Recipe<ArcaneCraftingTableBlockEntity.RecipeWrapper> {

    public static final String ID = "arcane_crafting";

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;

    // ⭕ 追加：各スロットに要求されるスクロールの呪文IDとレベルを保持するリスト
    private final List<ResourceLocation> requiredSpellIds;
    private final List<Integer> requiredSpellLevels;

    private final Ingredient catalyst;
    private final boolean    catalystConsume;
    private final ItemStack  result;

    @Nullable private final ResourceLocation spellId;
    private final int spellLevel;

    public ArcaneCraftingRecipe(ResourceLocation id,
                                NonNullList<Ingredient> ingredients,
                                List<ResourceLocation> requiredSpellIds,
                                List<Integer> requiredSpellLevels,
                                Ingredient catalyst,
                                boolean catalystConsume,
                                ItemStack result,
                                @Nullable ResourceLocation spellId,
                                int spellLevel) {
        this.id = id;
        this.ingredients = ingredients;
        this.requiredSpellIds = requiredSpellIds;
        this.requiredSpellLevels = requiredSpellLevels;
        this.catalyst = catalyst;
        this.catalystConsume = catalystConsume;
        this.result = result;
        this.spellId = spellId;
        this.spellLevel = spellLevel;
    }

    private ItemStack createSecureScroll() {
        Item scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "scroll"));
        if (scrollItem == null || scrollItem == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack scroll = new ItemStack(scrollItem, 1);
        if (this.spellId != null) {
            ISpellContainer.createScrollContainer(
                    SpellRegistry.getSpell(this.spellId),
                    this.spellLevel,
                    scroll
            );
        }
        return scroll;
    }

    // ⭕ あなたの環境のISSの仕様（getActiveSpells() が List<SpellSlot> を返す）に完全に適合させた判定ロジック
    private boolean checkScrollValidity(ItemStack clickedStack, int slotIndex) {
        if (slotIndex >= requiredSpellIds.size() || slotIndex >= requiredSpellLevels.size()) return true;

        ResourceLocation reqSpell = requiredSpellIds.get(slotIndex);
        int reqLevel = requiredSpellLevels.get(slotIndex);

        // JSON側でこのスロットに呪文制限が指定されていない（null）ならパス
        if (reqSpell == null) return true;

        // 実際に置かれたアイテムからISSの呪文コンテナを安全に取得
        if (!ISpellContainer.isSpellContainer(clickedStack)) return false;

        // ⭕ 以前のバージョンで存在が確認できているメソッドを使用
        ISpellContainer container = ISpellContainer.getSpellContainer(clickedStack);
        if (container == null) return false;

        // ⭕ エラーメッセージの通り、getActiveSpells() を呼び出して List<SpellSlot> で受け取る
        java.util.List<io.redspace.ironsspellbooks.api.spells.SpellSlot> slots = container.getActiveSpells();
        if (slots == null || slots.isEmpty()) return false;

        // スクロール内の最初のスロット（呪文データ）を検証
        io.redspace.ironsspellbooks.api.spells.SpellSlot slot = slots.get(0);
        if (slot == null || slot.getSpell() == null) return false;

        // 呪文のID、および指定レベルが完全に一致しているか判定
        return slot.getSpell().getSpellId().equals(reqSpell.toString()) && slot.getLevel() == reqLevel;
    }

    // ─── matches ──────────────────────────────────────────────────────────
    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, Level level) {
        // 周囲8スロット (0-7)
        for (int i = 0; i < 8; i++) {
            Ingredient ing = ingredients.get(i);
            ItemStack stack = inv.getItem(i);
            if (ing.isEmpty()) {
                if (!stack.isEmpty()) return false;
            } else {
                if (!ing.test(stack)) return false;
                // ⭕ スクロールの呪文内容チェックを実行
                if (!checkScrollValidity(stack, i)) return false;
            }
        }

        // 中央スロット (8)
        if (ingredients.size() > 8) {
            Ingredient centerIng = ingredients.get(8);
            ItemStack centerStack = inv.getItem(8);
            if (!centerIng.isEmpty()) {
                if (!centerIng.test(centerStack)) return false;
                // ⭕ スクロールの呪文内容チェックを実行（インデックス8）
                if (!checkScrollValidity(centerStack, 8)) return false;
            }
        }

        // 触媒スロット (9)
        if (catalyst != null && catalyst != Ingredient.EMPTY && catalyst.getItems().length > 0) {
            if (!catalyst.test(inv.getItem(9))) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, RegistryAccess reg) {
        if (this.spellId != null) {
            return createSecureScroll();
        }
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess reg) { return this.spellId != null ? createSecureScroll() : result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ArcaneCraftingRecipeSerializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return ArcaneCraftingRecipeType.INSTANCE; }

    public NonNullList<Ingredient> getIngredients() { return ingredients; }
    public List<ResourceLocation> getRequiredSpellIds() { return requiredSpellIds; }
    public List<Integer> getRequiredSpellLevels() { return requiredSpellLevels; }
    public Ingredient getCatalyst() { return catalyst; }
    public boolean isCatalystConsumed() { return catalystConsume; }
    public ItemStack getResult() { return result; }
    @Nullable public ResourceLocation getSpellId() { return spellId; }
    public int getSpellLevel() { return spellLevel; }
}