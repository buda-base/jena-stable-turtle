package io.bdrc.jena.sttl;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterGraphRIOT;
import org.apache.jena.riot.WriterGraphRIOTFactory;

public class STTLWriterFactory implements WriterGraphRIOTFactory {

    @Override
    public WriterGraphRIOT create(RDFFormat syntaxForm) {
        return new STTLWriter() ;
    }

}
