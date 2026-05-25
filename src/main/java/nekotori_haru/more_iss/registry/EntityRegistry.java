package nekotori_haru.more_iss.registry;

import nekotori_haru.more_iss.entity.BaseBeamVisualEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    // 1. ForgeのDeferredRegisterを定義（MODID: "more_iss"）
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "more_iss");

    // 🌟 2. ここが足りていなかった可能性のある登録処理です！
    // 呪文側から「EntityRegistry.BASE_BEAM_VISUAL.get()」で呼び出せるようにします。
    public static final RegistryObject<EntityType<BaseBeamVisualEntity>> BASE_BEAM_VISUAL =
            ENTITIES.register("base_beam_visual", () -> EntityType.Builder.<BaseBeamVisualEntity>of(BaseBeamVisualEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)             // Entityの当たり判定のサイズ（見た目だけなので小さめでOK）
                    .clientTrackingRange(64)       // クライアントに同期する距離（マス）
                    .updateInterval(1)             // 同期するTick間隔（ビームのブレを防ぐため最速の1に）
                    .build("base_beam_visual"));

    // 3. メインクラス（Mod本体）のコンストラクタから呼び出して登録を有効化する用
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}