package io.bdrc.jena.sttl;

import org.apache.jena.riot.adapters.RDFWriterRIOT;

/**
* A sorted TriG writer.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public class RDFWriterSTriG extends RDFWriterRIOT {

    public RDFWriterSTriG(String jenaName) {
        super(jenaName);
    }

}
