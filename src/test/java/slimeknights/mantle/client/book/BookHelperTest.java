package slimeknights.mantle.client.book;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the saved page to the exact nesting it used before {@link BookHelper#CURRENT_PAGE} existed
 * ({@value BookHelper#BOOK_COMPOUND} &rarr; {@value BookHelper#BOOK_DATA_COMPOUND} &rarr; {@value BookHelper#NBT_CURRENT_PAGE}),
 * so a book saved by an older Mantle still opens to the right page and a book saved by this class still opens on an
 * older Mantle.
 * <p>
 * That compound lives inside {@code minecraft:custom_data} rather than on the stack itself, so the assertions
 * reach it through {@link BaseMcTest}'s custom data helpers. The nesting they check is unchanged.
 */
class BookHelperTest extends BaseMcTest {
  /** Builds the tag layout {@link BookHelper} wrote before it had a key */
  private static CompoundTag legacyTag(String page) {
    CompoundTag root = new CompoundTag();
    CompoundTag mantle = new CompoundTag();
    CompoundTag book = new CompoundTag();
    book.putString("current_page", page);
    mantle.put("book", book);
    root.put("mantle", mantle);
    return root;
  }

  @Test
  void currentPage_isStoredUnderTheBareLegacyName() {
    // renaming this would strand the saved page in every book already saved
    assertThat(BookHelper.CURRENT_PAGE.getName()).isEqualTo("mantle");
  }

  @Test
  void getCurrentSavedPage_readsATagWrittenTheOldWay() {
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    setCustomData(stack, legacyTag("page_3"));
    assertThat(BookHelper.getCurrentSavedPage(stack)).isEqualTo("page_3");
  }

  @Test
  void getCurrentSavedPage_missingTagReadsAsEmpty() {
    assertThat(BookHelper.getCurrentSavedPage(new ItemStack(Items.WRITTEN_BOOK))).isEmpty();
  }

  @Test
  void getCurrentSavedPage_nullStackReadsAsEmpty() {
    assertThat(BookHelper.getCurrentSavedPage(null)).isEmpty();
  }

  @Test
  void getCurrentSavedPage_emptyStackReadsAsEmpty() {
    assertThat(BookHelper.getCurrentSavedPage(ItemStack.EMPTY)).isEmpty();
  }

  @Test
  void getCurrentSavedPage_missingNestedCompoundReadsAsEmpty() {
    // "mantle" present but without "book" inside, the shape the old getCompound chain silently defaulted on
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    CompoundTag root = new CompoundTag();
    root.put("mantle", new CompoundTag());
    setCustomData(stack, root);
    assertThat(BookHelper.getCurrentSavedPage(stack)).isEmpty();
  }

  @Test
  void writeSavedPageToBook_writesExactlyTheLegacyTagLayout() {
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    BookHelper.writeSavedPageToBook(stack, "page_5");

    assertThat(getCustomData(stack)).isEqualTo(legacyTag("page_5"));
  }

  @Test
  void writeSavedPageToBook_roundTripsThroughGetCurrentSavedPage() {
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    BookHelper.writeSavedPageToBook(stack, "page_7");
    assertThat(BookHelper.getCurrentSavedPage(stack)).isEqualTo("page_7");
  }

  @Test
  void writeSavedPageToBook_emptyPageIsStillWrittenAsAnEntry() {
    // an empty saved page is not the same as no saved page at all, unlike most keys the empty string is meaningful
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    BookHelper.writeSavedPageToBook(stack, "");
    assertThat(getCustomData(stack)).isEqualTo(legacyTag(""));
    assertThat(getCustomData(stack).getCompound("mantle").getCompound("book").get("current_page")).isEqualTo(StringTag.valueOf(""));
  }

  @Test
  void writeSavedPageToBook_keepsUnrelatedEntries() {
    ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
    getOrCreateCustomData(stack).putString("other_mod:their_key", "value");

    BookHelper.writeSavedPageToBook(stack, "page_1");
    assertThat(getCustomData(stack).getString("other_mod:their_key")).isEqualTo("value");
    assertThat(BookHelper.getCurrentSavedPage(stack)).isEqualTo("page_1");
  }
}
