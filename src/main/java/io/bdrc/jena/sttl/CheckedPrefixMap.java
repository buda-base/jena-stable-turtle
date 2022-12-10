package io.bdrc.jena.sttl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.riot.system.PrefixEntry;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.shared.PrefixMapping;

public class CheckedPrefixMap implements PrefixMap {
    
    public final PrefixMap pm;
    public final Map<String,Boolean> usedPrefixes = new HashMap<>(); 
    
    public CheckedPrefixMap(final PrefixMap pm) {
        this.pm = pm;
    }

    @Override
    public final String get(final String prefix) {
        return this.pm.get(prefix);
    }

    @Override
    public final Map<String, String> getMapping() {
        return this.pm.getMapping();
    }
    
    public final Map<String, String> getUsedMappingCopy() {
        final Map<String, String> res = new HashMap<>();
        for (Entry<String, String> e : this.pm.getMapping().entrySet()) {
            if (usedPrefixes.containsKey(e.getKey()))
                res.put(e.getKey(), e.getValue());
        }
        return res;
    }

    @Override
    public final Map<String, String> getMappingCopy() {
        return this.pm.getMappingCopy();
    }

    @Override
    public final void forEach(final BiConsumer<String, String> action) {
        this.pm.forEach(action);
    }

    @Override
    public final Stream<PrefixEntry> stream() {
        return this.pm.stream();
    }

    @Override
    public final void add(final String prefix, final String iriString) {
        this.pm.add(prefix, iriString);
    }

    @Override
    public final void putAll(final PrefixMap pmap) {
        this.pm.putAll(pmap);
    }

    @Override
    public final void putAll(final PrefixMapping pmap) {
        this.pm.putAll(pmap);
    }

    @Override
    public final void putAll(final Map<String, String> mapping) {
        this.pm.putAll(mapping);
    }

    @Override
    public final void delete(final String prefix) {
        this.pm.delete(prefix);
    }

    @Override
    public final void clear() {
        this.pm.clear();
    }

    @Override
    public final boolean containsPrefix(final String prefix) {
        return this.pm.containsPrefix(prefix);
    }

    @Override
    public final String abbreviate(final String uriStr) {
        System.out.println("abbreavio");
        Objects.requireNonNull(uriStr);
        final Pair<String, String> p = this.abbrev(uriStr);
        if (p == null)
            return null;
        return p.getLeft() + ":" + p.getRight();
    }

    @Override
    public final Pair<String, String> abbrev(final String uriStr) {
        final Pair<String, String> p = this.pm.abbrev(uriStr);
        if (p == null)
            return null;
        this.usedPrefixes.put(p.getLeft(), true);
        return p;
    }

    @Override
    public final String expand(final String prefixedName) {
        return this.pm.expand(prefixedName);
    }

    @Override
    public final String expand(final String prefix, final String localName) {
        return this.pm.expand(prefix, localName);
    }

    @Override
    public final boolean isEmpty() {
        return this.pm.isEmpty();
    }

    @Override
    public final int size() {
        return this.pm.size();
    }

        
    
}
