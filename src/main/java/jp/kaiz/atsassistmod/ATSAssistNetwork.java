package jp.kaiz.atsassistmod;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import jp.kaiz.atsassistmod.gui.ATSAssistGUIHandler;
import jp.kaiz.atsassistmod.network.*;

import static jp.kaiz.atsassistmod.ATSAssistCore.NETWORK_WRAPPER;

public class ATSAssistNetwork {
    public void init() {
        NETWORK_WRAPPER.registerMessage(PacketSetNotch.class, PacketSetNotch.class, 0, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(PacketSetNotchController.class, PacketSetNotchController.class, 1, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(PacketSetTrainState.class, PacketSetTrainState.class, 2, Side.SERVER);

        NETWORK_WRAPPER.registerMessage(PacketGroundUnitTileInit.class, PacketGroundUnitTileInit.class, 10, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(PacketGroundUnitTileInitToClient.class, PacketGroundUnitTileInit.class, 11, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(PacketGroundUnitTile.class, PacketGroundUnitTile.class, 12, Side.SERVER);

        NETWORK_WRAPPER.registerMessage(PacketTrainControllerToClient.class, PacketTrainControllerToClient.class, 20, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(PacketTrainProtectionSetter.class, PacketTrainProtectionSetter.class, 21, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(PacketTrainDriveMode.class, PacketTrainDriveMode.class, 22, Side.SERVER);

        NetworkRegistry.INSTANCE.registerGuiHandler(ATSAssistCore.INSTANCE, new ATSAssistGUIHandler());
    }
}
