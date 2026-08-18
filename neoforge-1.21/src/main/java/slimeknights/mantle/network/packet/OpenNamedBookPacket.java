package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.command.client.BookCommand;

@AllArgsConstructor
public class OpenNamedBookPacket implements IPacket.Threadsafe {
  /** Identifier of this packet on Mantle's channel */
  public static final ResourceLocation ID = Mantle.getResource("open_named_book");

  private final ResourceLocation book;

  public OpenNamedBookPacket(RegistryFriendlyByteBuf buffer) {
    this.book = buffer.readResourceLocation();
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer) {
    buffer.writeResourceLocation(book);
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      bookData.openGui(Component.literal("Book"), "", null, null);
    } else {
      ClientOnly.errorStatus(book);
    }
  }

  /**
   * Holds the client-only reference so this class loads on a dedicated server. The message itself belongs to
   * {@link BookCommand}, which owns the {@code command.mantle.book_test.not_found} key and is the other sender of it.
   */
  static class ClientOnly {
    static void errorStatus(ResourceLocation book) {
      BookCommand.bookNotFound(book);
    }
  }
}
