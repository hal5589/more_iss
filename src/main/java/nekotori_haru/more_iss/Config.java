package nekotori_haru.more_iss;

import nekotori_haru.more_iss.registry.MoreIssConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = More_iss.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)  // ← MODID に修正
public class Config {

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        float cap = MoreIssConfig.getDamageCap();
        // 必要に応じて処理
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        float cap = MoreIssConfig.getDamageCap();
        // 必要に応じて処理
    }
}