package io.bdrc.jena.sttl;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            NodeFactory.createLiteral("def", "en", RDF.dtLangString),
            NodeFactory.createLiteral("abc", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("def", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDnonNegativeInteger),
            NodeFactory.createLiteral("1", XSDDatatype.XSDinteger),
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
            NodeFactory.createLiteral("def", "en", RDF.dtLangString),
            NodeFactory.createLiteral("abc", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("def", XSDDatatype.XSDstring),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("1", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("+1", XSDDatatype.XSDnonNegativeInteger),
            NodeFactory.createLiteral("1.2", XSDDatatype.XSDfloat),
            NodeFactory.createLiteral("002", XSDDatatype.XSDinteger),
            NodeFactory.createLiteral("http://example.com", XSDDatatype.XSDanyURI),
            NodeFactory.createLiteral("false", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("true", XSDDatatype.XSDboolean),
            NodeFactory.createLiteral("2017-08-04T15:48:17+02:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteral("2017-08-04T13:48:31Z", XSDDatatype.XSDdateTime)
		));
	}
	
	@Test
	public void testBlanks() {
		Model m = ModelFactory.createDefaultModel();
		Resource t1 = m.createResource("http://example.com/type1");
		Resource t2 = m.createResource("http://example.com/type2");
		Literal l1 = m.createLiteral("abc");
		Literal l2 = m.createLiteral("def");
		Resource r0 = m.createResource(new AnonId("r0")).addProperty(RDF.type, t1);
		Resource r1 = m.createResource(new AnonId("r1")).addProperty(RDF.type, t2);
		Resource r2 = m.createResource(new AnonId("r2")).addProperty(RDF.type, t1).addProperty(RDFS.label, l1);
		Resource r3 = m.createResource(new AnonId("r3")).addProperty(RDF.type, t1).addProperty(RDFS.label, l2);
		Resource r4 = m.createResource(new AnonId("r4")).addProperty(RDFS.label, l1);
		Resource r5 = m.createResource(new AnonId("r5")).addProperty(RDFS.label, l2);
		// sort by type
		List<Node> list = Arrays.asList(r0.asNode(), r1.asNode(), r2.asNode(), r3.asNode(), r4.asNode(), r5.asNode());
		Collections.sort(list, new CompareComplex(m.getGraph()));
		assertThat(list, contains(r2.asNode(), r3.asNode(), r0.asNode(), r1.asNode(), r4.asNode(), r5.asNode()));
	}
	
	@Test
	public void testGeneral() throws IOException {
		Model m = ModelFactory.createDefaultModel();
		Lang sttl = STTLWriter.registerWriter();
		SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
		nsPrio.put(SKOS.getURI(), 1);
		nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
		nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
		List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
		predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
		predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
		predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
		Context ctx = new Context();
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
		// G844
		String content = new String(Files.readAllBytes(Paths.get("src/test/resources/G844.ttl"))).trim();
		m.read("src/test/resources/G844.ttl", "TURTLE");
		RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		w.output(baos);
		String res = baos.toString().trim();
		assertTrue(res.equals(content));
		// outline
		content = new String(Files.readAllBytes(Paths.get("src/test/resources/outline.ttl"))).trim();
		m = ModelFactory.createDefaultModel();
		m.read("src/test/resources/outline.ttl", "TURTLE");
		w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
		baos = new ByteArrayOutputStream();
		w.output(baos);
		res = baos.toString().trim();
		assertTrue(res.equals(content));
	}
	
}
