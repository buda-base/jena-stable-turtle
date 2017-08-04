package io.bdrc.jena.sttl;

import java.text.Collator;
import java.util.Comparator;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.reasoner.rulesys.Util;

public final class CompareLiterals implements Comparator<Node> {
    
    public static Collator collator = Collator.getInstance(); // root locale
    
    public static int compareStrings(final String s0, final String s1, final String lang) {
        // maybe we want to use the locale corresponding to the lang, maybe not
        return collator.compare(s0, s1);
    }
    
    public static Integer compareUri(final Node t1, final Node t2) {
        // URIs then literals then blank nodes
    	// if the situation is undecidable (both blank nodes or both literals), answers null
    	// 0 is answered when the equality is asserted
        if (t1.isURI()) {
            if (t2.isBlank()) 
                return -1;
            if (t2.isURI())
                return t1.getURI().compareTo(t2.getURI());
            return -1;
        }
        if (t2.isURI()) 
            return 1;
        if (t1.isBlank()) {
            if (!t2.isBlank()) 
                return 1;
            return null;
        }
        if (t2.isBlank())
            return -1;
        return null;
    }
    
    @Override
    public int compare(final Node t1, final Node t2) {
    	// strings with lang tag (sorted by lang tag then by content)
    	// then strings (sorted)
    	// then numbers (sorted)
    	// then others sorted by type then contentS
        Integer res = compareUri(t1, t2);
        if (res != null) return res;
        final String lang1 = t1.getLiteralLanguage();
        final String lang2 = t2.getLiteralLanguage();
        if (!lang1.isEmpty()) {
            res = lang1.compareTo(lang2);
            if (res != 0) return res;
            return compareStrings(t1.getLiteralLexicalForm(), t1.getLiteralLexicalForm(), lang1);
        } else if (!lang2.isEmpty()) {
            return 1;
        }
        // RDF.langStrings are taken into account, let's focus on XSDDatatype.XSDstring
        final RDFDatatype t1t = t1.getLiteralDatatype();
        final RDFDatatype t2t = t2.getLiteralDatatype();
        if (t1t == XSDDatatype.XSDstring) {
        	if (t2t != XSDDatatype.XSDstring)
        		return -1;
        	return compareStrings(t1.getLiteralLexicalForm(), t1.getLiteralLexicalForm(), null);
        }
        if (t2t == XSDDatatype.XSDstring)
        	return 1;

        if (Util.comparable(t1, t2)) {
            return Util.compareTypedLiterals(t1, t2);                
        }
        
        final Object o1 = t1.getLiteralValue();
        final Object o2 = t2.getLiteralValue();
        // non-comparable, so not both number nor both dates
        // numbers grouped first
        if (o1 instanceof Number)
        	return -1;
        if (o2 instanceof Number)
        	return 1;
        
        res = t1t.getURI().compareTo(t2t.getURI());
        if (res != 0) 
        	return res;
        return t1.getLiteralLexicalForm().compareTo(t2.getLiteralLexicalForm());
    }
}