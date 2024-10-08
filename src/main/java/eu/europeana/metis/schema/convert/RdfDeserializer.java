package eu.europeana.metis.schema.convert;

import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.EDM_HAS_VIEW;
import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.EDM_IS_SHOWN_AT;
import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.EDM_IS_SHOWN_BY;
import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.EDM_OBJECT;
import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.EDM_WEBRESOURCE;
import static eu.europeana.metis.schema.convert.model.RdfXpathConstants.SVCS_SERVICE;

import eu.europeana.metis.schema.convert.model.DeserializationOperation;
import eu.europeana.metis.schema.convert.model.RdfDeserializationException;
import eu.europeana.metis.schema.convert.model.RdfResourceEntry;
import eu.europeana.metis.schema.convert.model.ResourceInfo;
import eu.europeana.metis.schema.convert.model.UrlType;
import eu.europeana.metis.schema.convert.model.XPathExpressionWrapper;
import eu.europeana.metis.schema.jibx.RDF;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RdfDeserializer {

  private static final String UTF8 = StandardCharsets.UTF_8.name();
  private static final String OEMBED_NAMESPACE = "https://oembed.com/";
  private static final String XPATH_OEMBED_SERVICES =
      SVCS_SERVICE + "[dcterms:conformsTo/@rdf:resource = \"" + OEMBED_NAMESPACE + "\"]";
  private static final String XPATH_OEMBED_WEB_RESOURCES = EDM_WEBRESOURCE
      + "[svcs:has_service/@rdf:resource = " + XPATH_OEMBED_SERVICES + "/@rdf:about]";
  private static final String XPATH_IS_OEMBED_RESOURCE_CONDITION = "[. = "
      + XPATH_OEMBED_WEB_RESOURCES + "/@rdf:about]";
  private static final String OEMBED_XPATH_CONDITION_IS_SHOWN_BY =
      EDM_IS_SHOWN_BY + XPATH_IS_OEMBED_RESOURCE_CONDITION;
  private static final String OEMBED_XPATH_CONDITION_HAS_VIEW =
      EDM_HAS_VIEW + XPATH_IS_OEMBED_RESOURCE_CONDITION;

  private final XPathExpressionWrapper getObjectExpression = new XPathExpressionWrapper(xPath -> xPath.compile(EDM_OBJECT));
  private final XPathExpressionWrapper getHasViewExpression = new XPathExpressionWrapper(xPath -> xPath.compile(EDM_HAS_VIEW));
  private final XPathExpressionWrapper getIsShownAtExpression = new XPathExpressionWrapper(
      xPath -> xPath.compile(EDM_IS_SHOWN_AT));
  private final XPathExpressionWrapper getIsShownByExpression = new XPathExpressionWrapper(
      xPath -> xPath.compile(EDM_IS_SHOWN_BY));
  private final XPathExpressionWrapper getOEmbedExpression = new XPathExpressionWrapper(
      xPath -> xPath.compile(OEMBED_XPATH_CONDITION_HAS_VIEW + " | " + OEMBED_XPATH_CONDITION_IS_SHOWN_BY));

  private final IBindingFactory rdfBindingFactory;

  /**
   * Default constructor
   */
  public RdfDeserializer() {
    this(RDF.class);
  }

  /**
   * Constructor supplying class type for the binding factory.
   * <p>At the current state this is used for assisting testing</p>
   *
   * @param classType the class object type
   * @param <T> the class type
   */
  <T> RdfDeserializer(Class<T> classType) {
    try {
      rdfBindingFactory = BindingDirectory.getFactory(classType);
    } catch (JiBXException e) {
      throw new IllegalStateException("No binding factory available.", e);
    }
  }

  /**
   * Convert a UTF-8 encoded XML to {@link RDF}
   *
   * @param xml the xml string
   * @return the RDF object
   * @throws RdfDeserializationException if during unmarshalling there is a failure
   */
  public RDF deserialize(String xml) throws RdfDeserializationException {
    try (final InputStream inputStream = new ByteArrayInputStream(
        xml.getBytes(StandardCharsets.UTF_8))) {
      return deserialize(inputStream);
    } catch (IOException e) {
      throw new RdfDeserializationException("Unexpected issue with byte stream.", e);
    }
  }

  /**
   * Convert a UTF-8 encoded XML to {@link RDF}
   *
   * @param inputStream The xml. The stream is not closed.
   * @return the RDF object
   * @throws RdfDeserializationException if during unmarshalling there is a failure
   */
  public RDF deserialize(InputStream inputStream) throws RdfDeserializationException {
    try {
      final IUnmarshallingContext context = rdfBindingFactory.createUnmarshallingContext();
      return (RDF) context.unmarshalDocument(inputStream, UTF8);
    } catch (JiBXException e) {
      throw new RdfDeserializationException(
          "Something went wrong with converting to or from the RDF format.", e);
    }
  }

  public Document deserializeToDocument(InputStream inputStream) throws RdfDeserializationException {

    // Parse document to schema-agnostic XML document (but make parsing namespace-aware).
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(inputStream);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      throw new RdfDeserializationException("Problem with deserializing record to XML document.", e);
    }
  }

  public RdfResourceEntry getMainThumbnailResource(byte[] input) throws RdfDeserializationException {
    return performDeserialization(input, this::getMainThumbnailResource);
  }

  public RdfResourceEntry getMainThumbnailResource(InputStream inputStream)
      throws RdfDeserializationException {
    return getMainThumbnailResource(deserializeToDocument(inputStream)).orElse(null);
  }

  public Optional<RdfResourceEntry> getMainThumbnailResource(Document document)
      throws RdfDeserializationException {

    // Get the entries of the required types.
    final Map<String, ResourceInfo> resourceEntries = getResourceEntries(document,
        Collections.singleton(UrlType.URL_TYPE_FOR_MAIN_THUMBNAIL_RESOURCE));

    // If there is not exactly one, we return an empty optional.
    if (resourceEntries.size() != 1) {
      return Optional.empty();
    }

    // So there is exactly one. Convert and return.
    return Optional.of(convertToResourceEntries(resourceEntries).get(0));
  }

  public List<RdfResourceEntry> convertToResourceEntries(
      Map<String, ResourceInfo> urlWithTypes) {
    return urlWithTypes.entrySet().stream().map(RdfDeserializer::convertToResourceEntry)
                       .toList();
  }

  private static RdfResourceEntry convertToResourceEntry(Map.Entry<String, ResourceInfo> entry) {
    return new RdfResourceEntry(entry.getKey(), entry.getValue().urlTypes(),
        entry.getValue().configuredForOembed());
  }

  /**
   * Gets resource entries.
   *
   * @param document the document
   * @param allowedUrlTypes the allowed url types
   * @return the resource entries
   * @throws RdfDeserializationException the rdf deserialization exception
   */
  public Map<String, ResourceInfo> getResourceEntries(Document document,
      Set<UrlType> allowedUrlTypes) throws RdfDeserializationException {

    // Get the resources and their types.
    final Map<String, Set<UrlType>> urls = new HashMap<>();
    for (UrlType type : allowedUrlTypes) {
      final Set<String> urlsForType = getUrls(document, type);
      for (String url : urlsForType) {
        urls.computeIfAbsent(url, k -> new HashSet<>()).add(type);
      }
    }

    // For each resource, check whether they are configured for oEmbed.
    final Map<String, ResourceInfo> result = HashMap.newHashMap(urls.size());
    final Set<String> oEmbedUrls = getOEmbedUrls(document);
    for (Entry<String, Set<UrlType>> entry : urls.entrySet()) {
      boolean isConfiguredForOembed = oEmbedUrls.contains(entry.getKey());
      result.put(entry.getKey(), new ResourceInfo(entry.getValue(), isConfiguredForOembed));
    }

    // Done
    return result;
  }

  private Set<String> getUrls(Document document, UrlType type) throws RdfDeserializationException {

    // Determine the right expression to apply.
    final XPathExpressionWrapper expression =
        switch (type) {
          case OBJECT -> getObjectExpression;
          case HAS_VIEW -> getHasViewExpression;
          case IS_SHOWN_AT -> getIsShownAtExpression;
          case IS_SHOWN_BY -> getIsShownByExpression;
        };

    // Evaluate the expression and convert the node list to a set of attribute values.
    final NodeList nodes = expression.evaluate(document);
    return IntStream.range(0, nodes.getLength()).mapToObj(nodes::item).map(Node::getNodeValue)
                    .collect(Collectors.toSet());
  }

  private Set<String> getOEmbedUrls(Document document) throws RdfDeserializationException {
    final NodeList oEmbedNodes = getOEmbedExpression.evaluate(document);
    return IntStream.range(0, oEmbedNodes.getLength())
                    .mapToObj(oEmbedNodes::item)
                    .map(Node::getNodeValue)
                    .collect(Collectors.toSet());
  }

  public <R> R performDeserialization(byte[] input, DeserializationOperation<R> operation)
      throws RdfDeserializationException {
    try (InputStream inputStream = new ByteArrayInputStream(input)) {
      return operation.performDeserialization(inputStream);
    } catch (IOException e) {
      throw new RdfDeserializationException("Problem with reading byte array - Shouldn't happen.", e);
    }
  }

}
