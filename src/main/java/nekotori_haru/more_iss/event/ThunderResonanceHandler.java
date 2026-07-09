package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import nekotori_haru.more_iss.item.RingOfThunderResonanceItem;
import nekotori_haru.more_iss.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThunderResonanceHandler {

    // 再帰防止フラグ（プレイヤーごとに管理し、増幅処理中に発生する
    // MobEffectEvent.Added の再入を防ぐ）
    private static final Map<UUID, Boolean> processingMap = new ConcurrentHashMap<>();

    // ★ 「次のtickで増幅を適用する」ための保留キュー
    //    key: プレイヤーUUID, value: 追加する回路数分の増幅量
    private static final Map<UUID, Integer> pendingAmplifyMap = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();

        // 再帰防止（増幅処理中に発生した Added イベントは無視）
        if (processingMap.getOrDefault(uuid, false)) return;

        MobEffectInstance newEffect = event.getEffectInstance();
        if (newEffect == null) return;
        if (!newEffect.getEffect().equals(MobEffectRegistry.CHARGED.get())) return;

        Optional<SlotResult> ringSlot = CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inv -> inv.findFirstCurio(ModItems.RING_OF_THUNDER_RESONANCE.get()));
        if (ringSlot.isEmpty()) return;

        ItemStack ringStack = ringSlot.get().stack();
        if (ringStack.isEmpty()) return;

        int totalCircuits = RingOfThunderResonanceItem.getCircuitCount(ringStack);
        if (totalCircuits <= 0) return;

        int manaCost = totalCircuits * 100;
        MagicData magicData = MagicData.getPlayerMagicData(player);
        float currentMana = magicData.getMana();
        if (currentMana < manaCost) return;
        magicData.setMana(currentMana - manaCost);

        // ★ ここでは上書きせず、「次のtickで増幅する」という予約だけ行う
        //    同一tick内に複数回帯電が付与された場合は加算しておく
        pendingAmplifyMap.merge(uuid, totalCircuits, Integer::sum);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        Integer pending = pendingAmplifyMap.remove(uuid);
        if (pending == null || pending <= 0) return;

        MobEffectInstance currentCharged = player.getEffect(MobEffectRegistry.CHARGED.get());
        if (currentCharged == null) {
            // 元のCHARGEDが既に切れていた場合は増幅しようがないので何もしない
            return;
        }

        int newAmplifier = currentCharged.getAmplifier() + pending;
        int duration = currentCharged.getDuration();

        // ★ 再帰防止フラグを立ててから上書き
        processingMap.put(uuid, true);
        try {
            player.addEffect(new MobEffectInstance(
                    MobEffectRegistry.CHARGED.get(),
                    duration,
                    newAmplifier,
                    currentCharged.isAmbient(),
                    currentCharged.isVisible(),
                    currentCharged.showIcon()
            ));
        } finally {
            processingMap.remove(uuid);
        }
    }
}