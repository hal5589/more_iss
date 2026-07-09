package nekotori_haru.more_iss.client.screen;

import nekotori_haru.more_iss.menu.RingOfThunderResonanceContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public class RingOfThunderResonanceScreen extends BaseRingScreen<RingOfThunderResonanceContainer> {

    public RingOfThunderResonanceScreen(RingOfThunderResonanceContainer container, Inventory inv, Component title) {
        super(container, inv, title);
    }
}