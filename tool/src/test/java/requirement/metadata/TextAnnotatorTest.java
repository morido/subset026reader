package requirement.metadata;

import org.junit.Test;

import static org.junit.Assert.*;
import requirement.metadata.TextAnnotator;
import helper.XmlStringWriter;
import helper.formatting.textannotation.AnnotationBuilder;
import helper.formatting.textannotation.Annotator;

/**
 * Test various cases of annotated texts
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
@SuppressWarnings("static-method")
public class TextAnnotatorTest {
    
    /**
     * Test for {@link requirement.metadata.TextAnnotator#getAnnotatedText()}
     */
    @Test
    public void getAnnotatedTextTest() {		
	final Annotator nullAnnotator = new Annotator() {	    
	    @Override
	    public void writeStart(XmlStringWriter xmlwriter) {
		// intentionally do nothing		
	    }
	    
	    @Override
	    public void writeEnd(XmlStringWriter xmlwriter) {
		// intentionally do nothing		
	    }
	};
		
	final String textToAnnotate = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	{
	    // Test 1: No annotations at all	    
	    final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);
	    final String output = textAnnotator.getAnnotatedText();	
	    assertEquals(textToAnnotate, output);
	}
	{
	    // Test 2: Lots of overlapping annotations	    
	    final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);
	    textAnnotator.addAnnotation(5, 28, nullAnnotator);
	    textAnnotator.addAnnotation(8, 15, nullAnnotator);
	    textAnnotator.addAnnotation(8, 9, nullAnnotator);
	    textAnnotator.addAnnotation(18, 39, nullAnnotator);
	    textAnnotator.addAnnotation(23, 46, nullAnnotator);
	    textAnnotator.addAnnotation(25, 30, nullAnnotator);
	    textAnnotator.addAnnotation(44, 47, nullAnnotator);

	    final String output = textAnnotator.getAnnotatedText();	
	    assertEquals(textToAnnotate, output);
	}
	{
	    // Test 3: Lots of overlapping annotations with formatting	    
	    final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);
	    textAnnotator.addAnnotation(5, 28, AnnotationBuilder.WEAKWORD.getAnnotator());
	    textAnnotator.addAnnotation(8, 15, AnnotationBuilder.DEFINITION_TERM.getAnnotator());
	    textAnnotator.addAnnotation(8, 9, AnnotationBuilder.DEFINITION_DOMAIN.getAnnotator());
	    textAnnotator.addAnnotation(18, 39, AnnotationBuilder.WEAKWORD.getAnnotator());
	    textAnnotator.addAnnotation(23, 46, AnnotationBuilder.SENTENCE_ROOT_VERB.getAnnotator());
	    textAnnotator.addAnnotation(25, 30, AnnotationBuilder.LEGALOBLIGATION.getAnnotator());
	    textAnnotator.addAnnotation(44, 47, AnnotationBuilder.DEFINITION_EXPLANATION.getAnnotator());

	    final String output = textAnnotator.getAnnotatedText();	    
	    final String expectedOutput = "abcde<span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">fgh<span class=\"Term\" style=\"border:1px solid #00CC00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\"><span class=\"Domain\" style=\"border:1px solid #006600; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">i</span><span class=\"Domain_annotation\" style=\"background-color:#006600; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[DOMAIN]</span></span>jklmno</span><span class=\"Term_annotation\" style=\"background-color:#00CC00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[TERM]</span></span>pqr<span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">stuv</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span></span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span>w<span class=\"Predicate\" style=\"border-bottom:1px solid black; display:inline-block;\"><span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\"><span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">x</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span>y<span class=\"LegalObligation\" style=\"background-color:#D3D3D3; font-weight:bold; padding-left:0.1em; padding-right:0.1em;\"><span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">zAB</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span>CD</span>EFGHIJKLM</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span>NOPQR<span class=\"Explanation\" style=\"border:1px solid #008000; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">ST</span><span class=\"Explanation_annotation\" style=\"background-color:#008000; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[EXPLANATION]</span></span></span><span class=\"Explanation\" style=\"border:1px solid #008000; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">U</span><span class=\"Explanation_annotation\" style=\"background-color:#008000; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[EXPLANATION]</span></span>VWXYZ";
	    assertEquals(expectedOutput, output);
	}
    }
    
    /**
     * Test the generated annotated text for a typical definition
     */    
    @Test
    public void testDefinition() {
	final String textToAnnotate = "The fruit of an apple tree is edible.";	
	final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);	
	textAnnotator.addAnnotation(27, 36, AnnotationBuilder.DEFINITION_EXPLANATION.getAnnotator());
	textAnnotator.addAnnotation(4, 9, AnnotationBuilder.DEFINITION_TERM.getAnnotator());
	textAnnotator.addAnnotation(16, 26, AnnotationBuilder.DEFINITION_DOMAIN.getAnnotator());
	
	final String output = textAnnotator.getAnnotatedText();
	assertEquals("The <span class=\"Term\" style=\"border:1px solid #00CC00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">fruit</span><span class=\"Term_annotation\" style=\"background-color:#00CC00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[TERM]</span></span> of an <span class=\"Domain\" style=\"border:1px solid #006600; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">apple tree</span><span class=\"Domain_annotation\" style=\"background-color:#006600; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[DOMAIN]</span></span> <span class=\"Explanation\" style=\"border:1px solid #008000; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">is edible</span><span class=\"Explanation_annotation\" style=\"background-color:#008000; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[EXPLANATION]</span></span>.", output);	
    }
    
    /**
     * Test the generated annotated text for a string with lots of weak words
     */
    @Test
    public void testWeakWord() {
	final String textToAnnotate = "Temporarily out of order. At least 9 of the other possibly damaged elevators share the same destiny. Some may work, though.";	
	final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);	
	textAnnotator.addAnnotation(0, 11, AnnotationBuilder.WEAKWORD.getAnnotator());
	textAnnotator.addAnnotation(26, 34, AnnotationBuilder.WEAKWORD.getAnnotator());
	textAnnotator.addAnnotation(50, 58, AnnotationBuilder.WEAKWORD.getAnnotator());
	textAnnotator.addAnnotation(101, 105, AnnotationBuilder.WEAKWORD.getAnnotator());
	textAnnotator.addAnnotation(106, 109, AnnotationBuilder.LEGALOBLIGATION.getAnnotator());
	
	final String output = textAnnotator.getAnnotatedText();	
	assertEquals("<span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">Temporarily</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span> out of order. <span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">At least</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span> 9 of the other <span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">possibly</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span> damaged elevators share the same destiny. <span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">Some</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span> <span class=\"LegalObligation\" style=\"background-color:#D3D3D3; font-weight:bold; padding-left:0.1em; padding-right:0.1em;\">may</span> work, though.", output);	
    }
    
    /**
     * Test all sorts of annotations
     */
    @Test
    public void combinedTest() {
	final String textToAnnotate = "The fruit of a tree is sometimes edible. It shall be picked.";
	final TextAnnotator textAnnotator = new TextAnnotator(textToAnnotate);
	textAnnotator.addAnnotation(33, 39, AnnotationBuilder.DEFINITION_EXPLANATION.getAnnotator());
	textAnnotator.addAnnotation(4, 9, AnnotationBuilder.DEFINITION_TERM.getAnnotator());
	textAnnotator.addAnnotation(15, 19, AnnotationBuilder.DEFINITION_DOMAIN.getAnnotator());
	textAnnotator.addAnnotation(23, 32, AnnotationBuilder.WEAKWORD.getAnnotator());
	textAnnotator.addAnnotation(44, 49, AnnotationBuilder.LEGALOBLIGATION.getAnnotator());
	textAnnotator.addAnnotation(53, 59, AnnotationBuilder.SENTENCE_ROOT_VERB.getAnnotator());
	
	final String output = textAnnotator.getAnnotatedText();
	assertEquals("The <span class=\"Term\" style=\"border:1px solid #00CC00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">fruit</span><span class=\"Term_annotation\" style=\"background-color:#00CC00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[TERM]</span></span> of a <span class=\"Domain\" style=\"border:1px solid #006600; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">tree</span><span class=\"Domain_annotation\" style=\"background-color:#006600; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[DOMAIN]</span></span> is <span class=\"weak\" style=\"border:1px solid #FF8C00; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">sometimes</span><span class=\"weak_annotation\" style=\"background-color:#FF8C00; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[WEAK]</span></span> <span class=\"Explanation\" style=\"border:1px solid #008000; display:inline-table; margin:0.1em;\"><span class=\"content\" style=\"padding-left:0.2em; padding-right:0.1em;\">edible</span><span class=\"Explanation_annotation\" style=\"background-color:#008000; color:white; display:table-cell; font-family:sans-serif; font-size:x-small; font-style:normal; padding-left:1em; padding-right:0.2em;\">[EXPLANATION]</span></span>. It <span class=\"LegalObligation\" style=\"background-color:#D3D3D3; font-weight:bold; padding-left:0.1em; padding-right:0.1em;\">shall</span> be <span class=\"Predicate\" style=\"border-bottom:1px solid black; display:inline-block;\">picked</span>.", output);
    }        
}
