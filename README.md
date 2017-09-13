# Jena Stable Turtle output plugin

This repository contains code to define a new [RDF Writer](https://jena.apache.org/documentation/io/rdf-output.html) for [Jena](https://jena.apache.org/) which is turtle always sorted in the same way. It has been developped to reduce the diff noise when the data is stored on a git repository, we are confident there are plenty of other use cases where it will be useful.

### Changes from the stock turtle output

### Sorting some particular cases

There is always some arbitrary decisions to be taken for some cases. We took the following when sorting objects:
- first URIs (sorted) then literals (sorted) then blank nodes
- first `rdf:langString`s then `xsd:string`s then numbers then everything else, sorted by type uri then value
- `rdf:langString`s are sorted by lang then value, in the root unicode collator (not in the locale corresponding to the language)
- numbers are sorted first by value then by type uri (`"+1"^^xsd:integer` < `"1"^^xsd:integer` < `"+1"^^xsd:nonNegativeInteger` < `"1.2"^^xsd:float` < `"2"^^xsd:integer`)

## Installation

Using maven:

```xml
    <dependency>
      <groupId>io.bdrc-</groupId>
      <artifactId>jena-stable-turtle</artifactId>
      <version>0.5.0</version>
    </dependency>
```

## Use

```java
// register the STTL writer
Lang sttl = STTLWriter.registerWriter();
// build a map of namespace priorities
SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
nsPrio.put(SKOS.getURI(), 1);
nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
// build a list of predicates URIs to be used (in order) for blank node comparison
List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
// pass the values through a Context object
Context ctx = new Context();
ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
// the base indentation, defaults to 4
ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsBaseIndent"), 4);
// the minimal predicate width, defaults to 14
ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 14);
Graph g = ... ; // fetch the graph you want to write
RDFWriter w = RDFWriter.create().source().context(ctx).lang(sttl).build();
w.output( ... ); // write somewhere

```

## Change log

See [CHANGELOG.md](CHANGELOG.md).

## License

All the code on this repository is under the [Apache 2.0 License](LICENSE). 

The original parts are `Copyright © 2017 Buddhist Digital Resource Center`, and the file `TurtleShell.java` (coming from the [Jena repository](https://github.com/apache/jena/blob/master/jena-arq/src/main/java/org/apache/jena/riot/writer/TurtleShell.java)) is `Copyright © 2011-2017 Apache Software Foundation (ASF)`, see [NOTICE](https://github.com/apache/jena/blob/master/NOTICE) for more information.
