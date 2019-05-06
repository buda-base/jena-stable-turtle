package io.bdrc.jena.sttl;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.writer.TriGWriterBase;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;

/**
* Sorted TriG Writer.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class STriGWriter extends TriGWriterBase {

    /**
    * Lang associated with this writer.
    */
    public static Lang lang = null;
    /**
    * Prefix for Symbol creation in contexts.
    */
    public static final String SYMBOLS_NS = "http://bdrc.io/strig#" ;
    
    /**
     * Registers the writer components using "STriG" as name
     * and "x-sorted-trig" as content.
     *
     * @return
     * The Lang associated with sorted TriG.
     */
    public static Lang registerWriter() {
    	return registerWriter("STriG", "x-sorted-trig");
    }

    /**
    * Registers the writer components.
    * 
    * @param langName
    * The lang name to be associated with sorted TriG.
    * @param contentType
    * The content type to be associated with sorted TriG.
    * @return
    * The Lang associated with sorted TriG.
    */
    public static Lang registerWriter(String langName, String contentType) {
        if (lang != null) return lang;
        lang = LangBuilder.create(langName, contentType).build();
        RDFLanguages.register(lang);
        RDFFormat format = new RDFFormat(lang);
        RDFWriterRegistry.register(lang, format);
        RDFWriterRegistry.register(format, new STriGWriterFactory());
        return lang;
    }
    
    @Override
    public Lang getLang() {
        return lang;
    }

    @Override
    protected void output(IndentedWriter iOut, DatasetGraph graph, PrefixMap prefixMap, String baseURI, Context context) {
        TriGShell w = new TriGShell(iOut, prefixMap, baseURI, context) ;
        w.write(graph) ;
    }

}
