package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.spell.synthesis.PolychromaticLanceSpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, More_iss.MODID);

    public static final RegistryObject<CreativeModeTab> MORE_ISS_TAB = TABS.register("more_iss_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.more_iss"))
            .icon(() -> new ItemStack(ModBlocks.ARCANE_CRAFTING_TABLE_ITEM.get()))
            .displayItems((parameters, output) -> {
                // ===== ブロック =====
                output.accept(ModBlocks.ARCANE_CRAFTING_TABLE_ITEM.get());

                // ===== 火系統 =====
                addSpellScroll(output, ModSpells.FLAME_RAY.get());
                addSpellScroll(output, ModSpells.MARK_OF_DETONATION.get());
                addSpellScroll(output, ModSpells.NAPALM_RAIN.get());
                addSpellScroll(output, ModSpells.PHOENIX_BLESSING.get());
                addSpellScroll(output, ModSpells.METEOR_FALL.get());

                // ===== 氷系統 =====
                addSpellScroll(output, ModSpells.FROST_ARMOR.get());
                addSpellScroll(output, ModSpells.GLACIAL_EXECUTION.get());
                addSpellScroll(output, ModSpells.ABSOLUTE_ZERO.get());

                // ===== 雷系統 =====
                addSpellScroll(output, ModSpells.RAISEN.get());

                // ===== 自然系統 =====
                addSpellScroll(output, ModSpells.SOLAR_RAY.get());
                addSpellScroll(output, ModSpells.UNFADING.get());

                // ===== 聖系統 =====
                addSpellScroll(output, ModSpells.HOLY_RAY.get());
                addSpellScroll(output, ModSpells.HEAVENLY_BLAST.get());
                addSpellScroll(output, ModSpells.PROVIDENTIAL_CONDUIT.get());

                // ===== エンダー系統 =====
                addSpellScroll(output, ModSpells.ENDER_SHOOTING_STAR.get());
                addSpellScroll(output, ModSpells.FREISCHUTZ.get());
                addSpellScroll(output, ModSpells.VOID_RAY.get());

                // ===== 召喚系統 =====
                addSpellScroll(output, ModSpells.SPECTAL_RAY.get());

                // ===== エルドリッチ系統 =====
                addSpellScroll(output, ModSpells.SOUL_LINK.get());

                // ===== 合成系統 =====
                addSpellScroll(output, ModSpells.OVERBURST_BLOOD.get());
                addSpellScroll(output, ModSpells.SACRIFICIAL_EDGE.get());
                addSpellScroll(output, ModSpells.DISINTEGRATION.get());
                addSpellScroll(output, ModSpells.FUNNEL.get());
                addSpellScroll(output, ModSpells.POLYCHROMATIC_LANCE.get());
                addSpellScroll(output, ModSpells.POLYCHROMATIC_BEAM.get());

            })
            .build());

    private static void addSpellScroll(CreativeModeTab.Output output, AbstractSpell spell) {
        if (spell == null) return;
        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); ++level) {
            ItemStack scrollStack = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, level, scrollStack);
            output.accept(scrollStack);
        }
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}