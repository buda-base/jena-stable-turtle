/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for ad
 * ditional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bdrc.jena.sttl;

import static org.apache.jena.riot.writer.WriterConst.GAP_P_O;
import static org.apache.jena.riot.writer.WriterConst.GAP_S_P;
import static org.apache.jena.riot.writer.WriterConst.LONG_PREDICATE;
import static org.apache.jena.riot.writer.WriterConst.LONG_SUBJECT;
import static org.apache.jena.riot.writer.WriterConst.OBJECT_LISTS;
import static org.apache.jena.riot.writer.WriterConst.PREFIX_IRI;
import static org.apache.jena.riot.writer.WriterConst.RDF_First;
import static org.apache.jena.riot.writer.WriterConst.RDF_Nil;
import static org.apache.jena.riot.writer.WriterConst.RDF_Rest;
import static org.apache.jena.riot.writer.WriterConst.RDF_type;
import static org.apache.jena.riot.writer.WriterConst.rdfNS;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.lib.SetUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.out.NodeFormatterTTL_MultiLine;
import org.apache.jena.riot.out.NodeToLabel;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.DirectiveStyle;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Base class to support the pretty forms of Turtle-related languages (Turtle,
 * TriG)
 */
public class TurtleShell {
    protected IndentedWriter out;
    protected final NodeFormatter nodeFmt;
    protected final PrefixMap prefixMap;
    protected final String baseURI;
    protected final Boolean onlyWriteUsedPrefixes;

    protected int indent_base = 4;
    protected int predicate_base_width = 14;
    protected int long_subject = LONG_SUBJECT;
    protected boolean objects_multi_line = false;
    protected boolean named_dot_new_line = false;

    private Comparator<Node> compPredicates;
    protected static final Comparator<Node> compLiterals = new CompareLiterals();
    private List<String> complexPredicatesPriorities = null;

