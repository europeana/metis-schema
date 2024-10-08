package eu.europeana.metis.schema.convert.model;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XPathExpressionWrapper extends
    AbstractThreadSafeWrapper<XPathExpression, RdfDeserializationException> {

  /**
   * Instantiates a new X path expression wrapper.
   *
   * @param expressionCreator the expression creator
   */
  public XPathExpressionWrapper(
      ThrowingFunction<XPath, XPathExpression, XPathExpressionException> expressionCreator) {
    super(() -> {
      final XPathFactory factory;
      synchronized (XPathFactory.class) {
        factory = XPathFactory.newInstance();
      }
      final XPath xPath = factory.newXPath();
      xPath.setNamespaceContext(new RdfNamespaceContext());
      try {
        return expressionCreator.apply(xPath);
      } catch (XPathExpressionException e) {
        throw new RdfDeserializationException("Could not initialize xpath expression.", e);
      }
    });
  }

  /**
   * Evaluate node list.
   *
   * @param document the document
   * @return the node list
   * @throws RdfDeserializationException the rdf deserialization exception
   */
  public NodeList evaluate(Document document) throws RdfDeserializationException {
    return process(compiledExpression -> {
      try {
        return (NodeList) compiledExpression.evaluate(document, XPathConstants.NODESET);
      } catch (XPathExpressionException e) {
        throw new RdfDeserializationException("Problem with deserializing RDF.", e);
      }
    });
  }
}
