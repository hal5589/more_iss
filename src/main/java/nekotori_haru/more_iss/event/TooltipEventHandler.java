package nekotori_haru.more_iss.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.api.SynthesisUpgradeData;
import nekotori_haru.more_iss.util.RenderUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

@Mod.EventBusSubscriber(modid = More_iss.MODID, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent
    public static void onTooltipGather(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        // アップグレード済みのアイテムかどうかを判定
        if (!SynthesisUpgradeData.isUpgraded(stack)) return;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) return;

        // 最初の行（アイテム名）を取得
        Either<FormattedText, TooltipComponent> first = elements.get(0);
        first.ifLeft(text -> {
            String originalName = text.getString();
            // 既にマーカーが付いていなければ追加
            if (!originalName.contains(RenderUtils.WAVE_MARKER)) {
                elements.set(0, Either.left(RenderUtils.withWaveMarker(originalName)));
            }
        });
    }
}