package io.bdrc.jena.sttl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Blank nodes comparator.
 *  
 * @author Elie Roux
 * @author Buddhist Digital Resource Center (BDRC)
 * @version 0.1.0
 */
public class CompareComplex implements Comparator<Node> {

	public static Comparator<Node> defaultCompLiteral = new CompareLiterals();
	public Comparator<Node> compLiteral = defaultCompLiteral;
	public List<String> propUris = null;
	public Graph g = null;
	public static final List<String> defaultPropUris = new ArrayList<>();
	static {
		defaultPropUris.add(RDF.type.getURI());
		defaultPropUris.add(RDFS.label.getURI());
	}

	/**
	 * @return default predicate URIs for sorting
	 */
	public static List<String> getDefaultPropUris() {
		return defaultPropUris;
	}

	/**
	 * Default constructor. Needs a graph as we need to
	 * fetch the objects of the different predicated in
	 * a particular graph.
	 * 
	 * @param g
	 * The graph in which the comparison occurs.
	 */
	public CompareComplex(final Graph g) {
		this(defaultCompLiteral, defaultPropUris, g);
	}

	/**
	 * Advanced constructor.
	 * 
	 * @param compLiteral
	 * The Comparator that will be used for object comparison.
	 * @param propUris
	 * A list of predicate URIs that will be tried for the comparison.
	 * @param g
	 * The graph in which the comparison occurs.
	 */
	public CompareComplex(final Comparator<Node> compLiteral, final List<String> propUris, final Graph g) {
		super();
		this.compLiteral = compLiteral;
		this.propUris = propUris;
		this.g = g;
	}

	private static SortedMap<String, List<Node>> groupByPredicates(Collection<Triple> cluster) {
		final SortedMap<String, List<Node>> x = new TreeMap<>() ;
		for ( Triple t : cluster ) {
			final String p = t.getPredicate().getURI() ;
			if ( !x.containsKey(p) )
				x.put(p, new ArrayList<Node>()) ;
			x.get(p).add(t.getObject()) ;
		}
		return x ;
	}

	public Integer comparePGroups(SortedMap<String, List<Node>> pGroups1, SortedMap<String, List<Node>> pGroups2, boolean doRecurse) {
		// only stable if there is one object per compared property
		for (Entry<String, List<Node>> e : pGroups1.entrySet()) {
			if (!pGroups2.containsKey(e.getKey())) {
				return -1;
			}
			final Node o1 = e.getValue().get(0);
			final Node o2 = pGroups2.get(e.getKey()).get(0); 
			Integer res = CompareLiterals.compareUri(o1, o2);
			if (res != null && res != 0) 
				return res;
			if (res != null && res == 0)
				continue;
			if (res == null && o1.isBlank() && o2.isBlank() && doRecurse) {
				res = compare(o1, o2, false);
				if (res != null && res != 0)
					return res;
				continue;
			}
			res = compLiteral.compare(o1, o2);
			if (res != 0)
				return res;
		}
		// we don't want to return 0 when inside a recursion (doRecurse == false)
		return doRecurse ? 0 : null;
	}

	/**
	 * Main comparison function. Compares the objects
	 * associated with the predicate URIs given to the
	 * constructor, until a difference occurs.
	 * 
	 * @param t1
	 * The first node to compare.
	 * @param t2
	 * The second node to compare
	 * @return
	 * -1 or 1 in case of asserted comparison, 0 in case of equality or
	 * impossibility to compare (all listed predicates have the same value).
	 */
	@Override
	public int compare(final Node t1, final Node t2) {
		return compare(t1, t2, true);
	}
	
	public Integer compare(final Node t1, final Node t2, boolean doRecurse) {
		// sort by property
		Integer res = CompareLiterals.compareUri(t1, t2);
		if (res != null) return res;
		// iterate over the different properties registered by the user
		for (String propUri : this.propUris) {
			final Triple t1t = RiotLib.triple1(g, t1, NodeFactory.createURI(propUri), Node.ANY) ;
			final Triple t2t = RiotLib.triple1(g, t2, NodeFactory.createURI(propUri), Node.ANY) ;
			if (t1t == null) {
				if (t2t == null)
					// if neither of the nodes have the property, we just compare with the next property
					continue;
				return 1;
			}
			if (t2t == null) 
				return -1;
			// then we just compare in the regular way
			// note that this is not recursive: we don't apply the same method to
			// blank nodes we would encounter, although that would certainly be
			// relatively simple...
			final Node t1n = t1t.getObject();
			final Node t2n = t2t.getObject();
			res = CompareLiterals.compareUri(t1n, t2n);
			if (res != null && res != 0) 
				return res;
			if (res != null && res == 0)
				continue;
			if (res == null && t1n.isBlank() && t2n.isBlank() && doRecurse) {
				res = compare(t1n, t2n, false);
				if (res != 0)
					return res;
			}
			res = compLiteral.compare(t1n, t2n);
			if (res != 0)
				return res;
		}
		// if it's not enough, iterate over the properties of t1 + t2 in alphabetical order:
		final Collection<Triple> clusterT1 = RiotLib.triplesOfSubject(g, t1) ;
		final Collection<Triple> clusterT2 = RiotLib.triplesOfSubject(g, t2) ;
		final SortedMap<String, List<Node>> pGroups1 = groupByPredicates(clusterT1);
		final SortedMap<String, List<Node>> pGroups2 = groupByPredicates(clusterT2) ;
		if (pGroups1.keySet().size() > pGroups2.keySet().size()) {
			return comparePGroups(pGroups1, pGroups2, doRecurse);
		} else {
			res = comparePGroups(pGroups2, pGroups1, doRecurse);
			return (res != null) ? -res : null;
		}
	}
}

