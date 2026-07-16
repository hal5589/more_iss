package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import nekotori_haru.more_iss.item.RingOfManaFurnaceItem;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public class ManaFurnaceHandler {

    private static final int BUFF_COST = 1000;
    private static final int BUFF_DURATION = 2 * 20;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        Optional<SlotResult> ringSlot = CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inv -> inv.findFirstCurio(ModItems.RING_OF_MANA_FURNACE.get()));

        if (ringSlot.isEmpty()) return;

        ItemStack ringStack = ringSlot.get().stack();
        if (ringStack.isEmpty()) return;

        int circuits = RingOfManaFurnaceItem.getCircuitCount(ringStack);
        if (circuits <= 0) return;

        // ★ バフ発動中はチャージ蓄積をスキップ
        if (player.hasEffect(ModEffects.MANA_FURNACE_POWER.get())) {
            return;
        }

        boolean isActive = RingOfManaFurnaceItem.isActive(ringStack);

        // ★ 起動中モードなら、チャージ蓄積（マナ消費）を行わない
        if (isActive) {
            // チャージ蓄積はせず、バフ発動のみチェック
            int charge = RingOfManaFurnaceItem.getCharge(ringStack);
            if (charge >= BUFF_COST) {
                RingOfManaFurnaceItem.setCharge(ringStack, charge - BUFF_COST);
                int amplifier = Math.min(circuits - 1, 10);
                if (amplifier < 0) amplifier = 0;
                player.addEffect(new MobEffectInstance(
                        ModEffects.MANA_FURNACE_POWER.get(),
                        BUFF_DURATION,
                        amplifier,
                        false,
                        true,
                        true
                ));
            }
            return; // ★ 起動中はここで終了（チャージ蓄積しない）
        }

        // ★ ここからは停止中モードの処理（マナを消費してチャージを溜める）
        double chargePerTick = circuits * 0.5;
        int chargeToAdd = (int) Math.floor(chargePerTick);
        if (chargeToAdd < 1) chargeToAdd = 1;

        MagicData magicData = MagicData.getPlayerMagicData(player);
        float currentMana = magicData.getMana();

        if (currentMana >= chargeToAdd) {
            magicData.setMana(currentMana - chargeToAdd);
            int charge = RingOfManaFurnaceItem.getCharge(ringStack);
            int newCharge = Math.min(charge + chargeToAdd, RingOfManaFurnaceItem.MAX_CHARGE);
            RingOfManaFurnaceItem.setCharge(ringStack, newCharge);
        }
    }
}