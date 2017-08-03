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
    
    public static void registerWriter() {
        if (lang != null) return;
        lang = LangBuilder.create("STTL", "stupid-content").build();
        RDFLanguages.register(lang);
        RDFFormat format = new RDFFormat(lang);
        RDFWriterRegistry.register(lang, format);
        RDFWriterRegistry.register(format, new STTLWriterFactory()) ;
    }
    
    @Override
    public Lang getLang() {
        return lang;
    }

    @Override
    protected void output(IndentedWriter iOut, Graph graph, PrefixMap prefixMap, String baseURI, Context context) {
        SortedTurtleShell w = new SortedTurtleShell(iOut, prefixMap, baseURI, context) ;
        w.write(graph) ;
    }

}
