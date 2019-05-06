package io.bdrc.jena.sttl;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.WriterDatasetRIOTFactory;

/**
* A sorted TriG Writer Factory.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class STriGWriterFactory implements WriterDatasetRIOTFactory {

    @Override
    public WriterDatasetRIOT create(RDFFormat syntaxForm) {
        return new STriGWriter() ;
    }

}
