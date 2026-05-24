package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SynthesisSchoolRegistry {

    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(
                    SchoolRegistry.SCHOOL_REGISTRY_KEY,
                    "more_iss"
            );

    public static final ResourceLocation SYNTHESIS_RESOURCE =
            new ResourceLocation("more_iss", "synthesis");

    public static final RegistryObject<SchoolType> SYNTHESIS = SCHOOLS.register("synthesis",
            () -> {
                MutableComponent schoolName = Component.translatable("school.more_iss.synthesis")
                        .withStyle(Style.EMPTY.withColor(0xFFD700));

                // バニラの既存サウンドをHolderで取得（完全無音は不可のためダミー）
                // SoundEvents.INTENTIONALLY_EMPTY は音量0の特殊サウンド
                Holder<SoundEvent> sound = ForgeRegistries.SOUND_EVENTS.getHolder(
                        new ResourceLocation("minecraft", "intentionally_empty")
                ).orElse(ForgeRegistries.SOUND_EVENTS.getDelegateOrThrow(
                        new ResourceLocation("minecraft", "ui.button.click")
                ));

                TagKey<Item> focusTag = TagKey.create(
                        Registries.ITEM,
                        new ResourceLocation("more_iss", "synthesis_focus")
                );

                Holder<Attribute> power = ForgeRegistries.ATTRIBUTES.getHolder(
                        ModAttributes.SYNTHESIS_SPELL_POWER.getId()
                ).orElseThrow();

                Holder<Attribute> resist = ForgeRegistries.ATTRIBUTES.getHolder(
                        ModAttributes.SYNTHESIS_MAGIC_RESIST.getId()
                ).orElseThrow();

                return new SchoolType(
                        SYNTHESIS_RESOURCE,
                        focusTag,
                        schoolName,
                        power,
                        resist,
                        sound,
                        ISSDamageTypes.FIRE_MAGIC
                );
            }
    );
}