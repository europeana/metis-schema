package eu.europeana.metis.schema.convert.model;

import java.io.Serial;

/**
 * This exception represents a problem that occurred during serialization of an RDF object.
 */
public class RdfSerializationException extends Exception {

  /** This class implements {@link java.io.Serializable}. **/
  @Serial private static final long serialVersionUID = 1031549407979593963L;

  /**
   * Constructor.
   * 
   * @param message The exception message.
   * @param cause The cause.
   */
  public RdfSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