    protected TurtleShell(final IndentedWriter out, PrefixMap pmap, final String baseURI, final Context context) {
        this.out = out;
        if (pmap == null)
            pmap = PrefixMapFactory.emptyPrefixMap();
        this.onlyWriteUsedPrefixes = context.get(Symbol.create(STTLWriter.SYMBOLS_NS + "onlyWriteUsedPrefixes"), false);
        if (this.onlyWriteUsedPrefixes) {
            pmap = new CheckedPrefixMap(pmap);
        }
        this.prefixMap = pmap;
        this.baseURI = baseURI;
        if (context != null && context.isTrue(RIOT.multilineLiterals))
            this.nodeFmt = new NodeFormatterTTL_MultiLine(baseURI, pmap, NodeToLabel.createScopeByDocument());
        else
            this.nodeFmt = new NodeFormatterTTL(baseURI, pmap, NodeToLabel.createScopeByDocument());
        Symbol s = Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities");
        if (context != null && context.isDefined(s)) {
            SortedMap<String, Integer> nsPriorities = context.get(s);
            Integer nsDefaultPriority = context.get(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"));
            if (nsDefaultPriority == null || nsPriorities == null)
                this.compPredicates = new ComparePredicates();
            else
                this.compPredicates = new ComparePredicates(nsPriorities, nsDefaultPriority);
        } else
            this.compPredicates = new ComparePredicates();

        s = Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities");
        if (context != null && context.isDefined(s))
            this.complexPredicatesPriorities = context.get(s);

        s = Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase");
        this.indent_base = context.getInt(s, 4);
        s = Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth");
        this.predicate_base_width = context.getInt(s, 14);
        s = Symbol.create(STTLWriter.SYMBOLS_NS + "longSubject");
        this.long_subject = context.getInt(s, LONG_SUBJECT);
        s = Symbol.create(STTLWriter.SYMBOLS_NS + "objectsMultiLine");
        this.objects_multi_line = context.isTrue(s);
        s = Symbol.create(STTLWriter.SYMBOLS_NS + "namedDotNewLine");
        this.named_dot_new_line = context.isTrue(s);
    }

    protected void writeBase(final String base) {
        RiotLib.writeBase(out, base, DirectiveStyle.KEYWORD);
    }

    // returns the number of prefixes written
    protected int writePrefixes(final PrefixMap prefixMap) {
        // had to rewrite that to order it properly:
        if (prefixMap == null || prefixMap.isEmpty())
            return 0;
        final Map<String, String> map;
        if (this.onlyWriteUsedPrefixes && prefixMap instanceof CheckedPrefixMap) 
            map = ((CheckedPrefixMap) prefixMap).getUsedMappingCopy();
        else
            map = prefixMap.getMappingCopy();
        final List<String> sortedKeys = new ArrayList<String>(map.keySet());
        Collections.sort(sortedKeys);
        for (String prefix : sortedKeys) {
            out.print("@prefix ");
            out.print(prefix);
            out.print(": ");
            out.pad(PREFIX_IRI);
            out.print("<");
            out.print(map.get(prefix));
            out.print(">");
            out.print(" .");
            out.println();
        }
        return sortedKeys.size();
    }

    /* Write graph in Turtle syntax (or part of TriG) */
    protected void writeGraphTTL(Graph graph) {
        ShellGraph x = new ShellGraph(graph, null, null);
        x.writeGraph();
    }

    /*
     * Write graph in Turtle syntax (or part of TriG). graphName is null for default
     * graph.
     */
    protected void writeGraphTTL(DatasetGraph dsg, Node graphName) {
        Graph g = (graphName == null || Quad.isDefaultGraph(graphName)) ? dsg.getDefaultGraph() : dsg.getGraph(graphName);
        ShellGraph x = new ShellGraph(g, graphName, dsg);
        x.writeGraph();
    }

    // write comes from TurtleWriter.java
    public void write(Graph graph) {
        writeBase(baseURI);
        if (this.onlyWriteUsedPrefixes) {
            final IndentedWriter savedOut = this.out;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final IndentedWriter buffedOut = new IndentedWriter(baos);
            // buffedOut is always at indentation 0 at this point
            this.out = buffedOut;
            writeGraphTTL(graph);
            this.out = savedOut;
            int nbPrefixesWritten = writePrefixes(this.prefixMap);
            if (nbPrefixesWritten > 0 && !graph.isEmpty())
                out.println();
            buffedOut.flush();
            this.out.print(baos.toString());
        } else {
            int nbPrefixesWritten = writePrefixes(this.prefixMap);
            if (nbPrefixesWritten > 0 && !graph.isEmpty())
                out.println();
            writeGraphTTL(graph);
        }
    }

    // Write one graph - using an inner object class to isolate
    // the state variables for writing a single graph.
    private final class ShellGraph {
        // Dataset (for writing graphs indatasets) -- may be null
        private final DatasetGraph dsg;
        private final Collection<Node> graphNames;
        private final Node graphName;
        private final Graph graph;
        private CompareComplex compComplex;

        // Blank nodes that have one incoming triple
        private /* final */ Set<Node> nestedObjects;
        private final Set<Node> nestedObjectsWritten;

        // Blank node subjects that are not referenced as objects or graph names
        // excluding unlnked lists.
        private final Set<Node> freeBnodes;

        // The head node in each well-formed list -> list elements
        private final Map<Node, List<Node>> lists;

        // List that do not have any incoming triples
        private final Map<Node, List<Node>> freeLists;

        // Lists that have more than one incoming triple
        private final Map<Node, List<Node>> nLinkedLists;

        // All nodes that are part of list structures.
        private final Collection<Node> listElts;

        // Allow lists and nest bnode objects.
        // This is true for the main pretty printing then
        // false when we are clearing up unwritten triples.
        private boolean allowDeepPretty = true;

        private ShellGraph(Graph graph, Node graphName, DatasetGraph dsg) {
            this.dsg = dsg;
            this.graphName = graphName;

            this.graphNames = (dsg != null) ? Iter.toSet(dsg.listGraphNodes()) : null;

            this.graph = graph;
            this.nestedObjects = new HashSet<>();
            this.nestedObjectsWritten = new HashSet<>();
            this.freeBnodes = new HashSet<>();

            this.lists = new HashMap<>();
            this.freeLists = new HashMap<>();
            this.nLinkedLists = new HashMap<>();
            this.listElts = new HashSet<>();
            this.allowDeepPretty = true;

            // Must be in this order.
            findLists();
            findBNodesSyntax1();
            // Stop head of lists printed as triples going all the way to the
            // good part.
            nestedObjects.removeAll(listElts);

            if (complexPredicatesPriorities != null)
                this.compComplex = new CompareComplex(compLiterals, complexPredicatesPriorities, graph);
            else
                this.compComplex = new CompareComplex(graph);
        }

        // Debug

        private ShellGraph(Graph graph) {
            this(graph, null, null);
        }

        // ---- Data access
        /** Get all the triples for the graph.find */
        private List<Triple> triples(Node s, Node p, Node o) {
            List<Triple> acc = new ArrayList<>();
            RiotLib.accTriples(acc, graph, s, p, o);
            return acc;
        }

        /** Get exactly one triple or null for none or more than one. */
        private Triple triple1(Node s, Node p, Node o) {
            if (dsg != null)
                return CompareComplex.getOneTriple(dsg, s, p, o);
            else
                return CompareComplex.getOneTriple(graph, s, p, o);
        }

        private long countTriples(Node s, Node p, Node o) {
            if (dsg != null)
                return RiotLib.countTriples(dsg, s, p, o);
            else
                return RiotLib.countTriples(graph, s, p, o);
        }

        private ExtendedIterator<Triple> find(Node s, Node p, Node o) {
            return graph.find(s, p, o);
        }

        /** returns 0,1,2 (where 2 really means "more than 1") */
        private int inLinks(Node obj) {
            if (dsg != null) {
                Iterator<Quad> iter = dsg.find(Node.ANY, Node.ANY, Node.ANY, obj);
                return count012(iter);
            } else {
                ExtendedIterator<Triple> iter = graph.find(Node.ANY, Node.ANY, obj);
                try {
                    return count012(iter);
                } finally {
                    iter.close();
                }
            }
        }

        private int count012(Iterator<?> iter) {
            if (!iter.hasNext())
                return 0;
            iter.next();
            if (!iter.hasNext())
                return 1;
            return 2;
        }

        /** Check whether a node is used only in the graph we're working on */
        private boolean containedInOneGraph(Node node) {
            if (dsg == null)
                // Single graph
                return true;

            if (graphNames.contains(node))
                // Used as a graph name.
                return false;

            Iterator<Quad> iter = dsg.find(Node.ANY, node, Node.ANY, Node.ANY);
            if (!quadsThisGraph(iter))
                return false;

            iter = dsg.find(Node.ANY, Node.ANY, node, Node.ANY);
            if (!quadsThisGraph(iter))
                return false;

            iter = dsg.find(Node.ANY, Node.ANY, Node.ANY, node);
            if (!quadsThisGraph(iter))
                return false;
            return true;
        }

        /**
         * Check whether an iterator of quads is all in the same graph (dataset assumed)
         */
        private boolean quadsThisGraph(Iterator<Quad> iter) {
            if (!iter.hasNext())
                // Empty iterator
                return true;
            Quad q = iter.next();
            Node gn = q.getGraph();

            // Test first quad - both default graph (various forms) or same named graph
            if (isDefaultGraph(gn)) {
                if (!isDefaultGraph(graphName))
                    return false;
            } else {
                if (!Objects.equals(gn, graphName))
                    // Not both same named graph
                    return false;
            }
            // Check rest of iterator.
            for (; iter.hasNext();) {
                Quad q2 = iter.next();
                if (!Objects.equals(gn, q2.getGraph()))
                    return false;
            }
            return true;
        }

        private boolean isDefaultGraph(Node node) {
            return node == null || Quad.isDefaultGraph(node);
        }

        /** Get triples with the same subject */
        Collection<Triple> triplesOfSubject(Node subj) {
            return RiotLib.triplesOfSubject(graph, subj);
        }

        private List<Node> listSubjects() {
            // reimplement in a sorted way:
            ExtendedIterator<Triple> iter = graph.find(Node.ANY, Node.ANY, Node.ANY);
            List<Node> ln = Iter.iter(iter).map(Triple::getSubject).distinct().toList();
            Collections.sort(ln, compLiterals);
            return ln;
        }

        // ---- Data access

        /**
         * Find Bnodes that can written as [] Subject position (top level) - only used
         * for subject position anywhere in the dataset Object position (any level) -
         * only used as object once anywhere in the dataset
         */
        private void findBNodesSyntax1() {
            Set<Node> rejects = new HashSet<>(); // Nodes known not to meet the requirement.

            ExtendedIterator<Triple> iter = find(Node.ANY, Node.ANY, Node.ANY);
            try {
                for (; iter.hasNext();) {
                    Triple t = iter.next();
                    Node subj = t.getSubject();
                    Node obj = t.getObject();

                    if (subj.isBlank()) {
                        int sConn = inLinks(subj);
                        if (sConn == 0 && containedInOneGraph(subj))
                            // Not used as an object in this graph.
                            freeBnodes.add(subj);
                    }

                    if (!obj.isBlank())
                        continue;
                    if (rejects.contains(obj))
                        continue;

                    int connectivity = inLinks(obj);
                    if (connectivity == 1 && containedInOneGraph(obj)) {
                        // If not used in another graph (or as graph name)
                        nestedObjects.add(obj);
                    } else
                        // Uninteresting object connected multiple times.
                        rejects.add(obj);
                }
            } finally {
                iter.close();
            }
        }

        // --- Lists setup
        /*
         * Find all list heads and all nodes in well-formed lists. Return a (list head
         * -> Elements map), list elements)
         */
        private void findLists() {
            List<Triple> tails = triples(Node.ANY, RDF_Rest, RDF_Nil);
            for (Triple t : tails) {
                // Returns the elements, reversed.
                Collection<Node> listElts2 = new HashSet<>();
                Pair<Node, List<Node>> p = followTailToHead(t.getSubject(), listElts2);
                if (p != null) {
                    Node headElt = p.getLeft();
                    // Free standing/private
                    List<Node> elts = p.getRight();
                    long numLinks = countTriples(null, null, headElt);
                    if (numLinks == 1)
                        lists.put(headElt, elts);
                    else if (numLinks == 0)
                        // 0 connected lists
                        freeLists.put(headElt, elts);
                    else
                        // Two triples to this list.
                        nLinkedLists.put(headElt, elts);
                    listElts.addAll(listElts2);
                }
            }
        }

        // return head elt node, list of elements.
        private Pair<Node, List<Node>> followTailToHead(Node lastListElt, Collection<Node> listElts) {
            List<Node> listCells = new ArrayList<>();
            List<Node> eltsReversed = new ArrayList<>();
            List<Triple> acc = new ArrayList<>();
            Node x = lastListElt;

            for (;;) {
                if (!validListElement(x, acc)) {
                    if (listCells.size() == 0)
                        // No earlier valid list.
                        return null;
                    // Fix up to previous valid list cell.
                    x = listCells.remove(listCells.size() - 1);
                    break;
                }

                Triple t = triple1(x, RDF_First, null);
                if (t == null)
                    return null;
                eltsReversed.add(t.getObject());
                listCells.add(x);

                // Try to move up the list.
                List<Triple> acc2 = triples(null, null, x);
                long numRest = countTriples(null, RDF_Rest, x);
                if (numRest != 1) {
                    // Head of well-formed list.
                    // Classified by 0,1,more links later.
                    listCells.add(x);
                    break;
                }
                // numRest == 1
                int numLinks = acc2.size();
                if (numLinks > 1)
                    // Non-list links to x
                    break;
                // Valid.
                Triple tLink = acc2.get(0);
                x = tLink.getSubject();
            }
            // Success.
            listElts.addAll(listCells);
            Collections.reverse(eltsReversed);
            return Pair.create(x, eltsReversed);
        }

        /** Return the triples of the list element, or null if invalid list */
        private boolean validListElement(Node x, List<Triple> acc) {
            Triple t1 = triple1(x, RDF_Rest, null); // Which we came up to get
                                                    // here :-(
            if (t1 == null)
                return false;
            Triple t2 = triple1(x, RDF_First, null);
            if (t2 == null)
                return false;
            long N = countTriples(x, null, null);
            if (N != 2)
                return false;
            acc.add(t1);
            acc.add(t2);
            return true;
        }

        // ----

        private void writeGraph() {
            List<Node> subjects = listSubjects();
            boolean somethingWritten = writeBySubject(subjects);
            // Write remainders
            // 1 - Shared lists
            somethingWritten = writeRemainingNLinkedLists(somethingWritten);

            // 2 - Free standing lists
            somethingWritten = writeRemainingFreeLists(somethingWritten);

            // 3 - Blank nodes that are unwrittern single objects.
            // System.err.println("## ## ##") ;
            // printDetails("nestedObjects", nestedObjects) ;
            // printDetails("nestedObjectsWritten", nestedObjectsWritten) ;
            Set<Node> singleNodes = SetUtils.difference(nestedObjects, nestedObjectsWritten);
            somethingWritten = writeRemainingNestedObjects(singleNodes, somethingWritten);
        }

        private boolean writeRemainingNLinkedLists(boolean somethingWritten) {
            // Print carefully - need a label for the first cell.
            // So we write out the first element of the list in triples, then
            // put the remainer as a pretty list
            for (Node n : nLinkedLists.keySet()) {
                if (somethingWritten)
                    out.println();
                somethingWritten = true;

                List<Node> x = nLinkedLists.get(n);
                writeNode(n);

                write_S_P_Gap();
                out.pad();

                writeNode(RDF_First);
                print(" ");
                writeNode(x.get(0));
                print(" ;");
                println();
                writeNode(RDF_Rest);
                print("  ");
                x = x.subList(1, x.size());
                writeList(x);
                print(" .");
                out.decIndent(indent_base);
                println();
            }
            return somethingWritten;
        }

        // Write free standing lists - ones where the head is not an object of
        // some other triple. Turtle does not allow free standing (... ) .
        // so write as a predicateObjectList for one element.
        private boolean writeRemainingFreeLists(boolean somethingWritten) {
            for (Node n : freeLists.keySet()) {
                if (somethingWritten)
                    out.println();
                somethingWritten = true;

                List<Node> x = freeLists.get(n);
                // Print first element for the [ ... ]
                out.print("[ ");

                writeNode(RDF_First);
                print(" ");
                writeNode(x.get(0));
                print(" ; ");
                writeNode(RDF_Rest);
                print(" ");
                x = x.subList(1, x.size());
                // Print remainder.
                writeList(x);
                out.println(" ] .");
            }
            return somethingWritten;
        }

        // Write any left over nested objects
        // These come from blank node cycles : _:a <p> _:b . _b: <p> _:a .
        // Also from from blank node cycles + tail: _:a <p> _:b . _:a <p> "" . _b: <p>
        // _:a .
        private boolean writeRemainingNestedObjects(Set<Node> objects, boolean somethingWritten) {
            for (Node n : objects) {
                if (somethingWritten)
                    out.println();
                somethingWritten = true;

                Triple t = triple1(null, null, n);
                if (t == null)
                    throw new InternalErrorException("Expected exactly one triple");

                Node subj = t.getSubject();
                boolean b = allowDeepPretty;
                try {
                    allowDeepPretty = false;
                    Collection<Triple> triples = triples(subj, null, null);
                    writeCluster(subj, triples);
                } finally {
                    allowDeepPretty = b;
                }
            }

            return somethingWritten;
        }

        // return true if did write something.
        private boolean writeBySubject(List<Node> subjects) {
            boolean first = true;
            for (Node subj : subjects) {
                if (nestedObjects.contains(subj))
                    continue;
                if (listElts.contains(subj))
                    continue;
                if (!first)
                    out.println();
                first = false;
                if (freeBnodes.contains(subj)) {
                    // Top level: write in "[....]" on "[] :p" form.
                    writeNestedObjectTopLevel(subj);
                    continue;
                }

                Collection<Triple> cluster = triplesOfSubject(subj);
                writeCluster(subj, cluster);
            }
            return !first;
        }

        // A Cluster is a collection of triples with the same subject.
        private void writeCluster(Node subject, Collection<Triple> cluster) {
            if (cluster.isEmpty())
                return;
            writeNode(subject);
            writeClusterPredicateObjectList(indent_base, cluster);
        }

        // Write the PredicateObjectList fora subject already output.
        // The subject may have been a "[]" or a URI - the indentation is passed in.
        private void writeClusterPredicateObjectList(int indent, Collection<Triple> cluster) {
            int lastRow = out.getRow();
            write_S_P_Gap();
            out.incIndent(indent);
            out.pad();
            writePredicateObjectList(cluster);
            out.decIndent(indent);
            if (out.getRow() > lastRow && named_dot_new_line) {
                println();
                print(".");
            } else
                print(" .");
            println();
        }

        // Writing predicate-object lists.
        // We group the cluster by predicate and within each group
        // we print:
        // literals, then simple objects, then pretty objects

        private void writePredicateObjectList(Collection<Triple> cluster) {
            Map<Node, List<Node>> pGroups = groupByPredicates(cluster);
            Collection<Node> predicates = pGroups.keySet();

            // Find longest predicate URI
            // int predicateMaxWidth = RiotLib.calcWidth(prefixMap, baseURI, predicates,
            // MIN_PREDICATE, LONG_PREDICATE) ;

            // see
            // https://github.com/BuddhistDigitalResourceCenter/jena-stable-turtle/issues/1
            int predicateMaxWidth = predicate_base_width;

            boolean first = true;

            if (!OBJECT_LISTS) {
                for (Node p : predicates) {
                    for (Node o : pGroups.get(p)) {
                        writePredicateObject(p, o, predicateMaxWidth, first);
                        first = false;
                    }
                }
                return;
            }

            for (Node p : predicates) {
                List<Node> rdfSimpleNodes = new ArrayList<>();
                // Non-literals, printed (), or []-embedded
                List<Node> rdfComplexNodes = new ArrayList<>();

                for (Node o : pGroups.get(p)) {
                    if (isPrettyNode(o)) {
                        rdfComplexNodes.add(o);
                        continue;
                    }
                    rdfSimpleNodes.add(o);
                }

                if (!rdfSimpleNodes.isEmpty()) {
                    Collections.sort(rdfSimpleNodes, compLiterals);
                    writePredicateObjectList(p, rdfSimpleNodes, predicateMaxWidth, first, false);
                    first = false;
                }

                if (!rdfComplexNodes.isEmpty()) {
                    Collections.sort(rdfComplexNodes, compComplex);
                    writePredicateObjectList(p, rdfComplexNodes, predicateMaxWidth, first, true);
                    first = false;
                }
            }
        }

        private void writePredicateObject(Node p, Node obj, int predicateMaxWidth, boolean first) {
            writePredicate(p, predicateMaxWidth, first, false);
            out.incIndent(indent_base);
            writeNodePretty(obj);
            out.decIndent(indent_base);
        }

        private void writePredicateObjectList(Node p, List<Node> objects, int predicateMaxWidth, boolean first, boolean complex) {
            boolean useMultiLine = objects_multi_line && objects.size() > 1;
            writePredicate(p, predicateMaxWidth, first, useMultiLine);
            out.incIndent(indent_base);

            boolean firstObject = true;
            for (Node o : objects) {
                if (firstObject && useMultiLine)
                    out.pad(indent_base);
                if (!firstObject) {
                    if (out.getCurrentOffset() > 0)
                        out.print(" ,");
                    else
                        // Before the current indent, due to a multiline literal being written raw.
                        // We will pad spaces to indent on output spaces. Don't add a first " "
                        out.print(",");
                    if (useMultiLine || complex) {
                        println();
                        out.pad(indent_base);
                    } else
                        out.print(' ');
                } else
                    firstObject = false;
                if (complex)
                    writeNodePretty(o);
                else
                    writeNode(o);
            }
            out.decIndent(indent_base);
        }

        /** Write a predicate - jump to next line if deemed long */
        private void writePredicate(Node p, int predicateMaxWidth, boolean first, boolean objectNewLine) {
            if (first)
                first = false;
            else {
                print(" ;");
                println();
            }
            int colPredicateStart = out.getAbsoluteIndent();

            if (!prefixMap.containsPrefix(rdfNS) && RDF_type.equals(p))
                print("a");
            else
                writeNode(p);
            int colPredicateFinish = out.getCol();
            int wPredicate = (colPredicateFinish - colPredicateStart);

            if (objectNewLine || wPredicate > LONG_PREDICATE)
                println();
            else {
                out.pad(predicateMaxWidth);
                // out.print(' ', predicateMaxWidth-wPredicate) ;
                gap(GAP_P_O);
            }
        }

        private Map<Node, List<Node>> groupByPredicates(Collection<Triple> cluster) {
            SortedMap<Node, List<Node>> x = new TreeMap<>(compPredicates);
            for (Triple t : cluster) {
                Node p = t.getPredicate();
                if (!x.containsKey(p))
                    x.put(p, new ArrayList<Node>());
                x.get(p).add(t.getObject());
            }

            return x;
        }

        private int countPredicates(Collection<Triple> cluster) {
            Set<Node> x = new HashSet<>();
            for (Triple t : cluster) {
                Node p = t.getPredicate();
                x.add(p);
            }
            return x.size();
        }

        // [ :p "abc" ] . or [] : "abc" .
        private void writeNestedObjectTopLevel(Node subject) {
            writeNestedObject(subject);
            out.println(" .");
        }

        private void writeNestedObject(Node node) {
            Collection<Triple> x = triplesOfSubject(node);

            if (x.isEmpty()) {
                print("[] ");
                return;
            }

            int pCount = countPredicates(x);

            if (pCount == 1) {
                print("[ ");
                out.incIndent(2);
                writePredicateObjectList(x);
                out.decIndent(2);
                print(" ]");
                return;
            }

            // Two or more.
            int indent0 = out.getAbsoluteIndent();
            int here = out.getCol();
            out.setAbsoluteIndent(here);
            print("[ ");
            out.incIndent(2);
            writePredicateObjectList(x);
            out.decIndent(2);
            println(); // Newline for "]"
            print("]");
            out.setAbsoluteIndent(indent0);
        }

        // Write a list
        private void writeList(List<Node> elts) {
            if (elts.size() == 0) {
                out.print("()");
                return;
            }

            // "fresh line mode" means printed one on new line
            // Multi line items are ones that can be multiple lines. Non-literals.
            // Was the previous row a multiLine?
            boolean lastItemFreshLine = false;
            // Have there been any items that causes "fresh line" mode?
            boolean multiLineAny = false;
            boolean first = true;

            // Where we started.
            int originalIndent = out.getAbsoluteIndent();
            // Rebase indent here.
            int x = out.getCol();
            out.setAbsoluteIndent(x);

            out.print("(");
            out.incIndent(2);
            for (Node n : elts) {

                // Print this item on a fresh line? (still to check: first line)
                boolean thisItemFreshLine = /* multiLineAny | */ n.isBlank();

                // Special case List in List.
                // Start on this line if last item was on this line.
                if (lists.containsKey(n))
                    thisItemFreshLine = lastItemFreshLine;

                // Starting point.
                if (!first) {
                    if (lastItemFreshLine | thisItemFreshLine)
                        out.println();
                    else
                        out.print(" ");
                }

                first = false;
                // Literals with newlines: int x1 = out.getRow() ;
                // Adds indent_base even for a [ one triple ]
                // Special case [ one triple ]??
                writeNodePretty(n);
                // Literals with newlines:int x2 = out.getRow() ;
                // Literals with newlines: boolean multiLineAnyway = ( x1 != x2 ) ;
                lastItemFreshLine = thisItemFreshLine;
                multiLineAny = multiLineAny | thisItemFreshLine;

            }
            if (multiLineAny)
                out.println();
            else
                out.print(" ");
            out.decIndent(2);
            out.setAbsoluteIndent(x);
            out.print(")");
            out.setAbsoluteIndent(originalIndent);
        }

        private boolean isPrettyNode(Node n) {
            // Order matters? - one connected objects may include list elements.
            if (allowDeepPretty) {
                if (lists.containsKey(n))
                    return true;
                if (nestedObjects.contains(n))
                    return true;
            }
            if (RDF_Nil.equals(n))
                return true;
            return false;
        }

        // --> write S or O??
        private void writeNodePretty(Node obj) {
            // Assumes "isPrettyNode" is true.
            // Order matters? - one connected objects may include list elements.
            if (lists.containsKey(obj))
                writeList(lists.get(obj));
            else if (nestedObjects.contains(obj))
                writeNestedObject(obj);
            else if (RDF_Nil.equals(obj))
                out.print("()");
            else
                writeNode(obj);
            if (nestedObjects.contains(obj))
                nestedObjectsWritten.add(obj);

        }

        private void write_S_P_Gap() {
            if (out.getCol() > long_subject)
                out.println();
            else
                gap(GAP_S_P);
        }
    }

    protected final void writeNode(Node node) {
        nodeFmt.format(out, node);
    }

    private void print(String x) {
        out.print(x);
    }

    private void gap(int gap) {
        out.print(' ', gap);
    }

    // flush aggressively (debugging)
    private void println() {
        out.println();
        // out.flush() ;
    }

}
