package slimeknights.mantle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests that a channel's packet identities are unique and ordered, without building a channel */
class PacketRegistryTest {
  private static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("mantle", "network");

  /** Packet with no state, only ever used to fill a registration */
  private static class EmptyPacket implements IPacket {
    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {}

    @Override
    public void handle(PacketContext context) {}
  }

  /** Second packet class, to check the class based checks */
  private static class OtherPacket extends EmptyPacket {}

  private static PacketRegistration<?> registration(ResourceLocation id, Class<? extends EmptyPacket> clazz) {
    return registrationOf(id, clazz);
  }

  private static <P extends EmptyPacket> PacketRegistration<P> registrationOf(ResourceLocation id, Class<P> clazz) {
    return new PacketRegistration<>(id, clazz, IPacket::encode, buffer -> null, IPacket::handle, PacketFlow.CLIENTBOUND);
  }


  /* Uniqueness */

  @Test
  void register_duplicateIdFails() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "packet"), EmptyPacket.class));
    assertThatThrownBy(() -> registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "packet"), OtherPacket.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Duplicate packet ID mantle:packet")
      .hasMessageContaining(EmptyPacket.class.getName());
  }

  @Test
  void register_duplicateClassFails() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "first"), EmptyPacket.class));
    assertThatThrownBy(() -> registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "second"), EmptyPacket.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Duplicate packet class " + EmptyPacket.class.getName())
      .hasMessageContaining("mantle:first");
  }

  @Test
  void register_differentIdsAndClassesPass() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "first"), EmptyPacket.class));
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "second"), OtherPacket.class));
    assertThat(registry.size()).isEqualTo(2);
  }


  /* Ordering, which no longer reaches the wire but still has to be deterministic */

  @Test
  void ids_keepRegistrationOrder() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "zebra"), EmptyPacket.class));
    registry.register(registration(ResourceLocation.fromNamespaceAndPath("mantle", "aardvark"), OtherPacket.class));
    assertThat(registry.ids()).containsExactly(
      ResourceLocation.fromNamespaceAndPath("mantle", "zebra"),
      ResourceLocation.fromNamespaceAndPath("mantle", "aardvark"));
  }


  /* Lookup */

  @Test
  void get_findsTheRegistration() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mantle", "packet");
    PacketRegistration<EmptyPacket> registration = registrationOf(id, EmptyPacket.class);
    registry.register(registration);
    assertThat(registry.get(id)).isSameAs(registration);
    assertThat(registry.contains(id)).isTrue();
  }

  @Test
  void get_findsTheRegistrationByClass() {
    // sending a packet looks it up by class, which is how an unregistered packet is caught before it is encoded
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    PacketRegistration<EmptyPacket> registration = registrationOf(ResourceLocation.fromNamespaceAndPath("mantle", "packet"), EmptyPacket.class);
    registry.register(registration);
    assertThat(registry.get(EmptyPacket.class)).isSameAs(registration);
    assertThat(registry.get(OtherPacket.class)).isNull();
  }

  @Test
  void get_missingIsNull() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    assertThat(registry.get(ResourceLocation.fromNamespaceAndPath("mantle", "packet"))).isNull();
    assertThat(registry.contains(ResourceLocation.fromNamespaceAndPath("mantle", "packet"))).isFalse();
  }


  /* Payload identity */

  @Test
  void payloadType_carriesTheDeclaredId() {
    // the identifier a call site declares is the name the packet answers to on the wire, with nothing in between
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mantle", "packet");
    PacketRegistration<EmptyPacket> registration = registrationOf(id, EmptyPacket.class);
    assertThat(registration.payloadType().id()).isEqualTo(id);
    assertThat(registration.id()).isEqualTo(id);
    assertThat(registration.wrap(new EmptyPacket()).type()).isEqualTo(registration.payloadType());
  }


  /* Derived identifiers, the fallback for a channel that has not been migrated */

  @Test
  void deriveId_usesTheChannelNamespace() {
    PacketRegistry registry = new PacketRegistry(ResourceLocation.fromNamespaceAndPath("othermod", "network"));
    assertThat(registry.deriveId(EmptyPacket.class)).isEqualTo(ResourceLocation.fromNamespaceAndPath("othermod", "empty"));
  }

  @Test
  void defaultPath_dropsThePacketSuffix() {
    assertThat(PacketRegistry.defaultPath(EmptyPacket.class)).isEqualTo("empty");
  }

  @Test
  void defaultPath_convertsCamelCase() {
    assertThat(PacketRegistry.defaultPath(CamelCaseNamePacket.class)).isEqualTo("camel_case_name");
  }

  @Test
  void defaultPath_keepsAcronymsTogether() {
    assertThat(PacketRegistry.defaultPath(NBTKeyPacket.class)).isEqualTo("nbt_key");
  }

  @Test
  void defaultPath_keepsDigitsInTheirWord() {
    assertThat(PacketRegistry.defaultPath(Update2PagePacket.class)).isEqualTo("update2_page");
  }

  @Test
  void defaultPath_withoutTheSuffixUsesTheWholeName() {
    assertThat(PacketRegistry.defaultPath(NoSuffix.class)).isEqualTo("no_suffix");
  }

  @Test
  void defaultPath_suffixOnlyNameIsKept() {
    assertThat(PacketRegistry.defaultPath(Packet.class)).isEqualTo("packet");
  }

  @Test
  void defaultPath_anonymousClassFails() {
    IPacket anonymous = new IPacket() {
      @Override
      public void encode(RegistryFriendlyByteBuf buffer) {}

      @Override
      public void handle(PacketContext context) {}
    };
    assertThatThrownBy(() -> PacketRegistry.defaultPath(anonymous.getClass()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("anonymous class");
  }

  private static class CamelCaseNamePacket {}
  private static class NBTKeyPacket {}
  private static class Update2PagePacket {}
  private static class NoSuffix {}
  private static class Packet {}
}
