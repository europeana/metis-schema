package eu.europeana.metis.schema.convert;

import eu.europeana.metis.schema.convert.model.RdfSerializationException;
import eu.europeana.metis.schema.convert.model.RdfXmlElementMetadata;
import eu.europeana.metis.schema.jibx.RDF;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

public class RdfSerializer {

  @SuppressWarnings("java:S5852") //This regex is safe, and it's only meant for internal use without use input
  private static final Pattern complexTypePattern = Pattern.compile("^\\{(.*)}:(.*)$");
  private static final String UTF8 = StandardCharsets.UTF_8.name();
  private static final int INDENTATION_SPACE = 2;
  private final IBindingFactory rdfBindingFactory;
  private final Map<String, RdfXmlElementMetadata> rdfXmlElementMetadataMap;

  /**
   * Default constructor
   */
  public RdfSerializer() {
    this(RDF.class);
  }

  /**
   * Constructor supplying class type for the binding factory.
   * <p>At the current state this is used for assisting testing</p>
   *
   * @param classType the class object type
   * @param <T> the class type
   */
  <T> RdfSerializer(Class<T> classType) {
    try {
      rdfBindingFactory = BindingDirectory.getFactory(classType);
      rdfXmlElementMetadataMap = initializeRdfXmlElementMetadataMap();
    } catch (JiBXException e) {
      throw new IllegalStateException("No binding factory available.", e);
    }
  }

  /**
   * Convert an {@link RDF} to a UTF-8 encoded XML
   *
   * @param rdf The RDF object to convert
   * @return An XML string representation of the RDF object
   * @throws RdfSerializationException if during marshalling there is a failure
   */
  public String serialize(RDF rdf) throws RdfSerializationException {
    try {
      return new String(convertRdfToBytes(rdf), UTF8);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unexpected exception - should not occur.", e);
    }
  }

  /**
   * Convert an {@link RDF} to a UTF-8 encoded XML
   *
   * @param rdf The RDF object to convert
   * @return An XML string representation of the RDF object
   * @throws RdfSerializationException if during marshalling there is a failure
   */
  public byte[] convertRdfToBytes(RDF rdf) throws RdfSerializationException {
    try {
      IMarshallingContext context = rdfBindingFactory.createMarshallingContext();
      context.setIndent(INDENTATION_SPACE);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      context.marshalDocument(rdf, UTF8, null, out);
      return out.toByteArray();
    } catch (JiBXException e) {
      throw new RdfSerializationException(
          "Something went wrong with converting to or from the RDF format.", e);
    }
  }

  /**
   * Get the xml representation of a class that will contain the namespace prefix and the element name. E.g. dc:subject
   * <p>This class uses the internal static map that should be generated with regards to the RDF jibx classes</p>
   *
   * @param objectClass the jibx object class to search for
   * @return the xml representation
   */
  public String getQualifiedElementNameForClass(Class<?> objectClass) {
    final RdfXmlElementMetadata rdfXmlElementMetadata = rdfXmlElementMetadataMap.get(objectClass.getCanonicalName());
    Objects.requireNonNull(rdfXmlElementMetadata,
        String.format("Element metadata not found for class: %s", objectClass.getCanonicalName()));
    return String.format("%s:%s", rdfXmlElementMetadata.prefix(), rdfXmlElementMetadata.name());
  }

  /**
   * Collect all information that we can get for jibx classes from the {@link IBindingFactory}.
   */
  private Map<String, RdfXmlElementMetadata> initializeRdfXmlElementMetadataMap() {
    Map<String, RdfXmlElementMetadata> elementMetadataMap = new HashMap<>();
    for (int i = 0; i < rdfBindingFactory.getMappedClasses().length; i++) {
      final String canonicalName;
      final String elementNamespace;
      final String elementName;
      final Matcher matcher = complexTypePattern.matcher(rdfBindingFactory.getMappedClasses()[i]);
      if (matcher.matches()) {
        //Complex type search
        elementNamespace = matcher.group(1);
        elementName = matcher.group(2);
        final Pattern canonicalClassNamePattern = Pattern.compile(String.format("^(.*)\\.(%s)$", elementName));
        canonicalName = Arrays.stream(rdfBindingFactory.getAbstractMappings()).flatMap(Arrays::stream)
                              .filter(Objects::nonNull)
                              .filter(input -> canonicalClassNamePattern.matcher(input).matches())
                              .findFirst().orElse(null);
      } else {
        //Simple type search
        elementNamespace = rdfBindingFactory.getElementNamespaces()[i];
        elementName = rdfBindingFactory.getElementNames()[i];
        canonicalName = rdfBindingFactory.getMappedClasses()[i];
      }
      checkAndStoreMetadataInMap(elementMetadataMap, canonicalName, elementNamespace, elementName);
    }
    return elementMetadataMap;
  }

  private void checkAndStoreMetadataInMap(final Map<String, RdfXmlElementMetadata> rdfXmlElementMetadataMap,
      String canonicalName, String elementNamespace, String elementName) {
    //Store only if we could find the canonical name properly
    if (canonicalName != null) {
      final int namespaceIndex = IntStream.range(0, rdfBindingFactory.getNamespaces().length)
                                          .filter(j -> rdfBindingFactory.getNamespaces()[j].equals(elementNamespace))
                                          .findFirst().orElseThrow();
      final String prefix = rdfBindingFactory.getPrefixes()[namespaceIndex];
      final RdfXmlElementMetadata rdfXmlElementMetadata = new RdfXmlElementMetadata(canonicalName, prefix, elementNamespace,
          elementName);
      rdfXmlElementMetadataMap.put(rdfXmlElementMetadata.canonicalClassName(), rdfXmlElementMetadata);
    }
  }

}
