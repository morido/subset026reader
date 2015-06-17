Tool
====

This contains the tool to create reqif-files of the ETCS Subset-026 from Microsoft Word files.

* `bin/` - contains a binary version of the tool without support for NLP
* `doc/` - holds the respective Javadoc documentation
* `src/` - contains the actual Java sources of the tool

---

*Note:* Necessary libraries are not included and there is currently no build script, either. So this is how to do it manually:

1. Get the source distribution of Apache POI. The tool has been successfully tested with Version *3.10 FINAL* obtainable from [here](http://archive.apache.org/dist/poi/release/src/poi-src-3.10-FINAL-20140208.tar.gz).
2. Apply `poi-3.10.patch`
3. Build POI.
4. *(optionally)* get the the Stanford parser available through [this](http://nlp.stanford.edu/software/stanford-parser-full-2015-01-29.zip) link. Version 3.5.1 has been tested successfully.

In order to run the various tests [Powermock](https://code.google.com/p/powermock/) is also necessary. Version 1.6.1 with the jUnit and Mockito dependencies has been successfully tested. Here is a [direct link](http://dl.bintray.com/johanhaleby/generic/powermock-mockito-junit-1.6.1.zip) to the required files.
