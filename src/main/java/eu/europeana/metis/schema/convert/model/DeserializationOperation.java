package eu.europeana.metis.schema.convert.model;

import java.io.InputStream;

@FunctionalInterface
public interface DeserializationOperation<R> {
  /**
   * Perform deserialization r.
   *
   * @param inputStream the input stream
   * @return the r
   * @throws RdfDeserializationException the rdf deserialization exception
   */
  R performDeserialization(InputStream inputStream) throws RdfDeserializationException;
}
