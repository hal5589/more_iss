package nekotori_haru.more_iss.client.screen;

import nekotori_haru.more_iss.menu.RingOfManaConversionContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public class RingOfManaConversionScreen extends BaseRingScreen<RingOfManaConversionContainer> {

    public RingOfManaConversionScreen(RingOfManaConversionContainer container, Inventory inv, Component title) {
        super(container, inv, title);
    }
}