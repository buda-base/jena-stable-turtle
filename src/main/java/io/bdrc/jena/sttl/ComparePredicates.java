package io.bdrc.jena.sttl;

import static org.apache.jena.riot.writer.WriterConst.RDF_type;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class ComparePredicates implements Comparator<Node> {
	
	private SortedMap<String, Integer> NSPriorities;
	private int defaultPriority;
	
    private int getNSPriority(final Node p) {
        if ( p.equals(RDF_type) )
            return 0 ;
        final String ns = p.getNameSpace(); 
        Integer res = this.NSPriorities.get(ns);
        if (res != null) 
        	return res;
        return defaultPriority ;
    }
    
    public static SortedMap<String, Integer> getDefaultNSPriorities() {
    	// Default order of properties.
    	// rdf:type ("a")
    	// RDF and RDFS
    	// Others, sorted by URI.
    	SortedMap<String, Integer> NSPriorities = new TreeMap<>();
    	NSPriorities.put(RDF.getURI(), 1);
    	NSPriorities.put(RDFS.getURI(), 1);
    	return NSPriorities;
    }
    
    public ComparePredicates() {
    	this(getDefaultNSPriorities(), 2);
    }
    
    public ComparePredicates(SortedMap<String,Integer> NSPriorities, int defaultPriority) {
    	super();
    	this.NSPriorities = NSPriorities;
    	this.defaultPriority = defaultPriority;
    }

    @Override
    public int compare(final Node t1, final Node t2) {
        final int class1 = getNSPriority(t1) ;
        final int class2 = getNSPriority(t2) ;
        if ( class1 != class2 ) {
        	return Integer.compare(class1, class2) ;
        }
        final String p1 = t1.getURI() ;
        final String p2 = t2.getURI() ;
        return p1.compareTo(p2) ;
    }
}