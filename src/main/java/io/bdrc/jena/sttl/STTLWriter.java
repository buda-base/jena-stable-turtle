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

public class STTLWriter extends TurtleWriterBase {

    public static Lang lang = null;
    public static final String SYMBOLS_NS = "http://bdrc.io/sttl#" ;
    
    public static Lang registerWriter() {
    	return registerWriter("STTL", "stupid-content");
    }
    
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
