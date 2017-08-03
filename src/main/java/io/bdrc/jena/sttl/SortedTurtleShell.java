package io.bdrc.jena.sttl;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.util.Context;


public class SortedTurtleShell extends TurtleShell {

    protected SortedTurtleShell(IndentedWriter out, PrefixMap pmap, String baseURI, Context context) {
        super(out, pmap, baseURI, context);
    }
    
    // write comes from TurtleWriter.java
    public void write(Graph graph) {
        writeBase(baseURI);
        writePrefixes(prefixMap) ;
        if ( !prefixMap.isEmpty() && !graph.isEmpty() )
            out.println();
        writeGraphTTL(graph);
    }

}
