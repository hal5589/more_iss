package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import nekotori_haru.more_iss.item.RingOfManaConversionItem;
import nekotori_haru.more_iss.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public class ManaConversionHandler {

    // RingOfManaConversionItem と同じクールダウン時間を使用
    private static final int COOLDOWN_SECONDS = RingOfManaConversionItem.COOLDOWN_SECONDS;
    private static final int TICKS_PER_SECOND = 20;
    private static final int DURATION_SECONDS = 30;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        Optional<SlotResult> ringSlot = CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inv -> inv.findFirstCurio(ModItems.RING_OF_MANA_CONVERSION.get()));

        if (ringSlot.isEmpty()) return;

        ItemStack ringStack = ringSlot.get().stack();
        if (ringStack.isEmpty()) return;

        CompoundTag tag = ringStack.getOrCreateTag();
        long lastConvertTick = tag.getLong("LastConvertTick");
        long currentTick = player.tickCount;

        // ★ クールダウンはアイテムのクールダウンマネージャーで管理するため、
        //    ここでの時間チェックは削除（または併用しても可）
        //    ここではアイテムクールダウンに委譲するため、このチェックは削除する
        // if (currentTick - lastConvertTick < COOLDOWN_SECONDS * TICKS_PER_SECOND) return;

        boolean converted = performConversion(player, ringStack);
        if (converted) {
            tag.putLong("LastConvertTick", currentTick);
        }
    }

    private boolean performConversion(Player player, ItemStack ringStack) {
        int totalCircuits = RingOfManaConversionItem.getCircuitCount(ringStack);
        if (totalCircuits <= 0) return false;

        // 1回路 = 3ハート
        int heartsToGive = totalCircuits * 3;

        // アンプリファイア計算（吸収バフの仕様：付与ハート数 = 2 × (amplifier + 1)）
        int amplifier = (heartsToGive / 2) - 1;
        if (amplifier < 0) amplifier = 0;

        // マナコスト = (amplifier + 1) × 75
        int manaCost = (amplifier + 1) * 75;

        MagicData magicData = MagicData.getPlayerMagicData(player);
        float currentMana = magicData.getMana();

        if (currentMana < manaCost) {
            return false;
        }

        // マナを消費（回路は消費しない）
        magicData.setMana(currentMana - manaCost);

        // ★ 吸収バフを適用（上書き）
        int duration = DURATION_SECONDS * 20;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));

        // ★★★ 変換が成功したら、ここでクールダウンを設定 ★★★
        // ホットバーにエンダーパール風のゲージを表示
        player.getCooldowns().addCooldown(ringStack.getItem(), COOLDOWN_SECONDS * TICKS_PER_SECOND);

        return true;
    }
}