package io.bdrc.jena.sttl;

import java.text.Collator;
import java.util.Comparator;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.reasoner.rulesys.Util;

/**
* Literal comparator.
*  
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
*/
public final class CompareLiterals implements Comparator<Node> {
    
    public static Collator collator = Collator.getInstance(); // root locale
    
	/**
	* Comparison of two string literals, using the root collation.
	*  
	* @param s1
	* first string to compare
	* @param s2
	* second string to compare
	* @param lang
	* the lang tag of the string literal (ignored, for subclassing only)
	* @return
	* the result of the string comparison
	*/
    public static int compareStrings(final String s1, final String s2, final String lang) {
        return collator.compare(s1, s2);
    }
    
	/**
	* Compare two nodes in terms of URI (not literals).
	* Computes the following order:
	*   - URIs (sorted)
	*   - literals (unsorted)
	*   - blank nodes (unsorted)
	* 
	* @param t1
	* first node
	* @param t2
	* second node
	* @return null in case of unhandled comparison (both literals or both blanks),
	* 0 in case of asserted equality, -1 or 1 is case of asserted inequality.
	*/
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
            if (t1.getBlankNodeId().equals(t2.getBlankNodeId()))
                return 0;
            return null;
        }
        if (t2.isBlank())
            return -1;
        return null;
    }
    
	/**
	* Compare two objects
	* Computes the following order:
	*   - URIs (sorted)
	*   - literals (sorted)
	*   - blank nodes (unsorted)
	* 
	* Literals are sorted with the following rules:
	* - first `rdf:langString`s then `xsd:string`s then numbers,
	*     then everything else, sorted by type uri then value
    * - `rdf:langString`s are sorted by lang then value
    * - string values are sorted with the root collation, not in the locale corresponding to the language
    * - date values are sorted by date
    * - numbers are sorted first by value then by type uri, so the following order would be respected:
    *     * "+1"^^xsd:integer
    *     * "1"^^xsd:integer
    *     * "+1"^^xsd:nonNegativeInteger
    *     * "1.2"^^xsd:float
    *     * "2"^^xsd:integer
	* 
	* @param t1
	* first node
	* @param t2
	* second node
	* @return 0 only in case of full equality (value, lang, type), -1 or 1 otherwise
	*/
    @Override
    public int compare(final Node t1, final Node t2) {
    	// strings with lang tag (sorted by lang tag then by content)
    	// then strings (sorted)
    	// then numbers (sorted)
    	// then others sorted by type then contentS
        Integer res = compareUri(t1, t2);
        if (res != null) return res;
        if (t1.isBlank() && t2.isBlank())
        	return t1.getBlankNodeLabel().compareTo(t2.getBlankNodeLabel());
        final String lang1 = t1.getLiteralLanguage();
        final String lang2 = t2.getLiteralLanguage();
        if (!lang1.isEmpty()) {
            res = lang1.compareTo(lang2);
            if (res != 0) return res;
            return compareStrings(t1.getLiteralLexicalForm(), t2.getLiteralLexicalForm(), lang1);
        } else if (!lang2.isEmpty()) {
            return 1;
        }
        // RDF.langStrings are taken into account, let's focus on XSDDatatype.XSDstring
        final RDFDatatype t1t = t1.getLiteralDatatype();
        final RDFDatatype t2t = t2.getLiteralDatatype();
        if (t1t == XSDDatatype.XSDstring) {
        	if (t2t != XSDDatatype.XSDstring)
        		return -1;
        	return compareStrings(t1.getLiteralLexicalForm(), t2.getLiteralLexicalForm(), null);
        }
        if (t2t == XSDDatatype.XSDstring)
        	return 1;

        if (Util.comparable(t1, t2)) {
        	res = Util.compareTypedLiterals(t1, t2);
            if (res != 0) return res;
        }
        
        final Object o1 = t1.getLiteralValue();
        final Object o2 = t2.getLiteralValue();
        // group numbers
        if (o1 instanceof Number && !(o2 instanceof Number))
        	return -1;
        if (o2 instanceof Number && !(o1 instanceof Number))
        	return 1;
        
        res = t1t.getURI().compareTo(t2t.getURI());
        if (res != 0) 
        	return res;
        return t1.getLiteralLexicalForm().compareTo(t2.getLiteralLexicalForm());
    }
}