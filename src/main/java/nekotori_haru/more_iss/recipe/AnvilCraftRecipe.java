package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess; // ← 解決できないエラーの対策
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AnvilCraftRecipe implements Recipe<Container> {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "more_iss");

    public static final RegistryObject<RecipeSerializer<AnvilCraftRecipe>> SERIALIZER =
            SERIALIZERS.register("anvil_craft", Serializer::new);

    public static final RecipeType<AnvilCraftRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() { return "more_iss:anvil_craft"; }
    };

    private final ResourceLocation id;
    private final String inputId1;
    private final String inputId2;
    private final String outputSpellId;

    public AnvilCraftRecipe(ResourceLocation id, String inputId1, String inputId2, String outputSpellId) {
        this.id = id;
        this.inputId1 = inputId1;
        this.inputId2 = inputId2;
        this.outputSpellId = outputSpellId;
    }

    public boolean matchesSpells(String id1, String id2) {
        return (this.inputId1.equals(id1) && this.inputId2.equals(id2)) ||
                (this.inputId1.equals(id2) && this.inputId2.equals(id1));
    }

    public String getOutputSpellId() { return this.outputSpellId; }
    @Override public ResourceLocation getId() { return this.id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }

    // 1.20.1の正規メソッド名・引数名に完全修正
    @Override public boolean matches(Container pContainer, Level pLevel) { return false; }
    @Override public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }

    // --- Serializer (1.20.1仕様) ---
    public static class Serializer implements RecipeSerializer<AnvilCraftRecipe> {

        // 修正：メソッド名を「pNode」から「fromJson」へ変更
        @Override
        public AnvilCraftRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String input1 = GsonHelper.getAsString(json, "input_spell_1");
            String input2 = GsonHelper.getAsString(json, "input_spell_2");
            String output = GsonHelper.getAsString(json, "output_spell");
            return new AnvilCraftRecipe(recipeId, input1, input2, output);
        }

        @Override
        public AnvilCraftRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new AnvilCraftRecipe(recipeId, buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilCraftRecipe recipe) {
            buffer.writeUtf(recipe.inputId1);
            buffer.writeUtf(recipe.inputId2);
            buffer.writeUtf(recipe.outputSpellId);
        }
    }
}