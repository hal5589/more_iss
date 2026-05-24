package nekotori_haru.more_iss.recipe;

import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class ArcaneCraftingRecipe
        implements Recipe<ArcaneCraftingTableBlockEntity.RecipeWrapper> {

    public static final String ID = "arcane_crafting";

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;

    private final NonNullList<CompoundTag> ingredientNBTs;
    private final NonNullList<String> requiredSpells;
    private final NonNullList<Integer> requiredLevels;

    private final Ingredient catalyst;
    private final CompoundTag catalystNBT;
    private final boolean catalystConsume;

    private final ItemStack result;
    private final String resultSpellId;
    private final int resultSpellLevel;

    public ArcaneCraftingRecipe(ResourceLocation id,
                                NonNullList<Ingredient> ingredients,
                                NonNullList<CompoundTag> ingredientNBTs,
                                NonNullList<String> requiredSpells,
                                NonNullList<Integer> requiredLevels,
                                Ingredient catalyst,
                                CompoundTag catalystNBT,
                                boolean catalystConsume,
                                ItemStack result,
                                String resultSpellId,
                                int resultSpellLevel) {
        this.id = id;
        this.ingredients = ingredients;
        this.ingredientNBTs = ingredientNBTs;
        this.requiredSpells = requiredSpells;
        this.requiredLevels = requiredLevels;
        this.catalyst = catalyst;
        this.catalystNBT = catalystNBT;
        this.catalystConsume = catalystConsume;
        this.result = result;
        this.resultSpellId = resultSpellId;
        this.resultSpellLevel = resultSpellLevel;
    }

    // ─── matches ──────────────────────────────────────────────────────────
    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, Level level) {
        boolean allEmpty = true;
        for (int i = 0; i < 10; i++) {
            if (!inv.getItem(i).isEmpty()) { allEmpty = false; break; }
        }
        if (!allEmpty) {
            System.out.println("=== matches() called for: " + this.id + " ===");
            for (int i = 0; i < 10; i++) {
                System.out.println("  slot[" + i + "] = " + inv.getItem(i));
            }
        }

        for (int i = 0; i < 9; i++) {
            Ingredient ing = ingredients.get(i);
            ItemStack stack = inv.getItem(i);

            if (ing.isEmpty()) {
                if (!stack.isEmpty()) {
                    System.out.println("  -> Slot " + i + " failed: Recipe expects EMPTY, but slot has item.");
                    return false;
                }
                continue;
            }

            if (!ing.test(stack)) {
                System.out.println("  -> Slot " + i + " failed: Ingredient type mismatch.");
                return false;
            }

            // NBT タグチェック
            CompoundTag recipeNBT = ingredientNBTs.get(i);
            if (recipeNBT != null && !recipeNBT.isEmpty() && recipeNBT.getAllKeys().size() > 0) {
                if (!stack.hasTag()) {
                    System.out.println("  -> Slot " + i + " failed: Recipe expects NBT, but item has no NBT.");
                    return false;
                }
                CompoundTag stackTag = stack.getTag();
                if (stackTag == null || !nbtMatches(stackTag, recipeNBT)) {
                    System.out.println("  -> Slot " + i + " failed: Raw NBT mismatch.");
                    return false;
                }
            }

            // Required Spell チェック
            String requiredSpell = requiredSpells.get(i);
            if (requiredSpell != null && !requiredSpell.isEmpty()) {
                if (!isScrollWithSpell(stack, requiredSpell, requiredLevels.get(i), i)) {
                    System.out.println("  -> Slot " + i + " failed: Scroll spell/level verification failed.");
                    return false;
                }
            }
        }

        // 触媒スロット (9)
        if (catalyst != null && catalyst != Ingredient.EMPTY && catalyst.getItems().length > 0) {
            ItemStack catalystStack = inv.getItem(9);
            if (!catalyst.test(catalystStack)) {
                System.out.println("  -> Catalyst slot failed: Item type mismatch.");
                return false;
            }

            if (catalystNBT != null && !catalystNBT.isEmpty() && catalystNBT.getAllKeys().size() > 0) {
                if (!catalystStack.hasTag()) return false;
                CompoundTag stackTag = catalystStack.getTag();
                if (stackTag == null || !nbtMatches(stackTag, catalystNBT)) return false;
            }
        }

        System.out.println("  🎉 MATCH SUCCESS FOR RECIPE: " + this.id);
        return true;
    }

    private boolean nbtMatches(CompoundTag stackTag, CompoundTag recipeNBT) {
        for (String key : recipeNBT.getAllKeys()) {
            if (!stackTag.contains(key)) return false;
            if (!stackTag.get(key).equals(recipeNBT.get(key))) return false;
        }
        return true;
    }

    private boolean isScrollWithSpell(ItemStack stack, String requiredSpellId, int requiredLevel, int slotIndex) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !itemId.equals(new ResourceLocation("irons_spellbooks:scroll"))) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) return false;

        if (tag.contains("irons_spellbooks:spell_container")) {
            CompoundTag containerTag = tag.getCompound("irons_spellbooks:spell_container");
            if (containerTag.contains("data", net.minecraft.nbt.Tag.TAG_LIST)) {
                net.minecraft.nbt.ListTag dataList = containerTag.getList("data", net.minecraft.nbt.Tag.TAG_COMPOUND);
                if (!dataList.isEmpty()) {
                    CompoundTag dataTag = dataList.getCompound(0);
                    String spellId = dataTag.getString("id");
                    int level = dataTag.getInt("level");

                    System.out.println("    [Scroll Check Slot " + slotIndex + "] Slot Has: " + spellId + " (Lv " + level + ") | Required: " + requiredSpellId + " (Lv " + requiredLevel + ")");

                    return spellId.equals(requiredSpellId) && level >= requiredLevel;
                }
            }
        }
        return false;
    }

    // ─── assemble ─────────────────────────────────────────────────────────
    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, RegistryAccess reg) {
        if (resultSpellId != null && !resultSpellId.isEmpty()) {
            var scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks:scroll"));
            if (scrollItem != null) {
                ItemStack spellScroll = new ItemStack(scrollItem, 1);
                CompoundTag tag = new CompoundTag();
                CompoundTag containerTag = new CompoundTag();
                net.minecraft.nbt.ListTag dataList = new net.minecraft.nbt.ListTag();

                CompoundTag dataTag = new CompoundTag();
                dataTag.putString("id", resultSpellId);
                dataTag.putByte("index", (byte) 0);
                dataTag.putShort("level", (short) resultSpellLevel);
                dataTag.putBoolean("locked", true);
                dataList.add(dataTag);

                containerTag.put("data", dataList);
                containerTag.putInt("maxSpells", 1);
                containerTag.putBoolean("mustEquip", false);
                containerTag.putBoolean("spellWheel", false);

                tag.put("irons_spellbooks:spell_container", containerTag);
                spellScroll.setTag(tag);

                System.out.println("  [Assemble Output] Generated Scroll NBT: " + tag);
                return spellScroll;
            }
        }

        if (result != null && !result.isEmpty()) {
            return result.copy();
        }
        return ItemStack.EMPTY;
    }

    // ─── ゲッター ─────────────────────────────────────────────────────────
    // ⭕ 修正：メソッドの綴りを正確に変更（canCraftInDemensions -> canCraftInDimensions）
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess reg) {
        if (resultSpellId != null && !resultSpellId.isEmpty()) {
            var scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks:scroll"));
            if (scrollItem != null) {
                ItemStack spellScroll = new ItemStack(scrollItem, 1);
                CompoundTag tag = new CompoundTag();
                CompoundTag containerTag = new CompoundTag();
                net.minecraft.nbt.ListTag dataList = new net.minecraft.nbt.ListTag();
                CompoundTag dataTag = new CompoundTag();
                dataTag.putString("id", resultSpellId);
                dataTag.putByte("index", (byte) 0);
                dataTag.putShort("level", (short) resultSpellLevel);
                dataTag.putBoolean("locked", true);
                dataList.add(dataTag);
                containerTag.put("data", dataList);
                containerTag.putInt("maxSpells", 1);
                containerTag.putBoolean("mustEquip", false);
                containerTag.putBoolean("spellWheel", false);
                tag.put("irons_spellbooks:spell_container", containerTag);
                spellScroll.setTag(tag);
                return spellScroll;
            }
        }
        return result;
    }

    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ArcaneCraftingRecipeSerializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return ArcaneCraftingRecipeType.INSTANCE; }

    public boolean isCatalystConsumed() { return catalystConsume; }
    public NonNullList<Ingredient> getIngredients() { return ingredients; }
    public NonNullList<CompoundTag> getIngredientNBTs() { return ingredientNBTs; }
    public NonNullList<String> getRequiredSpells() { return requiredSpells; }
    public NonNullList<Integer> getRequiredLevels() { return requiredLevels; }
    public Ingredient getCatalyst() { return catalyst; }
    public CompoundTag getCatalystNBT() { return catalystNBT; }
    public ItemStack getResultItem() { return result; }
    public String getResultSpellId() { return resultSpellId; }
    public int getResultSpellLevel() { return resultSpellLevel; }
}