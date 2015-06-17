package helper.subset26;

import helper.annotations.DomainSpecific;
import helper.nlp.NLPJob;
import helper.nlp.NLPManager;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import requirement.RequirementPlaceholder;
import requirement.RequirementWParent;
import requirement.metadata.Kind;
import requirement.metadata.LegalObligation;
import requirement.metadata.MetadataReqif;
import requirement.metadata.TextAnnotator;

import static helper.Constants.Specification.LEGALOBLIGATION_KEYWORDS_MANDATORY;
import static helper.Constants.Specification.LEGALOBLIGATION_KEYWORDS_OPTIONAL;

/**
 * Similar to {@link MetadataDeterminer} but uses hierarchical information as well (i.e. it needs a requirement tree)
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
public final class MetadataDeterminerSecondPass {
    private static final Logger logger = Logger.getLogger(MetadataDeterminerSecondPass.class.getName()); // NOPMD - reference rather than a static field

    private MetadataDeterminerSecondPass() {
	// helper class, not instantiable
    }

    /**
     * Process a given requirement (which needs to be anchored in a proper hierarchy); as a result the metadata may become altered (side effect)
     * 
     * @param requirement requirement to process
     * @param nlpManager handle to NLP parsers or {@code null} if no NLP shall be applied
     * @throws IllegalArgumentException if the given argument is {@code null} 
     */
    @DomainSpecific
    public static void processRequirement(final RequirementWParent requirement, final NLPManager nlpManager) {
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	if (nlpManager == null) throw new IllegalArgumentException("nlpManager cannot be null");
	final MetadataReqif metadata = requirement.getMetadata();

	doNLP(requirement, nlpManager);	

	// reevaluate the switch below as long as the kind keeps changing	
	Kind oldKind = metadata.getKind();
	Kind newKind = oldKind;

	KindEvaluator:
	    do {
		oldKind = newKind;
		switch (oldKind) {		   
		case HEADING:
		    if (!requirement.getChildIterator().hasNext()) {
			// heading without children does not make sense
			newKind = Kind.ORDINARY;
		    }
		    else {
			assert requirement.getChildIterator().hasNext();
			final String lastSentence = getLastSentence(requirement);
			if (lastSentence != null) {
			    if (lastSentence.matches("^.* are defined:?$")) {
				applyToAllChildren(requirement, Kind.DEFINITION, false);
				break KindEvaluator; // do not allow any further changes
			    }
			}
		    }
		    break;	    
		case ORDINARY:
		    // inherit the kind of the parent if applicable
		    if (requirement.getParent() instanceof RequirementWParent) {
			final RequirementWParent parent = (RequirementWParent) requirement.getParent();
			final Kind parentalKind = inheritKindFromParent(parent, metadata.getLegalObligation());
			if (parentalKind != null) {
			    newKind = parentalKind;
			    break;
			}
		    }

		    // see if we are a heading of a sublist
		    if (requirement.getChildIterator().hasNext()) {
			final String lastSentence = getLastSentence(requirement);
			if (lastSentence != null) {
			    if (metadata.getLegalObligation() == LegalObligation.UNKNOWN) {
				final Integer numberOfSentences = getSentences(requirement).length;
				final String regex = "^(?!If |When ).*?(:| following[^\\.:]*)$";
				if (numberOfSentences == 1 && lastSentence.matches(regex)) {
				    newKind = Kind.HEADING;
				    break;
				}
			    }

			    // get the legal obligation of the last sentence which precedes the sublist
			    final LegalObligationDeterminer legalObligationDeterminer = new LegalObligationDeterminer(lastSentence, newKind);
			    legalObligationDeterminer.process();
			    final LegalObligation legalObligationLastSentence = legalObligationDeterminer.getLegalObligation();
			    if (legalObligationLastSentence == LegalObligation.MANDATORY || legalObligationLastSentence == LegalObligation.OPTIONAL){
				// Option 1: match the last sentence against common patterns for OR / XOR
				final StringBuilder regexBuilder = new StringBuilder();
				regexBuilder.append(".*?(?:(?<qXOR> (?<qOR>at least )?one of the)? (?:following|\\w+ listed hereafter)\\b.*|composed of\\b.*");
				for (final String keyword : LEGALOBLIGATION_KEYWORDS_MANDATORY) regexBuilder.append("|(?:(?i)").append(keyword).append(":?)");
				for (final String keyword : LEGALOBLIGATION_KEYWORDS_OPTIONAL) regexBuilder.append("|(?:(?i)").append(keyword).append(":?)");
				regexBuilder.append("|(?<qXOR2>either:?))$");
				final String regex = regexBuilder.toString();
				 
				//final String regex = ".*?(?:(?<qXOR> (?<qOR>at least )?one of the)? following\\b.*|composed of\\b.*|(?<qXOR2>either:?))$";
				final Pattern pattern = Pattern.compile(regex);
				final Matcher matcher = pattern.matcher(lastSentence);
				// Option 2: match the sublist items themselves against common patterns
				final LegalObligation legalObligationOfChildren = SublistRelationDeterminer.getRelationOfChildren(requirement, legalObligationLastSentence);
				if (legalObligationOfChildren != null && !matcher.matches()) {
				    applyToAllChildren(requirement, legalObligationOfChildren, false);
				}
				else if (matcher.matches()) {
				    if (matcher.group("qOR") != null) {
					applyToAllChildren(requirement, legalObligationLastSentence.getListOR(), true);					
				    }
				    else if (matcher.group("qXOR") != null || matcher.group("qXOR2") != null) {
					applyToAllChildren(requirement, legalObligationLastSentence.getListXOR(), true);
				    }
				    else {
					applyToAllChildren(requirement, legalObligationLastSentence, true); // make the children infer our own fake obligation
					if (legalObligationDeterminer.getStopwordCount() == 1 && !lastSentence.matches("(?i)^.+\\b(and|or)\\b.+$")) {
					    newKind = Kind.HEADING;
					    metadata.setLegalObligation(LegalObligation.NA); // set our own obligation correctly
					}
				    }
				    break KindEvaluator; // do not allow any further changes
				}
			    }
			}
		    }
		    break;
		case PLACEHOLDER:
		    final String requirementText = getText(requirement);
		    if (!(requirementText == null || RequirementPlaceholder.getPlaceholderText().equals(requirementText))) {
			final Iterator<RequirementWParent> iterator = requirement.getChildIterator();
			while (iterator.hasNext()) {
			    if (iterator.next().getMetadata().getKind() != oldKind) {
				logger.log(Level.WARNING, "{0}: Encountered a deleted requirement with children.", requirement.getHumanReadableManager().getTag());
				break KindEvaluator;
			    }
			}			
		    }
		    break;
		case EXAMPLE: case JUSTIFICATION: case NOTE: case DEFINITION:
		    break KindEvaluator;   
		case FIGURE: case TABLE:
		    break KindEvaluator;
		default:
		    break;
		}
	    } while (oldKind != newKind);
	metadata.setKind(newKind);
    }

    @DomainSpecific
    private static Kind inheritKindFromParent(final RequirementWParent parent, final LegalObligation legalObligation) {
	assert parent != null;
	final MetadataReqif metadata = parent.getMetadata();

	final Kind input = metadata.getKind();
	final Kind output;
	switch(input) {
	case EXAMPLE: case JUSTIFICATION: case NOTE:
	    output = input;
	    break;
	case DEFINITION:
	    if (legalObligation == LegalObligation.UNKNOWN) {
		output = input;
		break;
	    }
	    // intentional fallthrough
	    //$CASES-OMITTED$
	default:
	    output = null;
	    break;
	}

	return output;
    }


    private static class SublistRelationDeterminer {		

	/**
	 * Determines if the children of a requirement are connected by OR / AND
	 * 
	 * @param parent parent requirement whose children are to be examined
	 * @return the legalObligation of all children or {@code null} if it could not be determined
	 */
	@DomainSpecific
	static LegalObligation getRelationOfChildren(final RequirementWParent parent, final LegalObligation baseObligation) {	
	    assert parent != null;
	    assert parent.getChildIterator().hasNext();

	    final LegalObligation output;
	    final Iterator<RequirementWParent> iterator = parent.getChildIterator();

	    determineRelation: {		
		if (iterator.hasNext()) {
		    // Step 1: peek at the very first one
		    final RequirementWParent firstChild = iterator.next();
		    final LegalObligation loFirstElement = processSingleRequirement(firstChild, baseObligation);
		    if (loFirstElement == null || !iterator.hasNext()) {			
			output = null;	
		    }
		    else if (!iterator.hasNext()) {
			logger.log(Level.INFO, "{0}: Found a single element sublist which looks like it should have neighbors.", firstChild.getHumanReadableManager().getTag());
			output = null;
		    }
		    else {
			// Step 2: compare the first one with all the others except the very last
			while (iterator.hasNext()) {
			    final RequirementWParent currentChild = iterator.next();
			    if (iterator.hasNext()) {
				// execute this for everything but the very last element
				final LegalObligation loCurrentElement = processSingleRequirement(currentChild, baseObligation);
				if (loCurrentElement != loFirstElement) {
				    output = null;
				    break determineRelation;
				}
			    }
			}
			output = loFirstElement;
		    }
		}
		else throw new IllegalStateException("This should not be called for requirements without children.");
	    }
	    return output;
	}

	private static LegalObligation processSingleRequirement(final RequirementWParent requirement, final LegalObligation inheritedObligation) {
	    LegalObligation baseObligation = requirement.getMetadata().getLegalObligation() != LegalObligation.UNKNOWN ? requirement.getMetadata().getLegalObligation() : inheritedObligation;
	    final LegalObligation output;
	    final String text = getText(requirement);
	    if (text != null) {
		final String regex = ".*[, ](?:(?<OR>or)|(?<XOR>OR)|(?i)(?<AND>and))$"; // note the subtle difference between or and xor...
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(text);
		if (matcher.matches()) {
		    if (matcher.group("AND") != null) output = baseObligation;
		    else if (matcher.group("OR") != null) output = baseObligation.getListOR(); // can theoretically propagate null
		    else if (matcher.group("XOR") != null) output = baseObligation.getListXOR(); // can theoretically propagate null
		    else throw new IllegalStateException();
		}
		else output = null;		
	    }
	    else output = null;

	    return output;
	}
    }        

    private static String getText(final RequirementWParent requirement) {
	assert requirement != null;
	final String output;
	if (requirement.getText() != null && requirement.getText().getRaw() != null) {
	    output = requirement.getText().getRaw();
	}
	else output = null;
	return output;
    }

    private static String getLastSentence(final RequirementWParent requirement) {
	assert requirement != null;
	final String output;

	final String[] sentences = getSentences(requirement);
	if (sentences != null) output = sentences[sentences.length-1];
	else output = null;
	return output;	
    }

    private static String[] getSentences(final RequirementWParent requirement) {
	assert requirement != null;
	final String[] output;
	final String requirementText = getText(requirement);
	if (requirementText != null) {
	    output = requirementText.split("(?<=[\\w\\)][\\.:;])\\s(?=[A-Z])");	    
	}
	else output = null;
	return output;
    }

    private static void applyToAllChildren(final RequirementWParent requirement, final Kind kind, final boolean recursive) {
	assert requirement != null && kind != null;
	final Iterator<RequirementWParent> iterator = requirement.getChildIterator();
	while (iterator.hasNext()) {
	    final RequirementWParent child = iterator.next();
	    if (child.getMetadata().getKind() != Kind.PLACEHOLDER) child.getMetadata().setKind(kind);
	    if (recursive) applyToAllChildren(child, kind, true);
	}
    }

    private static void applyToAllChildren(final RequirementWParent requirement, final LegalObligation legalObligation, final boolean recursive) {
	assert requirement != null && legalObligation != null;
	final Iterator<RequirementWParent> iterator = requirement.getChildIterator();
	while (iterator.hasNext()) {
	    final RequirementWParent child = iterator.next();
	    if (child.getMetadata().getKind() != Kind.PLACEHOLDER) {
		if (legalObligation == LegalObligation.MANDATORY && getText(child) != null && getText(child).matches("^(When needed\\b|.*?\\bas needed\\b).*$")) {
		    // "downgrade" the legal obligation
		    child.getMetadata().setLegalObligation(LegalObligation.OPTIONAL);
		}
		else {
		    // ordinary case
		    child.getMetadata().setLegalObligation(legalObligation);
		}
	    }	    
	    if (recursive) applyToAllChildren(child, legalObligation, true);
	}
    }

    private static void doNLP(final RequirementWParent requirement, final NLPManager nlpManager) {
	if (nlpManager.isActive()) { 
	    final String rawText = getText(requirement);
	    if (rawText != null && rawText.length() > 10 && rawText.split("\\s").length > 3) {
		// do not process very short texts
		final TextAnnotator textAnnotator = requirement.getMetadata().getTextAnnotator();
		if (textAnnotator != null) {		    
		    nlpManager.submitNLPJob(new NLPJob(rawText, textAnnotator));		    
		}
	    }
	}
    }
}
