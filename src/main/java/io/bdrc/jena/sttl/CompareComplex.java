package io.bdrc.jena.sttl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
* Blank nodes comparator.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class CompareComplex implements Comparator<Node> {
    
	public static Comparator<Node> defaultCompLiteral = new CompareLiterals();
    public Comparator<Node> compLiteral = defaultCompLiteral;
    public List<String> propUris = null;
    public Graph g = null;
    public static final List<String> defaultPropUris = new ArrayList<>();
    static {
    	defaultPropUris.add(RDF.type.getURI());
    	defaultPropUris.add(RDFS.label.getURI());
    }

	/**
	* @return default predicate URIs for sorting
	*/
    public static List<String> getDefaultPropUris() {
    	return defaultPropUris;
    }

	/**
	* Default constructor. Needs a graph as we need to
	* fetch the objects of the different predicated in
	* a particular graph.
	* 
	* @param g
	* The graph in which the comparison occurs.
	*/
    public CompareComplex(final Graph g) {
    	this(defaultCompLiteral, defaultPropUris, g);
    }
    
    /**
	* Advanced constructor.
	* 
	* @param compLiteral
	* The Comparator that will be used for object comparison.
	* @param propUris
	* A list of predicate URIs that will be tried for the comparison.
	* @param g
	* The graph in which the comparison occurs.
	*/
    public CompareComplex(final Comparator<Node> compLiteral, final List<String> propUris, final Graph g) {
        super();
        this.compLiteral = compLiteral;
        this.propUris = propUris;
        this.g = g;
    }
    
    /**
	* Main comparison function. Compares the objects
	* associated with the predicate URIs given to the
	* constructor, until a difference occurs.
	* 
	* @param t1
	* The first node to compare.
	* @param t2
	* The second node to compare
	* @return
	* -1 or 1 in case of asserted comparison, 0 in case of equality or
	* impossibility to compare (all listed predicates have the same value).
	*/
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

