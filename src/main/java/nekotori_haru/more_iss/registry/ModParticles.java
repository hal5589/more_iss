package nekotori_haru.more_iss.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nekotori_haru.more_iss.More_iss;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, More_iss.MODID);

    public static final RegistryObject<SimpleParticleType> YAEZAKURA_SLASH =
            PARTICLES.register("yaezakura_slash", () -> new SimpleParticleType(true));
}