package helper.nlp;

import helper.annotations.DomainSpecific;
import helper.formatting.textannotation.AnnotationBuilder;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.GrammaticalRelation.Language;
import requirement.metadata.TextAnnotator;

/**
 * Represents some NLP work to be done in the future;
 * essentially a special "runnable" which takes an argument
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class NLPJob {
    private static final GrammaticalRelation GR_SUBJECT = GrammaticalRelation.valueOf(Language.English, "nsubj");
    private static final GrammaticalRelation GR_SUBJECTPASSIVE = GrammaticalRelation.valueOf(Language.English, "nsubjpass");
    private static final GrammaticalRelation GR_NOUN = GrammaticalRelation.valueOf(Language.English, "nn");
    private static final GrammaticalRelation GR_DEPENDENT = GrammaticalRelation.valueOf(Language.English, "dep");
    private static final Set<GrammaticalRelation> GR_SUBJECTS;
    private static final Pattern VERB_ADJECTIVE_TAG_PATTERN = Pattern.compile("(?:(?<verb>VB[DGNPZ]?)|(?<adjective>JJ[RS]?))");
    private static final Collection<GrammaticalRelation> INVALID_CHILDREN_OF_SUBJECT;

    static {
	INVALID_CHILDREN_OF_SUBJECT = new ArrayList<>();
	INVALID_CHILDREN_OF_SUBJECT.add(GR_SUBJECT);
	INVALID_CHILDREN_OF_SUBJECT.add(GR_DEPENDENT);
	INVALID_CHILDREN_OF_SUBJECT.add(GrammaticalRelation.valueOf(Language.English, "rcmod"));	
	INVALID_CHILDREN_OF_SUBJECT.add(GrammaticalRelation.valueOf(Language.English, "cop"));
	INVALID_CHILDREN_OF_SUBJECT.add(GrammaticalRelation.valueOf(Language.English, "conj"));
	INVALID_CHILDREN_OF_SUBJECT.add(GrammaticalRelation.valueOf(Language.English, "cc"));

	GR_SUBJECTS = new HashSet<>(2);
	GR_SUBJECTS.add(GR_SUBJECT);
	GR_SUBJECTS.add(GR_SUBJECTPASSIVE);
    }

    private final String textToAnnotate;
    private final TextAnnotator outputAnnotator;

    /**
     * Ordinary constructor
     * 
     * @param textToAnnotate text which shall be annotated
     * @param outputAnnotator annotator where the output (annotations) shall end up
     * @throws IllegalArgumentException if any of the arguments are {@code null}
     */
    public NLPJob(final String textToAnnotate, final TextAnnotator outputAnnotator) {
	if (textToAnnotate == null) throw new IllegalArgumentException("textToAnnotate cannot be null.");
	if (outputAnnotator == null) throw new IllegalArgumentException("outputAnnotator cannot be null.");	
	this.textToAnnotate = textToAnnotate;
	this.outputAnnotator = outputAnnotator;	
    }

    /**
     * Poison pill constructor; will create an object which causes the consuming thread to shut down
     */
    NLPJob() {
	this.textToAnnotate = null;
	this.outputAnnotator = null;
    }      

    /**
     * Perform actual NLP work; this is expensive (runtime- and memory-wise)
     * 
     * @param lexicalizedParser parser to use for this job
     * @return {@code true} if parse was successful; {@code false} if the consumer should shut down (we are a poison pill)
     */
    boolean process(final LexicalizedParser lexicalizedParser) {
	assert lexicalizedParser != null;
	final boolean output;
	if (this.textToAnnotate == null) {
	    output = false;
	}
	else {
	    for (final List<HasWord> sentence : new DocumentPreprocessor(new StringReader(this.textToAnnotate))) {
		final Tree parse = lexicalizedParser.apply(sentence);    	    
		final SemanticGraph semanticGraph = SemanticGraphFactory.makeFromTree(parse, false);

		processRoot(semanticGraph);
	    }
	    output = true;
	}
	return output;
    }

    /**
     * @param semanticGraph semanticGraph to work on    
     */
    private void processRoot(final SemanticGraph semanticGraph) {
	assert semanticGraph != null;
	IndexedWord root = null;
	try {
	    root = semanticGraph.getFirstRoot();
	}
	catch (RuntimeException e) {
	    // no root found
	    return;
	}		

	if (root.tag().matches("NN(?:P|S|PS)?") && !isNote(root)) {
	    // Strategy 1: root is noun; annotate self + any child nounds as headphrase	    
	    final TreeSet<IndexedWord> headphraseWords = new TreeSet<>(semanticGraph.getChildrenWithReln(root, GR_NOUN)); // make sure to use a TreeSet; they need to be ordered
	    headphraseWords.add(root);
	    combineHeadphrase(headphraseWords);
	}
	else {
	    // Strategy 2: root is verb/adjective; annotate root as predicate/sentence_root and children as headphrase
	    if (!annotateRootAsVerbOrAdjective(root)) {
		// we will get here for sentences like "Note: This is the real sentence." (where "Note:" is the actual root)
		final Set<IndexedWord> rootChildren = semanticGraph.getChildren(root);
		for (final IndexedWord child: rootChildren) {
		    if (annotateRootAsVerbOrAdjective(child)) {
			root = child; break;
		    }
		}
	    }			
	    processHeadphraseFromVerbOrAdjective(semanticGraph, root);
	}
    }

    /**
     * Check if this root candidate can be regarded a legitimate root; if so annotate it accordingly
     * 
     * @param root presumed root
     * @return {@true} if annotation was successful; {@code false} otherwise
     */
    @DomainSpecific
    private boolean annotateRootAsVerbOrAdjective(final IndexedWord root) {
	assert root != null;
	final boolean output;
	final Matcher matcher = VERB_ADJECTIVE_TAG_PATTERN.matcher(root.tag());	
	if (matcher.matches()) {
	    if (matcher.group("verb") != null) {
		if (isNote(root)) {
		    output = false; // "Note" appears often in the beginning of texts; but then it is never an adjective
		}
		else {
		    this.outputAnnotator.addAnnotation(root.beginPosition(), root.endPosition(), AnnotationBuilder.SENTENCE_ROOT_VERB.getAnnotator());
		    output = true;
		}
	    }
	    else if (matcher.group("adjective") != null) {
		this.outputAnnotator.addAnnotation(root.beginPosition(), root.endPosition(), AnnotationBuilder.SENTENCE_ROOT_ADJECTIVE.getAnnotator());
		output = true;
	    }
	    else output = false;
	}
	else output = false;
	return output;
    }

    private void processHeadphraseFromVerbOrAdjective(final SemanticGraph semanticGraph, final IndexedWord root) {
	assert semanticGraph != null && root != null;

	final Set<IndexedWord> subjects = semanticGraph.getChildrenWithRelns(root, GR_SUBJECTS);
	final TreeSet<IndexedWord> validChildren = new TreeSet<>();
	for (final IndexedWord subject : subjects) {
	    final List<IndexedWord> children = semanticGraph.getChildList(subject);

	    for (final IndexedWord child: children) {
		if (INVALID_CHILDREN_OF_SUBJECT.contains(semanticGraph.getEdge(subject, child).getRelation())) continue; //we do not care
		validChildren.add(child);
		getAllChildrenBelow(semanticGraph, child, validChildren);
	    }
	    validChildren.add(subject);
	}

	combineHeadphrase(validChildren);	
    }

    private void getAllChildrenBelow(final SemanticGraph semanticGraph, final IndexedWord node, final TreeSet<IndexedWord> collector) {
	assert semanticGraph != null && node != null && collector != null;	
	final TreeSet<IndexedWord> children = new TreeSet<>(semanticGraph.getChildren(node)); // make sure to copy these
	//collector.addAll(children);
	for (final IndexedWord child : children) {
	    if (semanticGraph.getEdge(node, child).getRelation().equals(GR_DEPENDENT)) continue; // no dependents, please (usually yields embraced stuff)
	    collector.add(child);
	    getAllChildrenBelow(semanticGraph, child, collector);
	}	
    }

    /**
     * @param headphraseWords ordered set of headphraseWords to process
     */
    private void combineHeadphrase(TreeSet<IndexedWord> headphraseWords) {
	// write the annotations; and combine if we found consecutive words; this is based on the assumption that words do not overlap
	final Iterator<IndexedWord> iterator = headphraseWords.iterator();	
	if (iterator.hasNext()) {
	    IndexedWord currentWord = iterator.next();
	    int annotatorBeginOffset = currentWord.beginPosition();
	    int annotatorEndOffset = currentWord.endPosition();
	    while(iterator.hasNext()) {
		final IndexedWord nextWord = iterator.next();
		final String charsInBetween = this.textToAnnotate.substring(currentWord.endPosition(), nextWord.beginPosition());
		if (charsInBetween.matches("\\s+")) {
		    // we found consecutive words; expand the annotator pattern
		    annotatorEndOffset = nextWord.endPosition();
		}
		else {
		    // non consecutive words; annotate what we had and start a new annotation group
		    this.outputAnnotator.addAnnotation(annotatorBeginOffset, annotatorEndOffset, AnnotationBuilder.HEADPHRASE.getAnnotator());
		    annotatorBeginOffset = nextWord.beginPosition();
		    annotatorEndOffset = nextWord.endPosition();
		}		
		currentWord = nextWord;
	    }
	    // process very last group
	    this.outputAnnotator.addAnnotation(annotatorBeginOffset, annotatorEndOffset, AnnotationBuilder.HEADPHRASE.getAnnotator());
	}
    }
    
    /**
     * Special handling for "Note" (which can appear in different grammatical contexts as this can be both a verb and a noun)
     * 
     * @param input word to check
     * @return {@code true} if the word represents a leading "Note" prefix
     */
    private static boolean isNote(final IndexedWord input) {
	// we should be safe with the rigorous beginPosition; a different one will decrease the likelihood of flagging this as a verb in the first place
	return input.word().equals("Note") && input.beginPosition() == 0;
    }
}
