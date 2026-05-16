package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * 秘術の金床でのカスタムレシピを処理するイベントハンドラー。
 *
 * inputSlots / resultSlots は ItemCombinerMenu で protected のため、
 * Reflection でアクセスする。
 * ServerTickEvent でプレイヤーのメニューを毎 tick 監視し、
 * 入力条件が揃ったとき result を上書きする。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArcaneAnvilRecipeHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    // ---- レシピ定義 ----------------------------------------
    private static final String SPELL_BASE   = "irons_spellbooks:electrify";
    private static final int              LEVEL_BASE   = 3;
    private static final String SPELL_ADD    = "irons_spellbooks:heartstop";
    private static final int              LEVEL_ADD    = 5;
    private static final String SPELL_OUTPUT_STR = "more_iss:overburst_blood";
    private static final int              LEVEL_OUTPUT = 1;
    private static final ResourceLocation SPELL_OUTPUT = new ResourceLocation("more_iss", "overburst_blood");
    // -------------------------------------------------------

    private static Field inputSlotsField  = null;
    private static Field resultSlotsField = null;

    static {
        try {
            Class<?> combinerClass = net.minecraft.world.inventory.ItemCombinerMenu.class;
            inputSlotsField  = combinerClass.getDeclaredField("inputSlots");
            resultSlotsField = combinerClass.getDeclaredField("resultSlots");
            inputSlotsField.setAccessible(true);
            resultSlotsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.error("[more_iss] failed to reflect ItemCombinerMenu fields", e);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ArcaneAnvilMenu menu) {
                tryApplyCustomRecipe(menu);
            }
        }
    }

    private static void tryApplyCustomRecipe(ArcaneAnvilMenu menu) {
        if (inputSlotsField == null || resultSlotsField == null) return;

        try {
            Container inputSlots  = (Container) inputSlotsField.get(menu);
            Container resultSlots = (Container) resultSlotsField.get(menu);

            ItemStack left  = inputSlots.getItem(0);
            ItemStack right = inputSlots.getItem(1);

            if (left.isEmpty() || right.isEmpty()) return;
            if (!(left.getItem() instanceof Scroll)) return;
            if (!(right.getItem() instanceof Scroll)) return;

            SpellData leftSpell  = ISpellContainer.get(left).getSpellAtIndex(0);
            SpellData rightSpell = ISpellContainer.get(right).getSpellAtIndex(0);

            if (leftSpell == null || rightSpell == null) return;

            AbstractSpell ls = leftSpell.getSpell();
            AbstractSpell rs = rightSpell.getSpell();
            if (ls == null || rs == null) return;

            String leftId  = ls.getSpellId();
            String rightId = rs.getSpellId();

            boolean match =
                    (SPELL_BASE.equals(leftId)  && leftSpell.getLevel()  == LEVEL_BASE
                            && SPELL_ADD.equals(rightId)   && rightSpell.getLevel() == LEVEL_ADD)
                            ||
                            (SPELL_ADD.equals(leftId)    && leftSpell.getLevel()  == LEVEL_ADD
                                    && SPELL_BASE.equals(rightId)  && rightSpell.getLevel() == LEVEL_BASE);

            if (!match) return;

            // すでに正しい出力が入っていれば上書きしない
            ItemStack current = resultSlots.getItem(0);
            if (!current.isEmpty() && current.getItem() instanceof Scroll) {
                SpellData cur = ISpellContainer.get(current).getSpellAtIndex(0);
                if (cur != null && cur.getSpell() != null
                        && SPELL_OUTPUT_STR.equals(cur.getSpell().getSpellId())
                        && cur.getLevel() == LEVEL_OUTPUT) {
                    return;
                }
            }

            ItemStack output = buildOutputScroll();
            if (!output.isEmpty()) {
                resultSlots.setItem(0, output);
            }

        } catch (IllegalAccessException e) {
            LOGGER.error("[more_iss] ArcaneAnvilRecipeHandler reflect failed", e);
        }
    }

    private static ItemStack buildOutputScroll() {
        AbstractSpell spell = SpellRegistry.getSpell(SPELL_OUTPUT);
        if (spell == null) {
            LOGGER.error("[more_iss] ArcaneAnvilRecipeHandler: spell not found: {}", SPELL_OUTPUT);
            return ItemStack.EMPTY;
        }
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, LEVEL_OUTPUT, scroll);
        return scroll;
    }
}