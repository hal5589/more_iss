package nekotori_haru.more_iss.registry;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.client.particle.YaezakuraSlashParticle;

@Mod.EventBusSubscriber(modid = More_iss.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModParticleFactories {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        Minecraft.getInstance().particleEngine.register(
                ModParticles.YAEZAKURA_SLASH.get(),
                YaezakuraSlashParticle.Provider::new
        );
    }
}