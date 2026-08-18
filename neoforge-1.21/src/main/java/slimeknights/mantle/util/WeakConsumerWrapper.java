package slimeknights.mantle.util;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Implementation of {@link Consumer} that weakly references a parent object.
 * Designed for use as a listener that must not keep the listening object (typically a block entity) alive purely by
 * being registered elsewhere, so the listener can still be garbage collected once nothing else references it.
 * <p>
 * Forge's {@code NonNullConsumer}/{@code LazyOptional#addListener} pairing this class used to target does not exist
 * under NeoForge's capability system - capabilities resolve synchronously now, and the closest analog,
 * {@link net.neoforged.neoforge.capabilities.BlockCapabilityCache}, takes a plain {@link Runnable} invalidation
 * listener instead. This class keeps its weak-referencing shape as a plain {@link Consumer}, so it can still back
 * that kind of listener without holding a strong reference to its parent.
 * @param <TE>  Parent object type, typically a TE
 * @param <C>   Consumer value
 */
public class WeakConsumerWrapper<TE,C> implements Consumer<C> {
  private final WeakReference<TE> te;
  private final NonnullBiConsumer<TE,C> consumer;

  /**
   * Creates a new weak consumer wrapper
   * @param te        Weak reference, typically to a TE
   * @param consumer  Consumer using the TE and the consumed value. Should not use a lambda reference to an object that may need to be garbage collected
   */
  public WeakConsumerWrapper(TE te, NonnullBiConsumer<TE,C> consumer) {
    this.te = new WeakReference<>(te);
    this.consumer = consumer;
  }

  @Override
  public void accept(C c) {
    TE te = this.te.get();
    if (te != null) {
      consumer.accept(te, c);
    }
  }
}
