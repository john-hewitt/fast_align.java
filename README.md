fast_align
==========

`fast_align` is a simple, fast, unsupervised word aligner.

If you use this software, please cite:
* [Chris Dyer](http://www.cs.cmu.edu/~cdyer), [Victor Chahuneau](http://victor.chahuneau.fr), and [Noah A. Smith](http://www.cs.cmu.edu/~nasmith). (2013). [A Simple, Fast, and Effective Reparameterization of IBM Model 2](http://www.ark.cs.cmu.edu/cdyer/fast_valign.pdf). In *Proc. of NAACL*.

The source code in this repository is provided under the terms of the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

 - Note that until we remove the GNU `getopt` depenency, the package is not compliant with the Apache license.

# Input format

Input to `fast_align` must be tokenized and aligned into parallel sentences. Each line is a source language sentence and its target language translation, separated by a triple pipe symbol (`|||`). An example is as follows.

    doch jetzt ist der Held gefallen . ||| but now the hero has fallen .
    neue Modelle werden erprobt . ||| new models are being tested .
    doch fehlen uns neue Ressourcen . ||| but we lack new resources .

# Compiling and using `fast_align`

This Java port of `fast_align` requires Apache Maven ; it can most easily be compiled using Maven by typing `mvn package` at the command line prompt.

Run `java -jar fast_align-1.0-SNAPSHOT.jar` to see a list of command line options. Here is an example invocation:

    java -jar target/fast_align-1.0-SNAPSHOT.jar -i text.fr-en -d -o -v > forward.align

# Authorship

 - Original C implementation made by [clab](https://github.com/clab)
 - Direct port to Java made by [Lane Schwartz](https://github.com/dowobeha/fast_align.java)
 - Further modifications and maintenance by John Hewitt

