package slimeknights.mantle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.network.packet.IPacket;
import slimeknights.mantle.network.packet.PacketContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests that a channel's packet identities are unique and ordered, without building a channel */
class PacketRegistryTest {
  private static final ResourceLocation CHANNEL = new ResourceLocation("mantle", "network");

  /** Packet with no state, only ever used to fill a registration */
  private static class EmptyPacket implements IPacket {
    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(PacketContext context) {}
  }

  /** Second packet class, to check the class based checks */
  private static class OtherPacket extends EmptyPacket {}

  private static PacketRegistration<?> registration(ResourceLocation id, Class<? extends EmptyPacket> clazz) {
    return registrationOf(id, clazz);
  }

  private static <P extends EmptyPacket> PacketRegistration<P> registrationOf(ResourceLocation id, Class<P> clazz) {
    return new PacketRegistration<>(id, clazz, IPacket::encode, buffer -> null, IPacket::handle, PacketDirection.CLIENTBOUND);
  }


  /* Uniqueness */

  @Test
  void register_duplicateIdFails() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(new ResourceLocation("mantle", "packet"), EmptyPacket.class));
    assertThatThrownBy(() -> registry.register(registration(new ResourceLocation("mantle", "packet"), OtherPacket.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Duplicate packet ID mantle:packet")
      .hasMessageContaining(EmptyPacket.class.getName());
  }

  @Test
  void register_duplicateClassFails() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(new ResourceLocation("mantle", "first"), EmptyPacket.class));
    assertThatThrownBy(() -> registry.register(registration(new ResourceLocation("mantle", "second"), EmptyPacket.class)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Duplicate packet class " + EmptyPacket.class.getName())
      .hasMessageContaining("mantle:first");
  }

  @Test
  void register_differentIdsAndClassesPass() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(new ResourceLocation("mantle", "first"), EmptyPacket.class));
    registry.register(registration(new ResourceLocation("mantle", "second"), OtherPacket.class));
    assertThat(registry.size()).isEqualTo(2);
  }


  /* Ordering, which is what actually reaches the wire */

  @Test
  void register_assignsIndexesInOrder() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    assertThat(registry.register(registration(new ResourceLocation("mantle", "first"), EmptyPacket.class))).isEqualTo(0);
    assertThat(registry.register(registration(new ResourceLocation("mantle", "second"), OtherPacket.class))).isEqualTo(1);
    assertThat(registry.size()).isEqualTo(2);
  }

  @Test
  void ids_keepRegistrationOrder() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    registry.register(registration(new ResourceLocation("mantle", "zebra"), EmptyPacket.class));
    registry.register(registration(new ResourceLocation("mantle", "aardvark"), OtherPacket.class));
    assertThat(registry.ids()).containsExactly(new ResourceLocation("mantle", "zebra"), new ResourceLocation("mantle", "aardvark"));
  }


  /* Lookup */

  @Test
  void get_findsTheRegistration() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    ResourceLocation id = new ResourceLocation("mantle", "packet");
    PacketRegistration<EmptyPacket> registration = registrationOf(id, EmptyPacket.class);
    registry.register(registration);
    assertThat(registry.get(id)).isSameAs(registration);
    assertThat(registry.contains(id)).isTrue();
  }

  @Test
  void get_missingIsNull() {
    PacketRegistry registry = new PacketRegistry(CHANNEL);
    assertThat(registry.get(new ResourceLocation("mantle", "packet"))).isNull();
    assertThat(registry.contains(new ResourceLocation("mantle", "packet"))).isFalse();
  }


  /* Derived identifiers, the fallback for a channel that has not been migrated */

  @Test
  void deriveId_usesTheChannelNamespace() {
    PacketRegistry registry = new PacketRegistry(new ResourceLocation("othermod", "network"));
    assertThat(registry.deriveId(EmptyPacket.class)).isEqualTo(new ResourceLocation("othermod", "empty"));
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
      public void encode(FriendlyByteBuf buffer) {}

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
