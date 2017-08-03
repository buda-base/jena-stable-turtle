package io.bdrc.jena.sttl;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

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
		assertThat(list, hasItems(
	            RDF.type.asNode(),
	            RDFS.label.asNode(),
	            SKOS.altLabel.asNode(),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/admin/1"),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/2")
		));
		// create more interesting comparison
		SortedMap<String, Integer> NSPriorities = ComparePredicates.getDefaultNSPriorities();
		NSPriorities.put(SKOS.getURI(), 3);
		Collections.sort(list, new ComparePredicates(NSPriorities, 2));
		assertThat(list, hasItems(
	            RDF.type.asNode(),
	            RDFS.label.asNode(),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/admin/1"),
	            NodeFactory.createURI("http://purl.bdrc.io/ontology/2"),
	            SKOS.altLabel.asNode()
		));
	}
	
}
