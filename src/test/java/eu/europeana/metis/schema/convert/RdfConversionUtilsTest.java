package eu.europeana.metis.schema.convert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import eu.europeana.metis.schema.jibx.AgentType;
import eu.europeana.metis.schema.jibx.Alternative;
import eu.europeana.metis.schema.jibx.Concept;
import eu.europeana.metis.schema.jibx.Coverage;
import eu.europeana.metis.schema.jibx.Creator1;
import eu.europeana.metis.schema.jibx.CurrentLocation;
import eu.europeana.metis.schema.jibx.Description;
import eu.europeana.metis.schema.jibx.EquivalentPID;
import eu.europeana.metis.schema.jibx.Format;
import eu.europeana.metis.schema.jibx.HasPart;
import eu.europeana.metis.schema.jibx.HasType;
import eu.europeana.metis.schema.jibx.HasURL;
import eu.europeana.metis.schema.jibx.IsPartOf;
import eu.europeana.metis.schema.jibx.IsReferencedBy;
import eu.europeana.metis.schema.jibx.IsRelatedTo;
import eu.europeana.metis.schema.jibx.Medium;
import eu.europeana.metis.schema.jibx.Pid;
import eu.europeana.metis.schema.jibx.PlaceType;
import eu.europeana.metis.schema.jibx.Provenance;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.metis.schema.jibx.References;
import eu.europeana.metis.schema.jibx.Relation;
import eu.europeana.metis.schema.jibx.ReplacesPID;
import eu.europeana.metis.schema.jibx.Rights;
import eu.europeana.metis.schema.jibx.Source;
import eu.europeana.metis.schema.jibx.Spatial;
import eu.europeana.metis.schema.jibx.Subject;
import eu.europeana.metis.schema.jibx.TableOfContents;
import eu.europeana.metis.schema.jibx.Temporal;
import eu.europeana.metis.schema.jibx.TimeSpanType;
import eu.europeana.metis.schema.jibx.Title;
import eu.europeana.metis.schema.jibx.Title1;
import eu.europeana.metis.schema.jibx.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RdfConversionUtilsTest {

  @Test
  void failRdfConversionUtilsInitialization() {
    //Force failure
    assertThrows(IllegalStateException.class, () -> new RdfConversionUtils(RdfConversionUtils.class));
  }

  @Test
  void getQualifiedElementNameForClass_ContextualClasses() {
    //Check contextual classes
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    assertEquals("edm:AgentType", rdfConversionUtils.getQualifiedElementNameForClass(AgentType.class));
    assertEquals("edm:TimeSpanType", rdfConversionUtils.getQualifiedElementNameForClass(TimeSpanType.class));
    assertEquals("edm:PlaceType", rdfConversionUtils.getQualifiedElementNameForClass(PlaceType.class));
    assertEquals("skos:Concept", rdfConversionUtils.getQualifiedElementNameForClass(Concept.class));
  }

  @Test
  void getQualifiedElementNameForClass_Dc() {
    //Check dc elements
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    assertEquals("dc:coverage", rdfConversionUtils.getQualifiedElementNameForClass(Coverage.class));
    assertEquals("dc:description", rdfConversionUtils.getQualifiedElementNameForClass(Description.class));
    assertEquals("dc:format", rdfConversionUtils.getQualifiedElementNameForClass(Format.class));
    assertEquals("dc:relation", rdfConversionUtils.getQualifiedElementNameForClass(Relation.class));
    assertEquals("dc:rights", rdfConversionUtils.getQualifiedElementNameForClass(Rights.class));
    assertEquals("dc:source", rdfConversionUtils.getQualifiedElementNameForClass(Source.class));
    assertEquals("dc:subject", rdfConversionUtils.getQualifiedElementNameForClass(Subject.class));
    assertEquals("dc:title", rdfConversionUtils.getQualifiedElementNameForClass(Title.class));
    assertEquals("dc:type", rdfConversionUtils.getQualifiedElementNameForClass(Type.class));
  }

  @Test
  void getQualifiedElementNameForClass_Dcterms() {
    //Check dcterms elements
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    assertEquals("dcterms:alternative", rdfConversionUtils.getQualifiedElementNameForClass(Alternative.class));
    assertEquals("dcterms:hasPart", rdfConversionUtils.getQualifiedElementNameForClass(HasPart.class));
    assertEquals("dcterms:isPartOf", rdfConversionUtils.getQualifiedElementNameForClass(IsPartOf.class));
    assertEquals("dcterms:isReferencedBy", rdfConversionUtils.getQualifiedElementNameForClass(IsReferencedBy.class));
    assertEquals("dcterms:medium", rdfConversionUtils.getQualifiedElementNameForClass(Medium.class));
    assertEquals("dcterms:provenance", rdfConversionUtils.getQualifiedElementNameForClass(Provenance.class));
    assertEquals("dcterms:references", rdfConversionUtils.getQualifiedElementNameForClass(References.class));
    assertEquals("dcterms:spatial", rdfConversionUtils.getQualifiedElementNameForClass(Spatial.class));
    assertEquals("dcterms:tableOfContents", rdfConversionUtils.getQualifiedElementNameForClass(TableOfContents.class));
    assertEquals("dcterms:temporal", rdfConversionUtils.getQualifiedElementNameForClass(Temporal.class));
    assertEquals("dcterms:title", rdfConversionUtils.getQualifiedElementNameForClass(Title1.class));
    assertEquals("dcterms:creator", rdfConversionUtils.getQualifiedElementNameForClass(Creator1.class));
  }

  @Test
  void getQualifiedElementNameForClass_Edm() {
    //Check edm elements
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    assertEquals("edm:currentLocation", rdfConversionUtils.getQualifiedElementNameForClass(CurrentLocation.class));
    assertEquals("edm:hasType", rdfConversionUtils.getQualifiedElementNameForClass(HasType.class));
    assertEquals("edm:isRelatedTo", rdfConversionUtils.getQualifiedElementNameForClass(IsRelatedTo.class));
    assertEquals("edm:pid", rdfConversionUtils.getQualifiedElementNameForClass(Pid.class));
    assertEquals("edm:hasURL", rdfConversionUtils.getQualifiedElementNameForClass(HasURL.class));
    assertEquals("edm:equivalentPID", rdfConversionUtils.getQualifiedElementNameForClass(EquivalentPID.class));
    assertEquals("edm:replacesPID", rdfConversionUtils.getQualifiedElementNameForClass(ReplacesPID.class));
  }

  @Test
  void convertRdfToBytes_withValue() throws IOException, SerializationException {
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    RDF rdf = rdfConversionUtils.convertStringToRdf(readFileToString("rdf/record-rdf-media.xml"));
    assertDoesNotThrow(() -> {byte[] result = rdfConversionUtils.convertRdfToBytes(rdf);
      assertNotNull(result);
    });
  }

  @Test
  void convertRdfToBytes_emptyRdf() throws IOException, SerializationException {
    final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    final RdfConversionUtils spyRdfConversionUtils = spy(rdfConversionUtils);

    doThrow(new SerializationException("Empty RDF"))
        .when(spyRdfConversionUtils)
        .convertRdfToBytes(any(RDF.class));

    RDF rdf = rdfConversionUtils.convertStringToRdf(readFileToString("rdf/record-rdf-media-empty.xml"));
    assertThrows(SerializationException.class, ()-> spyRdfConversionUtils.convertRdfToBytes(rdf));
  }

  static String readFileToString(String file) throws IOException {
    ClassLoader classLoader = RdfConversionUtilsTest.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(file);
    if (inputStream == null) {
      throw new IOException("Failed reading file " + file);
    }
    return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
  }
}
