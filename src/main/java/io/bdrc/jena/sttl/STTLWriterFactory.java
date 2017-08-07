package io.bdrc.jena.sttl;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterGraphRIOT;
import org.apache.jena.riot.WriterGraphRIOTFactory;

/**
* A sorted TTL Writer Factory.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class STTLWriterFactory implements WriterGraphRIOTFactory {

    @Override
    public WriterGraphRIOT create(RDFFormat syntaxForm) {
        return new STTLWriter() ;
    }

}
