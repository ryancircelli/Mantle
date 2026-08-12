package slimeknights.mantle.network;

import net.minecraft.network.protocol.PacketFlow;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
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
   * Registers packets into this network. Safe to call from any setup event, as the channel is not built until
   * {@link NetworkWrapper#registerPayloads} runs.
   */
  public static void registerPackets() {
    // each packet is identified on the wire by the ID passed here, so these calls may be reordered or made
    // conditionally; only renaming one is a protocol change
    INSTANCE.registerPacket(UpdateHeldPagePacket.ID, UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.ID, UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(UpdateLecternPagePacket.ID, UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, PacketFlow.SERVERBOUND);
    INSTANCE.registerPacket(DropLecternBookPacket.ID, DropLecternBookPacket.class, DropLecternBookPacket::new, PacketFlow.SERVERBOUND);
    // TODO(M-client): restore once slimeknights.mantle.client.book ports
    // INSTANCE.registerPacket(OpenNamedBookPacket.ID, OpenNamedBookPacket.class, OpenNamedBookPacket::new, PacketFlow.CLIENTBOUND);
    // TODO(M-capability): restore once util/OffhandCooldownTracker ports
    // INSTANCE.registerPacket(SwingArmPacket.ID, SwingArmPacket.class, SwingArmPacket::new, PacketFlow.CLIENTBOUND);
    // TODO(M-fluid): restore once slimeknights.mantle.fluid.transfer ports
    // INSTANCE.registerPacket(FluidContainerTransferPacket.ID, FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, PacketFlow.CLIENTBOUND);
  }
}
