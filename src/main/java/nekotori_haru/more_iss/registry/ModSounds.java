package nekotori_haru.more_iss.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "more_iss");

    // 八重桜 構え音（刀を構える）
    public static final RegistryObject<SoundEvent> YAEZAKURA_SET =
            SOUNDS.register("spell.yaezakura.set",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("more_iss", "spell.yaezakura.set")));

    // 八重桜 攻撃音（抜刀・斬撃）
    public static final RegistryObject<SoundEvent> YAEZAKURA_ATTACK =
            SOUNDS.register("spell.yaezakura.attack",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("more_iss", "spell.yaezakura.attack")));
}