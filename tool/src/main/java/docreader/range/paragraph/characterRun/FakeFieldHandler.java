package docreader.range.paragraph.characterRun;

import static helper.Constants.MSWord.DELIMITER_LISTLEVEL;
import static helper.Constants.Links.EXTRACT_LINKS;
import static helper.Constants.Links.EXTRACT_FAKE_LINKS;
import helper.RegexHelper;
import helper.annotations.DomainSpecific;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hwpf.usermodel.CharacterRun;

import requirement.RequirementTemporary;
import requirement.TraceabilityLinkerNonQualified;
import requirement.data.RequirementLinks;

/**
 * Processes textual links, which are not declared as such (i.e. they are not embedded in a Word field)
 * <p>similar to {@link docreader.range.paragraph.ParagraphReader.FieldDataHandler}</p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
@SuppressWarnings("javadoc")
public class FakeFieldHandler extends CharacterRunRawProcessor {    
    private final RequirementTemporary requirement;
    private final TraceabilityLinkerNonQualified traceabilityLinkerNonQualified;
    private boolean finalRunIsOfInterest = false;
    private Link realLink = null;
    private boolean paragraphHasLinks = false; // will be true if the underlying paragraph has at least one link
    private final static String LIST_LEVEL_DELIMITER_QUOTED = RegexHelper.quoteRegex(DELIMITER_LISTLEVEL);	
    private final static String REGEX_POSTFIX = "[\\)\\],;:\\.”]*(?= (?!(?:k[mV]|m/s))|/[1-8]|$)";
    private static final Logger logger = Logger.getLogger(FakeFieldHandler.class.getName()); // NOPMD - Reference rather than a static field
    
    private static class Link {
	private final LinkType linkType;
	private final String linkText;

	public Link (final String linkText, final LinkType linkType) {
	    assert linkText != null && linkType != null;
	    this.linkText = normalizeLinkText(linkText, linkType);
	    this.linkType = linkType;
	}

	public Link (final String linkText) {
	    final LinkType guessedType;	    
	    if (linkText.matches("(?i)^(Figure |Fig\\.).*")) {		
		guessedType = LinkType.FIGURE;
	    }
	    else if (linkText.matches("(?i)^(Table |Tab\\.).*")) {
		guessedType = LinkType.TABLE;		
	    }
	    else {
		guessedType = LinkType.REQUIREMENT;
	    }
	    this.linkText = normalizeLinkText(linkText, guessedType);
	    this.linkType = guessedType;
	}

	public void save(final RequirementLinks linkStore) {
	    this.linkType.saveLink(linkStore, this.linkText);
	}

	private static String normalizeLinkText(final String linkText, final LinkType linkType) {	    	    
	    switch(linkType) {
	    case FIGURE: case TABLE:
		String normalizedNumber = RegexHelper.extractRegex(linkText, "([1-9][0-9]* ?[a-z]?)");
		return normalizedNumber.replace(" ", "");
		//$CASES-OMITTED$
	    default:
		return linkText;
	    }
	}	
    }

    private enum LinkType {
	FIGURE {
	    @Override
	    public void saveLink(RequirementLinks linkStore, String link) {
		linkStore.addLinkToGivenFigure(link);
	    }	    	   
	},
	TABLE {
	    @Override
	    public void saveLink(RequirementLinks linkStore, String link) {
		linkStore.addLinkToGivenTable(link);		
	    }
	},
	REQUIREMENT {
	    @Override
	    public void saveLink(RequirementLinks linkStore, String link) {
		linkStore.addLinkToGivenRequirementID(link);
	    }	    
	};

	public abstract void saveLink(final RequirementLinks linkStore, final String link);
    }


    /**
     * Ordinary constructor
     * 
     * @param traceabilityLinkerNonQualified handle to the global traceability linker for non qualified requirements
     * @param startOffset startOffset from where to start reading the paragraph text (0-based; relevant for fake list paragraphs)
     * @param requirement requirement where to store the links
     * @param lastWordOfPreviousParagraph last word of the previously processed paragraph (used for links which span several paragraphs)
     * @throws IllegalArgumentException if one of the arguments is {@code null}
     */
    public FakeFieldHandler(final TraceabilityLinkerNonQualified traceabilityLinkerNonQualified, final int startOffset, final RequirementTemporary requirement, final String lastWordOfPreviousParagraph) {
	super(startOffset);
	if (traceabilityLinkerNonQualified == null) throw new IllegalArgumentException("traceabilityLinkerNonQualified cannot be null.");
	if (requirement == null) throw new IllegalArgumentException("requirement cannot be null.");
	if (lastWordOfPreviousParagraph == null) throw new IllegalArgumentException("lastWordOfPreviousParagraph cannot be null - but may be empty.");
	this.traceabilityLinkerNonQualified = traceabilityLinkerNonQualified;
	this.requirement = requirement;	

	this.characterRuns.append(lastWordOfPreviousParagraph);
    }

    @Override
    public void read(final CharacterRun inputRun) {
	if (EXTRACT_LINKS && EXTRACT_FAKE_LINKS) {
	    super.read(inputRun);
	    checkForTextualLink(false);
	}
    }

    /**
     * reset any historical information; used after a real field (which is necessarily at a characterRun boundary and cannot overlap with a fake field)
     */
    public void reset(final boolean isBookmarkField, final String fieldText) {	
	if (isBookmarkField) {
	    // check if any fake children depend on this true link; foward looking is done in checkForTextualLink
	    // feature vs. bug: this will add both the base (real link) and the children as links; whereas for a completely fake link it would only do the latter
	    if (fieldText == null) throw new IllegalArgumentException("fieldText cannot be null.");
	    final Link fieldLink = new Link(fieldText);
	    checkForChildrenAndWriteLinks(this.characterRuns.toString(), null, fieldLink, false); // intentionally discard the output
	    this.realLink = fieldLink;
	}

	this.characterRuns.delete(0, this.characterRuns.length());
    }

    @Override
    public void close() {
	if (this.finalRunIsOfInterest) checkForTextualLink(true);
    }

    /**
     * @return a regex to match a common requirement reference in the subset
     */
    @DomainSpecific
    public static String getRequirementRegex() {
	return "(?<requirement>(?<appendixPrepender>A[" + LIST_LEVEL_DELIMITER_QUOTED + "]?)?[1-8](" + LIST_LEVEL_DELIMITER_QUOTED + "[0-9]{1,3}){1,8})";
    }

    /**
     * @param finalMode {@code true} if this is the 2nd pass of the final character run in a paragraph; {@code false} otherwise
     */
    @DomainSpecific
    private void checkForTextualLink(final boolean finalMode) {
	final String input = this.characterRuns.toString();
	int deletePosition = 0;

	// check if we had field text (= real link) in the previous run
	if (this.realLink != null) {
	    // this is a little weak because it expects all children to be in one character run
	    int offset = 0;
	    final Set<Link> sanitizedTargets = new LinkedHashSet<>(1);
	    offset += childRequirementForwards(input, this.realLink, sanitizedTargets);
	    if (offset > 0) writeLinks(sanitizedTargets);	    
	    deletePosition += offset;
	    this.paragraphHasLinks = true;
	}
	else {
	    // check if we have a non qualified link (can only appear in the very beginning)
	    final String nonQualifiedRegex = "^(?:Regarding|Exception to) ([a-z])\\):?.*$";
	    final Pattern nonQualifiedPattern = Pattern.compile(nonQualifiedRegex);
	    final Matcher nonQualifiedMatcher = nonQualifiedPattern.matcher(input);
	    if (nonQualifiedMatcher.matches()) {
		final String targetID = this.traceabilityLinkerNonQualified.resolveNonQualifiedId(nonQualifiedMatcher.group(1));		
		if (targetID != null) {    
		    final Set<Link> links = new LinkedHashSet<>(1);
		    links.add(new Link(targetID, LinkType.REQUIREMENT));
		    writeLinks(links);
		    deletePosition = nonQualifiedMatcher.end(1);
		}
		else {
		    logger.log(Level.INFO, "Found something that looks like a backreference to a previous requirement. But that requirement does not exist.");
		}
	    }
	}

	final String figureRegex = "(?<figure>(?i)(Figure |Fig\\.)[1-9][0-9]?(?-i))";
	final String tableRegex =  "(?<table>(?i)(Table |Tab\\.)[1-9][0-9]?(?-i))";	
	final String regex = "(?:(?:[\\(/§ ])|^)(" + getRequirementRegex() + "|" + figureRegex + "|" + tableRegex + ")[a-z]?" + REGEX_POSTFIX;	
	final Pattern pattern = Pattern.compile(regex);
	final Matcher matcher = pattern.matcher(input);

	while(matcher.find()) {
	    final String prefix = input.substring(deletePosition, matcher.start());
	    deletePosition = matcher.end(1);

	    if (matcher.group("requirement") != null) {
		if (prefix.matches(".*(?i)((SRSs?|Issue|column|(Subset[ -]?[0-9]{1,3}(,? section)?( §)?)|(\\b[Vv]ersion( number)?)))$")) {
		    // this seems to be some sort of external reference or versioning information or both; skip
		    continue;
		}
		if (prefix.matches(".*([=≤≥<>]|[A-Z]:)$")) {
		    // some sort of assignment takes place here
		    continue;
		}
		if (prefix.matches(".*(is|are) (\\w+ )?between")) {
		    // probably some range of a floating point value
		    continue;
		}
		if (!this.paragraphHasLinks && prefix.matches("^(,|[,;]? (and|&|or))$")) {
		    // apparently the first entity was no link; so the second cannot be one either
		    continue;
		}
	    }
	    final StringBuilder linkTarget = new StringBuilder(matcher.group(1));

	    this.finalRunIsOfInterest = false;

	    final Link baseLink;
	    if (matcher.group("requirement") != null) {	
		if (!finalMode &&
			(matcher.end() == input.length() || linkTarget.charAt(linkTarget.length()-1) == DELIMITER_LISTLEVEL || input.substring(matcher.end()).matches(" [a-z]?"))) {
		    // there might be more characters of interest in the following characterRun; stop here and consume more characters
		    this.finalRunIsOfInterest = true;
		    return;
		}

		final String appendixPrepender = matcher.group("appendixPrepender");
		if (appendixPrepender != null && "A".equals(appendixPrepender)) {		    
		    // appendix group not separated by a dot; force dot
		    linkTarget.insert(1, DELIMITER_LISTLEVEL);
		}
		else if (appendixPrepender == null && prefix.matches(".*Appendix$")) {
		    linkTarget.insert(0, "A" + DELIMITER_LISTLEVEL);
		}
		baseLink = new Link(linkTarget.toString(), LinkType.REQUIREMENT);
	    }
	    else if (matcher.group("figure") != null) {
		if (!finalMode &&
			(matcher.end() == input.length() || input.substring(matcher.end()).matches(" [a-z]?"))) {
		    // there might be more characters of interest in the following characterRun; stop here and consume more characters
		    this.finalRunIsOfInterest = true;
		    return;
		}
		baseLink = new Link(linkTarget.toString(), LinkType.FIGURE);
	    }
	    else if (matcher.group("table") != null) {
		if (!finalMode &&
			(matcher.end() == input.length() || input.substring(matcher.end()).matches(" [a-z]?"))) {
		    // there might be more characters of interest in the following characterRun; stop here and consume more characters
		    this.finalRunIsOfInterest = true;
		    return;
		}
		baseLink = new Link(linkTarget.toString(), LinkType.TABLE);
	    }
	    else {
		throw new IllegalStateException("Fake link extraction failed.");
	    }

	    deletePosition += checkForChildrenAndWriteLinks(prefix, input.substring(matcher.end(1)), baseLink, true);	    
	}

	this.characterRuns.delete(0, deletePosition);
    }


    private int checkForChildrenAndWriteLinks(final String prefix, final String appendix, final Link baseLink, final boolean addBaseLink) {	
	final Set<Link> sanitizedTargets = new LinkedHashSet<>(1);	    
	childRequirementBackwards(prefix, baseLink, sanitizedTargets);
	final int deletePosition = appendix != null ? childRequirementForwards(appendix, baseLink, sanitizedTargets) : 0;	    
	if (sanitizedTargets.isEmpty() && addBaseLink) sanitizedTargets.add(baseLink);
	this.writeLinks(sanitizedTargets);
	return deletePosition;
    }

    private void writeLinks(final Set<Link> links) {
	assert links != null;	
	for (final Link currentLink : links) {
	    this.paragraphHasLinks = true;
	    currentLink.save(this.requirement.getRequirementLinks());	
	}
	this.realLink = null; // throw away; fully processed now
    }

    /**
     * Forward search; are we suffixed by a list of child-requirements?
     * 
     * @param suffix text right of the current match
     * @param baseLink current matched tag (on which any non-qualified children must be based)
     * @param output handle to a set of matches where the output will be written
     * @return the number of characters to skip because they are actually a part of the link (i.e. contain children)
     */
    @DomainSpecific
    private static int childRequirementForwards(final String suffix, final Link baseLink, final Set<Link> output) {
	int offset = 0;
	if (baseLink.linkType == LinkType.REQUIREMENT) {
	    final String regex = "^(?:" + LIST_LEVEL_DELIMITER_QUOTED + "|(?: (?:items? )?))?([a-z])\\)?((?:,? [a-z]\\)?)*)(?: (?:and|&) ([a-z]\\)?))?" + REGEX_POSTFIX;

	    final Pattern pattern = Pattern.compile(regex);
	    final Matcher matcher = pattern.matcher(suffix);

	    if (matcher.find()) {	    
		for (int i = 1; i <= matcher.groupCount(); i++) {
		    final String currentGroup = matcher.group(i);
		    if (currentGroup != null) {
			for (final String currentGroupPart : currentGroup.split(",")) {
			    // we cannot have variable number of groups in a regex, so split any lists herein
			    final String child = RegexHelper.extractRegex(currentGroupPart, "([a-z])");
			    if (child != null) output.add(new Link(baseLink.linkText + String.valueOf(DELIMITER_LISTLEVEL) + child, baseLink.linkType));
			}			
			offset = matcher.end(i);
		    }
		}	
	    }
	}
	else {
	    assert baseLink.linkType == LinkType.FIGURE || baseLink.linkType == LinkType.TABLE;

	    final String regex = "^ ?([a-z])(?: and ([a-z]))?" + REGEX_POSTFIX;

	    final Pattern pattern = Pattern.compile(regex);
	    final Matcher matcher = pattern.matcher(suffix);

	    if (matcher.find()) {	    
		for (int i = 1; i <= matcher.groupCount(); i++) {
		    final String currentGroup = matcher.group(i);
		    if (currentGroup != null) {	
			final String child = RegexHelper.extractRegex(currentGroup, "([a-z])");
			if (child != null) output.add(new Link(baseLink.linkText + child, baseLink.linkType));			
			offset = matcher.end(i);
		    }
		}	
	    }
	}
	return offset;
    }

    /**
     * Backward search; are we prepended by a list of child-requirements?
     * 
     * @param prefix text left of the current match
     * @param baseLink current matched tag (on which any non-qualified children must be based)
     * @param output handle to a set of matches where the output will be written
     */
    @DomainSpecific
    private static void childRequirementBackwards(final String prefix, final Link baseLink, final Set<Link> output) {
	// only applicable for requirement links
	if (baseLink.linkType == LinkType.REQUIREMENT) {
	    // if java had named groups with recursion we could do this in a single regex rather than two
	    final String postfix = " (?:of(?: clause)?|in)$";	    
	    final String regexForABCSublist = "\\b([a-z]\\)?)((?:, [a-z]\\)?)*)(?: (?:and|&) ([a-z]\\)?))?" + postfix;
	    final String regexForBulletSublist = "\\b([1-9][0-9]*)(?:st|nd|rd|th) bullet(?: point)?";

	    final Pattern abcPattern = Pattern.compile(regexForABCSublist);
	    final Matcher abcMatcher = abcPattern.matcher(prefix);
	    final Pattern bulletPattern = Pattern.compile(regexForBulletSublist);
	    final Matcher bulletMatcher = bulletPattern.matcher(prefix);

	    if (abcMatcher.find()) {	    
		for (int i = 1; i <= abcMatcher.groupCount(); i++) {
		    final String currentGroup = abcMatcher.group(i);
		    if (currentGroup != null) {
			for (final String currentGroupPart : currentGroup.split(",")) {
			    // we cannot have variable number of groups in a regex, so split any lists herein
			    final String child = RegexHelper.extractRegex(currentGroupPart, "([a-z])");
			    if (child != null) output.add(new Link(baseLink.linkText + String.valueOf(DELIMITER_LISTLEVEL) + child, baseLink.linkType));
			}
		    }
		}
	    }
	    else if (bulletMatcher.find()) {
		final String child = "*[" + bulletMatcher.group(1) + "]"; // Note: dependent on implementation in TraceabilityManagerHumanReadable
		output.add(new Link(baseLink.linkText + String.valueOf(DELIMITER_LISTLEVEL) + child, baseLink.linkType));
	    }
	}
    }    
}