package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import slimeknights.mantle.data.loadable.field.ConstantField;
import slimeknights.mantle.data.loadable.field.RecordField;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simple helpers to create exceptions.
 * <p>
 * Loadables report all failures by throwing an unchecked exception rather than returning a result object, with the
 * exception type chosen by the destination of the data instead of its format: {@link #JSON_SYNTAX_ERROR} while reading
 * a datapack, {@link #DECODER_EXCEPTION} and {@link #ENCODER_EXCEPTION} while reading and writing the network, and
 * {@link #RUNTIME} while writing a datapack. Notably that means the ops based methods on {@link Loadable} throw the
 * same exceptions as their gson counterparts no matter which {@link com.mojang.serialization.DynamicOps} they were
 * given; the error messages are keyed by field name, which is meaningful in any format.
 * Use {@link #catching(Supplier)} to move a loadable into the {@link DataResult} world.
 */
public interface ErrorFactory extends Consumer<String> {
  /** Error factory for a json syntax error during parsing */
  ErrorFactory JSON_SYNTAX_ERROR = JsonSyntaxException::new;
  /** Error factory for a decoder exception */
  ErrorFactory DECODER_EXCEPTION = DecoderException::new;
  /** Error factory for a decoder exception */
  ErrorFactory ENCODER_EXCEPTION = EncoderException::new;
  /** Error factory for a json during writing JSON */
  ErrorFactory RUNTIME = new ErrorFactory() {
    @Override
    public RuntimeException create(String error) {
      return new RuntimeException(error);
    }

    @Override
    public RuntimeException create(RuntimeException base) {
      return base;
    }
  };
  /** Field for constructors wishing to possibly throw */
  RecordField<ErrorFactory,Object> FIELD = new ConstantField<>(JSON_SYNTAX_ERROR, DECODER_EXCEPTION);

  /** Throws an exception from the given error */
  @Override
  default void accept(String error) {
    throw create(error);
  }

  /** Creates an exception with a string error */
  RuntimeException create(String error);

  /** Creates an exception wrapping the given exception message */
  default RuntimeException create(RuntimeException base) {
    return create(base.getMessage());
  }

  /**
   * Runs the given supplier, converting any exception it throws into a {@link DataResult} error.
   * Notably useful to implement a {@link com.mojang.serialization.Codec} in terms of a loadable.
   * @param supplier  Supplier running the loadable logic
   * @param <T>  Type of the result
   * @return  Result of the supplier, or an error result containing its message.
   * @apiNote  Catches every runtime exception as loadables do not share a single exception type; while parsing errors
   *           are typically a {@link JsonSyntaxException}, implementations are free to throw anything unchecked.
   */
  static <T> DataResult<T> catching(Supplier<T> supplier) {
    return catching(supplier, e -> {});
  }

  /**
   * Same as {@link #catching(Supplier)} but hands the exception to the given consumer before discarding it.
   * @param supplier  Supplier running the loadable logic
   * @param onError   Consumer receiving the exception, typically to log it as a {@link DataResult} keeps just the message
   * @param <T>  Type of the result
   * @return  Result of the supplier, or an error result containing its message.
   */
  static <T> DataResult<T> catching(Supplier<T> supplier, Consumer<RuntimeException> onError) {
    try {
      return DataResult.success(supplier.get());
    } catch (RuntimeException e) {
      onError.accept(e);
      // NPEs and the like have no message, so fall back to the type name to avoid a null error
      String message = e.getMessage();
      String error = message != null ? message : e.toString();
      return DataResult.error(() -> error);
    }
  }
}
