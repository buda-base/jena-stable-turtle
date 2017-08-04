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

TODO

## Use

TODO

## Change log

## License

All the code on this repository is under the [Apache 2.0 License](LICENSE). 

The original parts are `Copyright © 2017 Buddhist Digital Resource Center`, and the file `TurtleShell.java` (coming from the [Jena repository](https://github.com/apache/jena/blob/master/jena-arq/src/main/java/org/apache/jena/riot/writer/TurtleShell.java)) is `Copyright © 2011-2017 Apache Software Foundation (ASF)`, see [NOTICE](https://github.com/apache/jena/blob/master/NOTICE) for more information.
