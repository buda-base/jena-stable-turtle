package io.bdrc.jena.sttl;

import org.apache.jena.riot.adapters.RDFWriterRIOT;

/**
* A sorted TTL writer.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class RDFWriterSTTL extends RDFWriterRIOT {

    public RDFWriterSTTL(String jenaName) {
        super(jenaName);
    }

}
