package io.bdrc.jena.sttl;

import java.util.Comparator;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.RiotLib;

public class CompareComplex implements Comparator<Node> {
    
    public Comparator<Node> compLiteral = null;
    public List<String> propUris = null;
    public Graph g = null;
    // create a property: ResourceFactory.createProperty(uri)
    
    public CompareComplex(final Comparator<Node> compLiteral, final List<String> propUris, final Graph g) {
        super();
        this.compLiteral = compLiteral;
        this.propUris = propUris;
        this.g = g;
    }
    
    @Override
    public int compare(final Node t1, final Node t2) {
        // sort named nodes by uri
        Integer res = CompareLiterals.compareUri(t1, t2);
        if (res != null) return res;
        for (String propUri : this.propUris) {
            //res = compareProperties(r1, r2, p);
            final Triple t1t = RiotLib.triple1(g, t1, NodeFactory.createURI(propUri), Node.ANY) ;
            final Triple t2t = RiotLib.triple1(g, t1, NodeFactory.createURI(propUri), Node.ANY) ;
            if (t1t == null && t2t != null) return -1;
            if (t1t != null && t2t == null) return 1;
            final Node t1n = t1t.getObject();
            final Node t2n = t2t.getObject();
            res = CompareLiterals.compareUri(t1, t2);
            if (res != null) return res;
            return compLiteral.compare(t1n, t2n);
        }
        return 0;
    }
}

