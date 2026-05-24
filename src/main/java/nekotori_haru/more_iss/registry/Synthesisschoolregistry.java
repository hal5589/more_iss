package nekotori_haru.more_iss.registry;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 融合（Synthesis）スクールの登録。
 *
 * More_iss.class の static initializer で
 *   SCHOOLS.register(modEventBus)
 * を呼び出してください。
 *
 * ゲーム内テキストキー "school.more_iss.synthesis" は
 * ja_jp.json に既に「融合」として定義済みです。
 */
public class SynthesisSchoolRegistry {

    /**
     * SchoolRegistry が DeferredRegister<SchoolType> を使う場合のキー。
     * ISS 2.x では SchoolRegistry.REGISTRY.get().getRegistryKey() で取得します。
     */
    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(
                    SchoolRegistry.REGISTRY.get().getRegistryKey(),
                    "more_iss"
            );

    /**
     * 融合スクールのリソースロケーション。
     * 既存の SchoolRegistry.ICE_RESOURCE などと同じ形式。
     * 呪文クラスの setSchoolResource() に渡します。
     */
    public static final ResourceLocation SYNTHESIS_RESOURCE =
            new ResourceLocation("more_iss", "synthesis");

    /**
     * 融合スクール本体の登録。
     * カラー: ゴールド (#FFD700)
     * spellPower 属性 / magicResist 属性 / 詠唱音は null → デフォルト動作。
     */
    public static final RegistryObject<SchoolType> SYNTHESIS = SCHOOLS.register("synthesis",
            () -> new SchoolType(
                    Component.translatable("school.more_iss.synthesis")
                            .withStyle(Style.EMPTY.withColor(0xFFD700)),
                    null,   // spell power attribute
                    null,   // magic resist attribute
                    null    // cast sound
            )
    );
}