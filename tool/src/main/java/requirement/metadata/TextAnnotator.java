package requirement.metadata;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import helper.XmlStringWriter;
import helper.formatting.textannotation.Annotator;

/**
 * Manage annotated text; that is raw text which has certain words / phrases highlighted (and thus becomes XHTML)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public class TextAnnotator {    
    private static class AnnotationRun {
	private final int startOffset;
	private final int endOffset;
	private final Annotator annotator;

	AnnotationRun(final int startOffset, final int endOffset, final Annotator annotator) {
	    this.startOffset = startOffset;
	    this.endOffset = endOffset;
	    this.annotator = annotator;
	}
    }        

    private final Comparator<AnnotationRun> startOffsetComparator = new Comparator<AnnotationRun>() {

	@Override
	public int compare(final AnnotationRun o1, final AnnotationRun o2) {
	    // order by startOffset, if equal sort by length (ascending)
	    final int output;
	    if (o1.startOffset != o2.startOffset) {
		output = o1.startOffset - o2.startOffset;
	    }
	    else {
		// make sure to never return 0 as this would imply equality for the SortedSet
		final int endOffsetDifference = o1.endOffset - o2.endOffset;
		output = endOffsetDifference != 0 ? endOffsetDifference : -1;		
	    }
	    return output;
	}	
    };

    private final String textToAnnotate;
    private final SortedSet<AnnotationRun> annotations = new TreeSet<>(this.startOffsetComparator); // does not contain any equal objects


    /**
     * Ordinary constructor
     * 
     * @param textToAnnotate raw string which shall become annotated
     */
    protected TextAnnotator(final String textToAnnotate) {
	if (textToAnnotate == null) throw new IllegalArgumentException("textToAnnotate cannot be null.");
	this.textToAnnotate = textToAnnotate;	
    }

    /**
     * Add a new annotation
     * 
     * @param startOffset character offset at which this annotation begins (0-based), inclusive
     * @param endOffset character offset at which this annotation ends (0-based), exclusive
     * @param annotator Annotator to use for this annotation (i.e. the type of this annotation)
     */
    public synchronized void addAnnotation(final int startOffset, final int endOffset, final Annotator annotator) {
	if (annotator == null) throw new IllegalArgumentException("annotator cannot be null.");
	if (startOffset < 0 || startOffset > this.textToAnnotate.length()) throw new IllegalArgumentException("startOffset out of range.");
	if (endOffset <= startOffset || endOffset > this.textToAnnotate.length())
	    throw new IllegalArgumentException("endOffset out of range." + "| startoffset:  " + Integer.toString(startOffset) + "| endOffset: " + Integer.toString(endOffset) + "| Text: " + this.textToAnnotate);
	this.annotations.add(new AnnotationRun(startOffset, endOffset, annotator));
    }   
    
    /**
   * @return a XHTML-formatted string containing all the annotations; never {@code null}
   */
    public String getAnnotatedText() {
	final XmlStringWriter xmlwriter = new XmlStringWriter();
	
	findElementaryIntervals(0, this.textToAnnotate.length(), this.annotations, xmlwriter);
	return xmlwriter.toString();
    }

    /**
     * Count the number of annotations managed by this class
     * 
     * @param filters Array of annotations; if set only annotations of this type will be counted
     * @return the number of annotations which are currently being managed
     */
    public int getNumberOfAnnotations(final Annotator... filters) {
	final int output;
	if (filters.length == 0) {
	    output = this.annotations.size();
	}
	else {
	    int counter = 0;
	    for (final AnnotationRun annotation: this.annotations) {
		for (final Annotator filter : filters) {
		    if (filter == annotation.annotator) counter++;
		}
	    }
	    output = counter;
	}
	return output;
    }    

    /**
     * Finds groups of overlapping annotations and processes each group individually
     * 
     * @param startOffset startOffset from where to process the underlying string; 0-based; inclusive
     * @param endOffset endOffset until where to process the underlying string; 0-based; exclusive
     * @param applicableAnnotations annotations to process
     * @param outputWriter writer where the text and rendered annotations shall end up
     */
    private void findElementaryIntervals(final int startOffset, final int endOffset, final SortedSet<AnnotationRun> applicableAnnotations, final XmlStringWriter outputWriter) {
	final Iterator<AnnotationRun> iterator = applicableAnnotations.iterator();
		
	if (iterator.hasNext()) {
	    AnnotationRun current = iterator.next();
	    AnnotationRun nestingStartRun = current; // first element in the set
	    if (current.startOffset > startOffset) {
		// write everything before first annotation
		outputWriter.writeCharacters(this.textToAnnotate.substring(startOffset, current.startOffset));		
	    }
	    int furthestRightEndpointSeen = current.endOffset;

	    // process all nested groups except last
	    while (iterator.hasNext()) {
		final AnnotationRun next = iterator.next();
		if (furthestRightEndpointSeen < next.startOffset) {
		    // end of nesting group; capture this group		
		    final SortedSet<AnnotationRun> nestedAnnotations = applicableAnnotations.subSet(nestingStartRun, next);
		    splitNestedStructures(nestingStartRun.startOffset, next.startOffset, nestedAnnotations, outputWriter);
		    nestingStartRun = next; // for next nesting group
		}
		current = next;
		furthestRightEndpointSeen = current.endOffset > furthestRightEndpointSeen ? current.endOffset : furthestRightEndpointSeen;
	    }
	    // process very last group
	    final SortedSet<AnnotationRun> remainingAnnotations = applicableAnnotations.tailSet(nestingStartRun);
	    splitNestedStructures(nestingStartRun.startOffset, endOffset, remainingAnnotations, outputWriter);
	}
	else {
	    // no annotations to write; simply output underlying text
	    outputWriter.writeCharacters(this.textToAnnotate.substring(startOffset, endOffset));	    
	}
    }

    /**
     * Splits overlapping annotations
     * 
     * @param startOffset startOffset from where to process the underlying string; 0-based; inclusive
     * @param endOffset endOffset until where to process the underlying string; 0-based; exclusive
     * @param nestedAnnotations annotations to process
     * @param outputWriter writer where the text and rendered annotations shall end up
     * @throws IllegalStateException if the nestedAnnotations were empty
     */
    private void splitNestedStructures(final int startOffset, final int endOffset, final SortedSet<AnnotationRun> nestedAnnotations, final XmlStringWriter outputWriter) {
	assert nestedAnnotations != null && nestedAnnotations.size() > 0; // the latter will cause an IllegalStateException; see below
	
	final TreeSet<AnnotationRun> sortedAnnotations = new TreeSet<>(new Comparator<AnnotationRun>() {
	    @Override
	    public int compare(final AnnotationRun o1, final AnnotationRun o2) {
		// sort by length; descending
		final int lengthComparison = (o2.endOffset - o2.startOffset) - (o1.endOffset - o1.startOffset);
		// make sure never to return 0 since SortedSet expects the elements to be equal in this case (and there are never equal elements in here)
		return lengthComparison != 0 ? lengthComparison : -1;		
	    }
	});
	sortedAnnotations.addAll(nestedAnnotations);

	final Iterator<AnnotationRun> iterator = sortedAnnotations.iterator();
	if (iterator.hasNext()) {
	    final AnnotationRun longest = iterator.next();

	    final TreeSet<AnnotationRun> leftSubTree = new TreeSet<>(this.startOffsetComparator);
	    final TreeSet<AnnotationRun> embeddedSubTree = new TreeSet<>(this.startOffsetComparator);
	    final TreeSet<AnnotationRun> rightSubTree = new TreeSet<>(this.startOffsetComparator);
	    while (iterator.hasNext()) {
		final AnnotationRun comparisonRun = iterator.next();
		if (comparisonRun.startOffset < longest.startOffset) {
		    if (comparisonRun.endOffset <= longest.startOffset) {
			// fully left of longest
			leftSubTree.add(comparisonRun);
		    }
		    else {
			// overlaps from left
			final AnnotationRun fullyLeft = new AnnotationRun(comparisonRun.startOffset, longest.startOffset, comparisonRun.annotator);
			final AnnotationRun fullyEmbedded = new AnnotationRun(longest.startOffset, comparisonRun.endOffset, comparisonRun.annotator);
			leftSubTree.add(fullyLeft);
			embeddedSubTree.add(fullyEmbedded);
		    }
		}
		else {
		    if (comparisonRun.endOffset <= longest.endOffset) {
			// fully embedded
			embeddedSubTree.add(comparisonRun);
		    }
		    else {
			if (comparisonRun.startOffset >= longest.endOffset) {
			    // fully right of longest
			    rightSubTree.add(comparisonRun);
			}
			else {
			    // overlaps to right
			    final AnnotationRun fullyRight = new AnnotationRun(longest.endOffset, comparisonRun.endOffset, comparisonRun.annotator);
			    final AnnotationRun fullyEmbedded = new AnnotationRun(comparisonRun.startOffset, longest.endOffset, comparisonRun.annotator);
			    rightSubTree.add(fullyRight);
			    embeddedSubTree.add(fullyEmbedded);
			}
		    }
		}
	    }
	    findElementaryIntervals(startOffset, longest.startOffset, leftSubTree, outputWriter);
	    longest.annotator.writeStart(outputWriter);	    
	    findElementaryIntervals(longest.startOffset, longest.endOffset, embeddedSubTree, outputWriter);
	    longest.annotator.writeEnd(outputWriter);	    
	    findElementaryIntervals(longest.endOffset, endOffset, rightSubTree, outputWriter);
	}
	else throw new IllegalStateException("Logical error while serializing annotations."); // we do not tolerate empty input sets
    }
}
