package io.bdrc.jena.sttl;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.writer.TurtleWriterBase;
import org.apache.jena.sparql.util.Context;

/**
* Sorted TTL Writer.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class STTLWriter extends TurtleWriterBase {

    /**
    * Lang associated with this writer.
    */
    public static Lang lang = null;
    /**
    * Prefix for Symbol creation in contexts.
    */
    public static final String SYMBOLS_NS = "http://bdrc.io/sttl#" ;
    
    /**
     * Registers the writer components using "STTL" as name
     * and "x-sorted-ttl" as content.
     *
     * @return
     * The Lang associated with sorted TTL.
     */
    public static Lang registerWriter() {
    	return registerWriter("STTL", "x-sorted-ttl");
    }

    /**
    * Registers the writer components.
    * 
    * @param langName
    * The lang name to be associated with sorted TTL.
    * @param contentType
    * The content type to be associated with sorted TTL.
    * @return
    * The Lang associated with sorted TTL.
    */
    public static Lang registerWriter(String langName, String contentType) {
        if (lang != null) return lang;
        lang = LangBuilder.create(langName, contentType).build();
        RDFLanguages.register(lang);
        RDFFormat format = new RDFFormat(lang);
        RDFWriterRegistry.register(lang, format);
        RDFWriterRegistry.register(format, new STTLWriterFactory());
        return lang;
    }
    
    @Override
    public Lang getLang() {
        return lang;
    }

    @Override
    protected void output(IndentedWriter iOut, Graph graph, PrefixMap prefixMap, String baseURI, Context context) {
        TurtleShell w = new TurtleShell(iOut, prefixMap, baseURI, context) ;
        w.write(graph) ;
    }

}
