package io.bdrc.jena.sttl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class CompareComplex implements Comparator<Node> {
    
	public static Comparator<Node> defaultCompLiteral = new CompareLiterals();
    public Comparator<Node> compLiteral = defaultCompLiteral;
    public List<String> propUris = null;
    public Graph g = null;

    public static List<String> getDefaultPropUris() {
    	return Arrays.asList(RDF.type.getURI(), RDFS.label.getURI());
    }
    
    public CompareComplex(final Graph g) {
    	this(defaultCompLiteral, getDefaultPropUris(), g);
    }
    
    public CompareComplex(final Comparator<Node> compLiteral, final List<String> propUris, final Graph g) {
        super();
        this.compLiteral = compLiteral;
        this.propUris = propUris;
        this.g = g;
    }
    
    @Override
    public int compare(final Node t1, final Node t2) {
        // sort by property
        Integer res = CompareLiterals.compareUri(t1, t2);
        if (res != null) return res;
        for (String propUri : this.propUris) {
            final Triple t1t = RiotLib.triple1(g, t1, NodeFactory.createURI(propUri), Node.ANY) ;
            final Triple t2t = RiotLib.triple1(g, t2, NodeFactory.createURI(propUri), Node.ANY) ;
            if (t1t == null) {
            	if (t2t == null)
            		// if neither of the nodes have the property, we just compare with the next property
            		continue;
            	return 1;
            }
            if (t2t == null) 
            	return -1;
            // then we just compare in the regular way
            // note that this is not recursive: we don't apply the same method to
            // blank nodes we would encounter, although that would certainly be
            // relatively simple...
            final Node t1n = t1t.getObject();
            final Node t2n = t2t.getObject();
            res = CompareLiterals.compareUri(t1n, t2n);
            if (res != null && res != 0) 
            	return res;
            if (res != null && res == 0)
            	continue;
            res = compLiteral.compare(t1n, t2n);
            if (res != 0)
            	return res;
        }
        return 0;
    }
}

