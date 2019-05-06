package io.bdrc.jena.sttl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.apache.jena.riot.writer.WriterConst.GDFT_BRACE ;
import static org.apache.jena.riot.writer.WriterConst.INDENT_GDFT ;
import static org.apache.jena.riot.writer.WriterConst.INDENT_GNMD ;
import static org.apache.jena.riot.writer.WriterConst.NL_GDFT_END ;
import static org.apache.jena.riot.writer.WriterConst.NL_GDFT_START ;
import static org.apache.jena.riot.writer.WriterConst.NL_GNMD_END ;
import static org.apache.jena.riot.writer.WriterConst.NL_GNMD_START ;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

class TriGShell extends TurtleShell
{
    TriGShell(final IndentedWriter out, final PrefixMap prefixMap, final String baseURI, final Context context) {
        super(out, prefixMap, baseURI, context) ;
    }

    void write(final DatasetGraph dsg) {
        writeBase(baseURI) ;
        writePrefixes(prefixMap) ;
        if ( !prefixMap.isEmpty() && !dsg.isEmpty() )
            out.println() ;

        final Iterator<Node> graphNamesI = dsg.listGraphNodes() ;
        final List<Node> graphNames = new ArrayList<>();
        graphNamesI.forEachRemaining(graphNames::add);
        Collections.sort(graphNames, compLiterals);

        boolean anyGraphOutput = writeGraphTriG(dsg, null) ;

        for ( final Node gn : graphNames ) {
            if ( anyGraphOutput )
                out.println() ;
            anyGraphOutput |= writeGraphTriG(dsg, gn) ;
        }
    }

    /** Return true if anything written */
    private boolean writeGraphTriG(final DatasetGraph dsg, final Node name) {
        boolean dftGraph =  ( name == null || name == Quad.defaultGraphNodeGenerated  ) ;
        
        if ( dftGraph && dsg.getDefaultGraph().isEmpty() )
            return false ;
        
        if ( dftGraph && ! GDFT_BRACE ) {
            // Non-empty default graph, no braces.
            // No indenting.
            writeGraphTTL(dsg, name) ;
            return true ;
        }
        
        // The graph will go in braces, whether non-empty default graph or a named graph. 
        final boolean NL_START =  ( dftGraph ? NL_GDFT_START : NL_GNMD_START ) ; 
        final boolean NL_END =    ( dftGraph ? NL_GDFT_END : NL_GNMD_END ) ; 
        final int INDENT_GRAPH =  ( dftGraph ? INDENT_GDFT : INDENT_GNMD ) ; 

        if ( !dftGraph ) {
            writeNode(name) ;
            out.print(" ") ;
        }

        out.print("{") ;
        if ( NL_START )
            out.println() ;
        else
            out.print(" ") ;

        out.incIndent(INDENT_GRAPH) ;
        writeGraphTTL(dsg, name) ;
        out.decIndent(INDENT_GRAPH) ;

        if ( NL_END )
            out.ensureStartOfLine() ;
        out.println("}") ;
        return true ;
    }
}