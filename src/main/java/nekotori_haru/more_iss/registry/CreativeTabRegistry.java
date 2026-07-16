package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import nekotori_haru.more_iss.More_iss;
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

                output.accept(ModBlocks.ARCANE_CRAFTING_TABLE_ITEM.get());

                //素材
                output.accept(ModItems.PHOENIX_FALLEN_FEATHER.get());
                output.accept(ModItems.BLAZING_NOVA.get());
                output.accept(ModItems.RECORD_OF_THE_GODS.get());
                output.accept(ModItems.PRIMAL_CORE.get());
                output.accept(ModItems.ABSOLUTE_ZERO_SHARD.get());
                output.accept(ModItems.FORBIDDEN_GRIMOIRE.get());
                output.accept(ModItems.MANA_CIRCUIT.get());

                //装備
                output.accept(ModItems.RING_OF_SYNTHESIS.get());
                output.accept(ModItems.RING_OF_MANA_CYCLE.get());
                output.accept(ModItems.SPELLBOOK_OF_CONCENTRATION.get());

                //アプグレ
                output.accept(ModItems.SYNTHESIS_UPGRADE_ORB.get());

                //火
                addSpellScroll(output, ModSpells.FLAME_RAY.get());
                addSpellScroll(output, ModSpells.MARK_OF_DETONATION.get());
                addSpellScroll(output, ModSpells.NAPALM_RAIN.get());
                addSpellScroll(output, ModSpells.PHOENIX_BLESSING.get());
                addSpellScroll(output, ModSpells.METEOR_FALL.get());
                addSpellScroll(output, ModSpells.SUMMON_PYROMANCER.get());
                addSpellScroll(output, ModSpells.INFERNO_STEP.get()); // ★ 追加

                //氷
                addSpellScroll(output, ModSpells.FROST_ARMOR.get());
                addSpellScroll(output, ModSpells.GLACIAL_EXECUTION.get());
                addSpellScroll(output, ModSpells.ABSOLUTE_ZERO.get());
                addSpellScroll(output, ModSpells.SUMMON_CRYOMANCER.get()); // 重複を削除（元は2回）
                addSpellScroll(output, ModSpells.CRYO_CONVERGENCE.get());   // ★ 追加（重複していた行をこちらに置き換え）

                //雷
                addSpellScroll(output, ModSpells.RAISEN.get());
                addSpellScroll(output, ModSpells.PLASMA_STEP.get()); // ★ 追加

                //自然
                addSpellScroll(output, ModSpells.SOLAR_RAY.get());
                addSpellScroll(output, ModSpells.UNFADING.get());
                addSpellScroll(output, ModSpells.SUMMON_APOTHECARIST.get());
                addSpellScroll(output, ModSpells.YAEZAKURA.get()); // ★ 追加

                //聖
                addSpellScroll(output, ModSpells.HOLY_RAY.get());
                addSpellScroll(output, ModSpells.HEAVENLY_BLAST.get());
                addSpellScroll(output, ModSpells.PROVIDENTIAL_CONDUIT.get());
                addSpellScroll(output, ModSpells.SUMMON_PRIEST.get());

                //エンダー
                addSpellScroll(output, ModSpells.ENDER_SHOOTING_STAR.get());
                addSpellScroll(output, ModSpells.FREISCHUTZ.get());
                addSpellScroll(output, ModSpells.VOID_RAY.get());

                //召喚
                addSpellScroll(output, ModSpells.SPECTAL_RAY.get());
                addSpellScroll(output, ModSpells.SUMMON_ARCHEVOKER.get());

                //エルドリッチ
                addSpellScroll(output, ModSpells.SOUL_LINK.get());
                addSpellScroll(output, ModSpells.OBLIVION.get()); // ★ 追加

                //融合
                addSpellScroll(output, ModSpells.OVERBURST_BLOOD.get());
                addSpellScroll(output, ModSpells.SACRIFICIAL_EDGE.get());
                addSpellScroll(output, ModSpells.DISINTEGRATION.get());
                addSpellScroll(output, ModSpells.FUNNEL.get());
                addSpellScroll(output, ModSpells.POLYCHROMATIC_LANCE.get());
                addSpellScroll(output, ModSpells.POLYCHROMATIC_BEAM.get());
                addSpellScroll(output, ModSpells.SUMMON_WIZARDS.get());
                addSpellScroll(output, ModSpells.STARLIGHT.get());              // ★ 追加
                addSpellScroll(output, ModSpells.SEVEN_COLORED_CAGE.get());     // ★ 追加
                addSpellScroll(output, ModSpells.LITTLE_MONOLITH.get());        // ★ 追加
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