package requirement;

import helper.TraceabilityManagerHumanReadable;

/**
 * Interface for any sort of requirement (or whatever else) which is uniquely adressable
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
public interface TraceableArtifact {
    /**
     * @return the hrmanager associated with this artifact, never {@code null}
     */     
    TraceabilityManagerHumanReadable getHumanReadableManager();
    
    /**
     * @return a textual representation of the contents of this artifact; never {@code null}
     */
    String getContent();
    
    /**
     * @return {@code true} if this artifact must be implemented; {@code false} otherwise
     */
    boolean getImplementationStatus();
}
