package io.bdrc.jena.sttl;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

public class TestSttl {

	@Test
	public void testPredicateOrder() {
        List<Node> list = Arrays.asList(
            RDF.type.asNode(),
            RDFS.label.asNode(),
            SKOS.altLabel.asNode(),
            NodeFactory.createURI("http://purl.bdrc.io/ontology/admin/1"),
            NodeFactory.createURI("http://purl.bdrc.io/ontology/2")
        );
        // default comparison
		Collections.sort(list, new ComparePredicates());
		assertThat(list, contains(
	            RDF.type.asNode(),
	            RDFS.label.asNode(),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/2"),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/admin/1"),
	            SKOS.altLabel.asNode()
		));
		// create more interesting comparison
		SortedMap<String, Integer> NSPriorities = ComparePredicates.getDefaultNSPriorities();
		NSPriorities.put(SKOS.getURI(), 3);
		Collections.sort(list, new ComparePredicates(NSPriorities, 2));
		assertThat(list, contains(
	            RDF.type.asNode(),
	            RDFS.label.asNode(),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/2"),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/admin/1"),
	            SKOS.altLabel.asNode()
		));
	}
	
	@Test
	public void testUriLitComplex() {
		List<Node> list = Arrays.asList(
            NodeFactory.createLiteral("def", "en", RDF.dtLangString),
            NodeFactory.createLiteral("abc", "en", RDF.dtLangString),
            RDFS.label.asNode(),
            SKOS.altLabel.asNode(),
            NodeFactory.createBlankNode("123")
        );
		Collections.sort(list, new CompareLiterals());
		assertThat(list, contains(
	            RDFS.label.asNode(),
	            SKOS.altLabel.asNode(),
	            NodeFactory.createLiteral("abc", "en", RDF.dtLangString),
	            NodeFactory.createLiteral("def", "en", RDF.dtLangString),
	            NodeFactory.createBlankNode("123")
		));
	}

	@Test
	public void testLiterals() {
		List<Node> list = Arrays.asList(
            NodeFactory.createLiteral("def", "aa", RDF.dtLangString),
            NodeFactory.createLiteral("abc", "en", RDF.dtLangString),
            NodeFactory.createLiteral("abc", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("def", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("002", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("1.2", XSDDatatype.XSDfloat),
            NodeFactory.createLiteral("2017-08-04T15:48:17+02:00", XSDDatatype.XSDdateTime), // timestamp 1501854497
            NodeFactory.createLiteral("2017-08-04T13:48:31Z", XSDDatatype.XSDdateTime),      // timestamp 1501854511
            NodeFactory.createLiteral("true", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("false", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("http://example.com", XSDDatatype.XSDanyURI)
        );
		Collections.sort(list, new CompareLiterals());
		assertThat(list, contains(
            NodeFactory.createLiteral("def", "aa", RDF.dtLangString),
            NodeFactory.createLiteral("abc", "en", RDF.dtLangString),
            NodeFactory.createLiteral("abc", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("def", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("1.2", XSDDatatype.XSDfloat),
            NodeFactory.createLiteral("002", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("http://example.com", XSDDatatype.XSDanyURI),
            NodeFactory.createLiteral("false", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("true", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("2017-08-04T15:48:17+02:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteral("2017-08-04T13:48:31Z", XSDDatatype.XSDdateTime)
		));
	}
	
}
