package docreader;

import java.util.Iterator;

import helper.ConsoleOutputFilter;
import helper.nlp.NLPManager;
import helper.subset26.MetadataDeterminerSecondPass;
import requirement.RequirementRoot;
import requirement.RequirementWParent;

/**
 * Rescans the requirement tree and adjusts certain data which can only be determined with lookarounds
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
class SecondPassReader implements GenericReader<Void> {        
    private final RequirementRoot root;
    private final NLPManager nlpManager;
    private final ConsoleOutputFilter consoleFilter;

    public SecondPassReader(final RequirementRoot root, final ConsoleOutputFilter consoleFilter) {
	assert root != null && consoleFilter != null;
	this.root = root;
	this.consoleFilter = consoleFilter;	
	this.nlpManager = new NLPManager(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public Void read() {
	this.nlpManager.writeStatusOutput(this.consoleFilter);
	recurse(this.root);
	this.nlpManager.waitForNLPJobsToFinish();
	return null;
    }
    
    /**
     * Recurse into child requirements
     * 
     * @param currentRequirement requirement from which to obtain children
     */
    private void recurse(final RequirementRoot currentRequirement) {
	assert currentRequirement != null;
	final Iterator<RequirementWParent> iterator = currentRequirement.getChildIterator();	   
	while (iterator.hasNext()) process(iterator.next());	
    }

    /**
     * Generate the the tree part for anything below the root
     * 
     * @param currentRequirement requirement to process
     */
    private void process(final RequirementWParent currentRequirement) {
	assert currentRequirement != null;	
	MetadataDeterminerSecondPass.processRequirement(currentRequirement, this.nlpManager);

	recurse(currentRequirement);	
    }    
}
