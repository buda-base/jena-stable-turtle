package io.bdrc.jena.sttl;

import static org.apache.jena.riot.writer.WriterConst.RDF_type;

import java.util.Comparator;

import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

// Order of properties.
// rdf:type ("a")
// RDF and RDFS
// Other.
// Sorted by URI.

public class ComparePredicates implements Comparator<Node> {
    private static int classification(final Node p) {
        if ( p.equals(RDF_type) )
            return 0 ;

        if ( p.getURI().startsWith(RDF.getURI()) || p.getURI().startsWith(RDFS.getURI()) )
            return 1 ;

        return 2 ;
    }

    @Override
    public int compare(final Node t1, final Node t2) {
        final int class1 = classification(t1) ;
        final int class2 = classification(t2) ;
        if ( class1 != class2 ) {
            // Java 1.7
            // return Integer.compare(class1, class2) ;
            if ( class1 < class2 )
                return -1 ;
            if ( class1 > class2 )
                return 1 ;
            return 0 ;
        }
        final String p1 = t1.getURI() ;
        final String p2 = t2.getURI() ;
        return p1.compareTo(p2) ;
    }
}