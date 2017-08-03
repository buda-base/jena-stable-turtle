package io.bdrc.jena.sttl;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestSttl {

	@Test
	public void testPredicateOrder() {
        List<Node> list = Arrays.asList(
            NodeFactory.createURI("http://example.com/test2"),
            NodeFactory.createURI("http://example.com/test1")
        );
		Collections.sort(list, new ComparePredicates());
		assertThat(list, hasItems(
			NodeFactory.createURI("http://example.com/test1"),
            NodeFactory.createURI("http://example.com/test2")
		));
	}
	
}
