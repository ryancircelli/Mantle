package slimeknights.mantle.network;

import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferPacket;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.OpenLecternBookPacket;
import slimeknights.mantle.network.packet.OpenNamedBookPacket;
import slimeknights.mantle.network.packet.SwingArmPacket;
import slimeknights.mantle.network.packet.UpdateHeldPagePacket;
import slimeknights.mantle.network.packet.UpdateInventoryPagePacket;
import slimeknights.mantle.network.packet.UpdateLecternPagePacket;

public class MantleNetwork {
  /**
   * Network instance
   * 1: 1.11.101 and before
   * 2: 1.11.102 - New predicate types, enum loadable nullable field optimization
   */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "2");

  /**
   * Registers packets into this network
   */
  public static void registerPackets() {
    // the order of these calls is the wire format: each packet is indexed by its position here, so append, never insert
    INSTANCE.registerPacket(OpenLecternBookPacket.ID, OpenLecternBookPacket.class, OpenLecternBookPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(UpdateHeldPagePacket.ID, UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.ID, UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(UpdateLecternPagePacket.ID, UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(DropLecternBookPacket.ID, DropLecternBookPacket.class, DropLecternBookPacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(SwingArmPacket.ID, SwingArmPacket.class, SwingArmPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(OpenNamedBookPacket.ID, OpenNamedBookPacket.class, OpenNamedBookPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(FluidContainerTransferPacket.ID, FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, PacketDirection.CLIENTBOUND);
  }
}
