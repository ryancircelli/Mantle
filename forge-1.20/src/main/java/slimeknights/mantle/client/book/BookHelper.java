package slimeknights.mantle.client.book;

import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.item.data.DataEditor;
import slimeknights.mantle.item.data.DataKey;
import slimeknights.mantle.item.data.DataView;

import javax.annotation.Nullable;
import java.util.function.Function;

public class BookHelper {

  public static final String BOOK_COMPOUND = "mantle";
  public static final String BOOK_DATA_COMPOUND = "book";

  public static final String NBT_CURRENT_PAGE = "current_page";

  /** Loadable for the {@value #BOOK_DATA_COMPOUND} compound nested inside {@value #BOOK_COMPOUND}, holding just the saved page */
  private static final RecordLoadable<String> BOOK_DATA = RecordLoadable.create(
    StringLoadable.DEFAULT.<String>requiredField(NBT_CURRENT_PAGE, Function.identity()),
    Function.identity());

  /**
   * Key for a book's saved page, stored under the bare name {@value #BOOK_COMPOUND} rather than a namespaced one,
   * and nested two deep ({@value #BOOK_COMPOUND} &rarr; {@value #BOOK_DATA_COMPOUND} &rarr; {@value #NBT_CURRENT_PAGE})
   * to match the format written before this key existed.
   */
  public static final DataKey<String> CURRENT_PAGE = DataKey.ofLegacyName(BOOK_COMPOUND, RecordLoadable.create(
    BOOK_DATA.<String>requiredField(BOOK_DATA_COMPOUND, Function.identity()),
    Function.identity()));

  /**
   * Returns the current saved page on the book
   * Returns an empty string is one is not found
   *
   * @param item The book to check for a saved page on
   * @return The current saved page
   */
  public static String getCurrentSavedPage(@Nullable ItemStack item) {
    if (item == null || item.isEmpty()) {
      return "";
    }
    return DataView.of(item).getOrDefault(CURRENT_PAGE, "");
  }

  /**
   * Saves the current open page to the given book ItemStack.
   *
   * @param stack       the current book stack
   * @param currentPage the current open page
   */
  public static void writeSavedPageToBook(ItemStack stack, String currentPage) {
    DataEditor.edit().set(CURRENT_PAGE, currentPage).apply(stack);
  }
}
