package slimeknights.mantle.network;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Set of packets registered to a channel, keyed by identity.
 * <p>
 * A channel's packets are indexed on the wire by the order they were registered, which means an ordering difference
 * between two builds is only discovered when a decoder reads the wrong packet at login. Giving each packet a
 * {@link ResourceLocation} makes that mistake visible earlier: a duplicate is a hard failure while the channel is being
 * built, and a channel can be listed and compared without connecting to anything.
 * <p>
 * <b>The identifier is not written to the wire.</b> The index this class hands back is still what a message carries, so
 * adding identifiers to an existing channel does not change a single byte, and reordering registrations is still a
 * protocol change.
 */
public class PacketRegistry {
  /** Suffix trimmed from a class name when deriving an identifier, as every packet class carries it */
  private static final String PACKET_SUFFIX = "Packet";

  /** Channel these packets belong to, used for derived identifiers and error messages */
  private final ResourceLocation channel;
  /** Registrations in the order they were added, which is the order of their wire indexes */
  private final Map<ResourceLocation,PacketRegistration<?>> byId = new LinkedHashMap<>();
  /** Identifier of each registered class, as a channel can only encode a class one way */
  private final Map<Class<?>,ResourceLocation> byType = new HashMap<>();

  public PacketRegistry(ResourceLocation channel) {
    this.channel = channel;
  }

  /**
   * Adds a packet to this registry.
   * @param registration  Packet registration
   * @return  Index of this packet within the channel, which is what identifies it on the wire
   * @throws IllegalArgumentException  If the identifier or the class is already registered
   */
  public int register(PacketRegistration<?> registration) {
    ResourceLocation id = registration.id();
    PacketRegistration<?> conflict = byId.get(id);
    if (conflict != null) {
      throw new IllegalArgumentException("Duplicate packet ID " + id + " on channel " + channel + ", already registered to " + conflict.type().getName());
    }
    ResourceLocation existing = byType.get(registration.type());
    if (existing != null) {
      throw new IllegalArgumentException("Duplicate packet class " + registration.type().getName() + " on channel " + channel + ", already registered as " + existing);
    }
    int index = byId.size();
    byId.put(id, registration);
    byType.put(registration.type(), id);
    return index;
  }

  /**
   * Gets the packet registered under the given identifier.
   * @param id  Packet identifier
   * @return  Registration, or null if no packet uses that identifier
   */
  @Nullable
  public PacketRegistration<?> get(ResourceLocation id) {
    return byId.get(id);
  }

  /** Checks if the given identifier is registered */
  public boolean contains(ResourceLocation id) {
    return byId.containsKey(id);
  }

  /** Gets the number of registered packets */
  public int size() {
    return byId.size();
  }

  /** Gets every registered identifier, in registration order */
  public Collection<ResourceLocation> ids() {
    return Collections.unmodifiableCollection(byId.keySet());
  }

  /** Gets every registration, in registration order */
  public Collection<PacketRegistration<?>> registrations() {
    return Collections.unmodifiableCollection(byId.values());
  }


  /* Derived identifiers */

  /**
   * Derives an identifier for a packet registered without one, so a channel that has not been migrated is still
   * checked for duplicates. The identifier is in the channel's namespace, meaning two mods cannot collide.
   * @param clazz  Packet class
   * @return  Identifier for the class
   */
  public ResourceLocation deriveId(Class<?> clazz) {
    return new ResourceLocation(channel.getNamespace(), defaultPath(clazz));
  }

  /**
   * Converts a packet class name into an identifier path: the trailing {@code Packet} is dropped and the remaining
   * camel case is converted to snake case, so {@code OpenLecternBookPacket} becomes {@code open_lectern_book}.
   * <p>
   * Note this reads the class's own name, so a packet extending another packet without being registered under an
   * explicit identifier derives its parent's path only if it shares the parent's name; distinct subclasses get distinct
   * paths, and a subclass registered under an inherited {@code ID} constant instead trips the duplicate check.
   * @param clazz  Packet class
   * @return  Identifier path
   */
  public static String defaultPath(Class<?> clazz) {
    String name = clazz.getSimpleName();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Cannot derive a packet ID for anonymous class " + clazz.getName());
    }
    if (name.length() > PACKET_SUFFIX.length() && name.endsWith(PACKET_SUFFIX)) {
      name = name.substring(0, name.length() - PACKET_SUFFIX.length());
    }
    int length = name.length();
    StringBuilder builder = new StringBuilder(length + 4);
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        // start a new word at the first capital of a run, and at the last capital of a run followed by a lowercase
        if (i > 0 && (!Character.isUpperCase(name.charAt(i - 1)) || (i + 1 < length && Character.isLowerCase(name.charAt(i + 1))))) {
          builder.append('_');
        }
        builder.append(Character.toLowerCase(c));
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}
