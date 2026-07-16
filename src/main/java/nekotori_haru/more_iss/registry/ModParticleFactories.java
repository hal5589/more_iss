package nekotori_haru.more_iss.registry;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
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
        // ★ registerSpriteSetに匿名クラスでSpriteParticleRegistrationを直接渡す
        event.registerSpriteSet(
                ModParticles.YAEZAKURA_SLASH.get(),
                new ParticleEngine.SpriteParticleRegistration<SimpleParticleType>() {
                    @Override
                    public ParticleProvider<SimpleParticleType> create(SpriteSet spriteSet) {
                        // ★ ここでProviderのインスタンスを返す
                        return new YaezakuraSlashParticle.Provider(spriteSet);
                    }
                }
        );
    }
}