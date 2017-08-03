package io.bdrc.jena.sttl;

import java.text.Collator;
import java.util.Comparator;

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
        if (t1.isURI()) {
            if (t2.isBlank()) 
                return 1;
            if (t2.isURI())
                return t1.getURI().compareTo(t2.getURI());
            return 1;
        }
        if (t2.isURI()) 
            return -1;
        if (t1.isBlank()) {
            if (!t2.isBlank()) 
                return -1;
            return 0;
        }
        if (t2.isBlank())
            return 1;
        return null;
    }
    
    @Override
    public int compare(final Node t1, final Node t2) {
        Integer res = compareUri(t1, t2);
        if (res != null) return res;
        final String lang1 = t1.getLiteralLanguage();
        final String lang2 = t2.getLiteralLanguage();
        if (!lang1.isEmpty()) {
            res = lang1.compareTo(lang2);
            if (res != 0) return res;
        } else if (!lang2.isEmpty()) {
            return -1;
        }
        final Object o1 = t1.getLiteralValue();
        final Object o2 = t2.getLiteralValue();
        if (o1 instanceof String) {
            if (o2 instanceof String)
                return compareStrings((String) o1, (String) o2, lang1);
            return 1;
        }
        if (Util.comparable(t1, t2)) {
            return Util.compareTypedLiterals(t1, t2);                
        }
        return 0;
    }
}