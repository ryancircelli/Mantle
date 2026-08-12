package slimeknights.mantle.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.test.BaseMcTest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards 1.21's new argument type check on {@code TranslatableContents} across Mantle's command messages.
 * <p>
 * 1.20 accepted any {@code Object} as a translation argument and rendered it through {@code String.valueOf} at display
 * time. 1.21 validates in the constructor: an argument must be a {@code Component}, {@code Number}, {@code Boolean} or
 * {@code String}, and anything else — a {@code ResourceLocation}, a {@code Path}, a {@code ResourceKey} — throws
 * {@link IllegalArgumentException}. Mantle's command messages are full of resource locations, so this bit in two
 * places at once: in a class initializer it is a hard crash at command registration, and inside an exception factory
 * it replaces the error the user should have seen with an unrelated one.
 * <p>
 * Both shapes are covered below. The class-initializer sweep is the one that matters most, because that failure took
 * the whole dedicated server down before it reached "Done".
 */
class CommandMessageTest extends BaseMcTest {
  /**
   * Every class in {@code slimeknights.mantle.command} outside {@code command.client}, which cannot be initialized off
   * a client. Loading a class runs its static initializers, so a message built from a bad argument at class scope
   * fails here exactly as it failed the server.
   */
  @Test
  void everyCommandClassInitializes() {
    List<String> classes = List.of(
      "slimeknights.mantle.command.DumpAllTagsCommand",
      "slimeknights.mantle.command.DumpLootModifiers",
      "slimeknights.mantle.command.DumpTagCommand",
      "slimeknights.mantle.command.GeneratePackHelper",
      "slimeknights.mantle.command.HungerCommand",
      "slimeknights.mantle.command.MantleCommand",
      "slimeknights.mantle.command.RegistryArgument",
      "slimeknights.mantle.command.RemoveDataCommand",
      "slimeknights.mantle.command.RemoveRecipesCommand",
      "slimeknights.mantle.command.SourcesCommand",
      "slimeknights.mantle.command.TagPreferenceCommand",
      "slimeknights.mantle.command.TagsForCommand",
      "slimeknights.mantle.command.ViewTagCommand",
      "slimeknights.mantle.command.argument.ResourceOrTagKeyArgument",
      "slimeknights.mantle.command.argument.TagSourceArgument",
      "slimeknights.mantle.command.tags.ModifyTagCommand");
    for (String name : classes) {
      assertThatCode(() -> Class.forName(name, true, CommandMessageTest.class.getClassLoader()))
        .as("%s must initialize; a translation argument that is not a Component, Number, Boolean or String throws here", name)
        .doesNotThrowAnyException();
    }
  }

  private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mantle", "test");

  /** Asserts an exception factory produced a real message rather than throwing over its own arguments */
  private static void assertMessage(CommandSyntaxException exception) {
    assertThat(exception.getRawMessage()).isInstanceOf(Component.class);
    assertThat(((Component) exception.getRawMessage()).getString()).isNotEmpty();
  }

  @Test
  void dynamicExceptionsAcceptResourceLocations() {
    assertMessage(ViewTagCommand.TAG_NOT_FOUND.create(ID, ID));
    assertMessage(TagsForCommand.VALUE_NOT_FOUND.create(ID, ID));
    assertMessage(GeneratePackHelper.FAILED_SAVE.create(ID));
  }

  /** {@code FAILED_SAVE} is also thrown with a {@code ResourceKey} and with a {@code Path}-derived id, not just a location */
  @Test
  void failedSaveAcceptsAnyId() {
    assertMessage(GeneratePackHelper.FAILED_SAVE.create(ResourceKey.create(Registries.RECIPE_TYPE, ID)));
    assertMessage(GeneratePackHelper.FAILED_SAVE.create(Path.of("pack", "data")));
  }

  /** The one message that was a hard crash: it is built at class scope, so it is only reachable through the field */
  @Test
  void lootModifierReadErrorIsBuildable() {
    assertThat(DumpLootModifiers.ERROR_READING_LOOT_MODIFIERS.create().getRawMessage())
      .isInstanceOf(Component.class);
  }
}
