package nekotori_haru.more_iss.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer; // ResultContainerをインポート
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("inputSlots")
    Container getInputSlots();

    @Accessor("resultSlots")
    ResultContainer getResultSlots();
}