package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;

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
   * Holds the client-only reference so the class loads on a dedicated server. {@code command.client.BookCommand}
   * (this packet's original error-reporting target) is out of scope for the book/screen port and stays behind the
   * frontier, so this duplicates its four-line {@code bookNotFound} message rather than reaching across the boundary.
   */
  static class ClientOnly {
    private static final String BOOK_NOT_FOUND = "command.mantle.book_test.not_found";

    static void errorStatus(ResourceLocation book) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
        player.displayClientMessage(Component.translatable(BOOK_NOT_FOUND, book).withStyle(ChatFormatting.RED), false);
      }
    }
  }
}
