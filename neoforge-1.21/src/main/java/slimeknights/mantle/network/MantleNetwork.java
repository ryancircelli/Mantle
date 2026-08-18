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
   * 3: 1.21 - Packets became payloads, so every packet's encoding and framing changed
   */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "3");

  /**
   * Registers packets into this network. Safe to call from any setup event, as the payloads are not built until
   * {@link slimeknights.mantle.platform.neoforge.NeoForgePacketTransport#registerPayloads} runs.
   */
  public static void registerPackets() {
    // each packet is identified on the wire by the ID passed here, so these calls may be reordered or made
    // conditionally; only renaming one is a protocol change
    INSTANCE.registerPacket(OpenLecternBookPacket.ID, OpenLecternBookPacket.class, OpenLecternBookPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(UpdateHeldPagePacket.ID, UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.ID, UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(UpdateLecternPagePacket.ID, UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(DropLecternBookPacket.ID, DropLecternBookPacket.class, DropLecternBookPacket::new, PacketDirection.SERVERBOUND);
    INSTANCE.registerPacket(OpenNamedBookPacket.ID, OpenNamedBookPacket.class, OpenNamedBookPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(SwingArmPacket.ID, SwingArmPacket.class, SwingArmPacket::new, PacketDirection.CLIENTBOUND);
    INSTANCE.registerPacket(FluidContainerTransferPacket.ID, FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, PacketDirection.CLIENTBOUND);
  }
}
