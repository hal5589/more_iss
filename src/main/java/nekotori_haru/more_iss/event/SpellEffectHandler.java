package nekotori_haru.more_iss.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "more_iss")
public class SpellEffectHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getTags().contains("more_iss.frozen_ai")) {
            CompoundTag nbt = entity.getPersistentData();
            int ticksLeft = nbt.getInt("more_iss.frozen_ticks");

            if (ticksLeft > 0) {
                // 移動ベクトルを毎Tick上書きして完全にフリーズ（ジャンプ・落下・移動すべて拒否）
                entity.setDeltaMovement(0, 0, 0);
                nbt.putInt("more_iss.frozen_ticks", ticksLeft - 1);
            } else {
                // 時間切れでタグとカウンターを安全に削除
                entity.removeTag("more_iss.frozen_ai");
                nbt.remove("more_iss.frozen_ticks");
            }
        }
    }
}