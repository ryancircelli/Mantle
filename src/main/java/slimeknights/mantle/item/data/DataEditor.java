package slimeknights.mantle.item.data;

import net.minecraft.world.item.ItemStack;

/**
 * Builder collecting changes to the values an item stack stores under {@link DataKey}s, which {@link #apply(ItemStack)}
 * then commits in one step.
 * <p>
 * Nothing an editor is told is visible on any stack until {@code apply}, so a caller can build up a change, decide
 * against it and simply drop the editor. Values are written as they are set rather than at the end, which means a value
 * the loadable cannot write fails at the call which supplied it and {@code apply} itself always succeeds in full.
 * <p>
 * An editor is also a {@link DataView} reading its own pending changes, so a key set here reads back here. An editor
 * from {@link #edit(ItemStack)} additionally reads through to that stack for keys it has not touched, which is what
 * makes a read-modify-write of one key a single expression:
 * <pre>{@code
 * DataEditor editor = DataEditor.edit(stack);
 * editor.set(USES, editor.getOrDefault(USES, 0) + 1).apply(stack);
 * }</pre>
 * The stack an editor reads is not the stack it writes: {@code apply} always names its target, so stamping the data
 * read from one stack onto another is just applying to the other stack. An editor may be applied any number of times
 * and the stacks do not end up sharing anything.
 * <p>
 * An editor is a local builder and is not safe to use from more than one thread.
 *
 * @see DataKey
 * @see DataView
 */
@SuppressWarnings("unused")  // API
public interface DataEditor extends DataView {
  /**
   * Sets the value of the passed key, replacing anything set or removed here earlier.
   * @param key    Key to write
   * @param value  Value to write
   * @param <T>    Type of the value
   * @return  This editor, for chaining
   * @throws RuntimeException  If the value cannot be written, per the key's loadable. See {@link slimeknights.mantle.data.loadable.ErrorFactory}.
   */
  <T> DataEditor set(DataKey<T> key, T value);

  /**
   * Removes the value of the passed key, discarding anything set or removed here earlier. The key reads as absent from
   * this editor afterwards even if the stack it reads through to has a value.
   * @param key  Key to clear
   * @return  This editor, for chaining
   */
  DataEditor remove(DataKey<?> key);

  /**
   * Commits every change collected here onto the passed stack.
   * <p>
   * Only the entries of the keys this editor was given are touched; everything else in the stack's tag, whether
   * vanilla's or another mod's, is left exactly as it was. A stack with no tag gains one only if there is a value to
   * store, and a tag left with nothing in it is dropped, so a stack which ends up with no data still stacks with a
   * plain one. An empty stack is never written to, as it is a shared instance.
   * @param stack  Stack to write to
   * @return  The passed stack, for chaining
   */
  ItemStack apply(ItemStack stack);


  /* Factories */

  /**
   * Creates an editor holding no starting data, which reads only what it is given. Use this to build a set of values
   * to stamp onto stacks, such as the contents of a creative tab or the result of a recipe.
   * @return  New editor
   */
  static DataEditor edit() {
    return new BufferedDataEditor(DataView.EMPTY);
  }

  /**
   * Creates an editor reading through to the passed stack for any key it has not been given, for changing data based
   * on what is already there. The stack is not written to until it is passed to {@link #apply(ItemStack)}.
   * @param stack  Stack to read from
   * @return  New editor
   */
  static DataEditor edit(ItemStack stack) {
    return new BufferedDataEditor(DataView.of(stack));
  }
}
