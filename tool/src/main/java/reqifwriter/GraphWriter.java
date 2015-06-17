package reqifwriter;

import helper.ParallelExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import static helper.Constants.Generic.STATISTICS_STORE_DIR;
import static helper.Constants.Generic.WRITE_STATISTICAL_DATA;
import requirement.TraceableArtifact;

/**
 * Write a simple CSV-output of the requirement tree (nodes + edges); may be used used by tools such as gephi
 * <p>This is purely nice-to-have and technically unrelated to the ReqIF-output</p>
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de>
 */
class GraphWriter {           
    
    private transient final Collection<Node> nodes = new ArrayList<>();
    private transient final Collection<Edge> edges = new ArrayList<>();
    
    /**
     * A node in the resulting graph (requirements will be mapped to nodes)
     */
    private static class Node {
	private final String id;
	private final String label;
	private final int level;
	private final boolean implementationStatus;
	
	Node(final String id, final String label, final int level, final boolean implementationStatus) {
	    assert id != null && label != null;
	    this.id = id;
	    this.label = label;
	    this.level = level;
	    this.implementationStatus = implementationStatus;
	}	
	
	@Override
	public String toString() {
	    final StringBuilder output = new StringBuilder();
	    output.append('"');
	    output.append(this.id);
	    output.append('"').append(',').append('"');
	    output.append(this.label);
	    output.append('"').append(',').append('"');
	    output.append(this.level);
	    output.append('"').append(',').append('"');
	    output.append(this.implementationStatus ? "TRUE" : "FALSE");
	    output.append('"');
	    return output.toString();
	}
	
	/**
	 * Columns map as follows:
	 * <dl>
	 * <dt>ID</dt><dd>GUID of the node</dd>
	 * <dt>Label</dt><dd>Human-readable contents of this node</dd>
	 * <dt>Level</dt><dd>Hierarchical level where this node is located (0 means root, +1 for each level)</dd>
	 * <dt>Implement</dt><dd>{@code TRUE} if this artifact must be implemented; {@code FALSE} otherwise</dd>
	 * </dl>
	 * 
	 * @return the header of the resulting CSV-file, never {@code null}
	 */
	static String getCSVdescription() {
	    final StringBuilder output = new StringBuilder(20);
	    output.append('"');
	    output.append("ID");
	    output.append('"').append(',').append('"');
	    output.append("Label");
	    output.append('"').append(',').append('"');
	    output.append("Level");
	    output.append('"').append(',').append('"');
	    output.append("Implement");	    
	    output.append('"');
	    return output.toString();
	}
    }
    
    /**
     * An edge in the resulting graph (links between requirements are mapped to edges)
     */
    private static class Edge {
	enum EdgeType {
	    PARENT_CHILD(3.0f),	    
	    LINK(2.0f),
	    KNOWN_TERM_LINK(1.0f);
		
	    private float weight;
	    
	    /**
	     * @param weight edge weight
	     */
	    private EdgeType(final float weight) {
		this.weight = weight;
	    }	    	   
	}
	
	private final String source;
	private final String target;
	private final EdgeType edgeType;
	
	Edge(final String source, final String target, final EdgeType edgeType) {
	    assert source != null && target != null;
	    this.source = source;
	    this.target = target;
	    this.edgeType = edgeType;
	}
	
	@Override
	public String toString() {
	    final StringBuilder output = new StringBuilder();
	    output.append('"');
	    output.append(this.source);
	    output.append('"').append(',').append('"');
	    output.append(this.target);
	    output.append('"').append(',').append('"');
	    output.append(this.edgeType.weight);
	    output.append('"');
	    return output.toString();
	}
	
	/**
	 * Columns map as follows:
	 * <dl>
	 * <dt>Source</dt><dd>GUID of the source of this link</dd>
	 * <dt>Target</dt><dd>GUID of the target of this link</dd>
	 * <dt>Weight</dt><dd>Float representing the importance of this link (higher means more important)</dd>
	 * </dl>
	 * 
	 * @return the header of the resulting CSV-file, never {@code null}
	 */
	static String getCSVdescription() {
	    final StringBuilder output = new StringBuilder(26);
	    output.append('"');
	    output.append("Source");
	    output.append('"').append(',').append('"');
	    output.append("Target");
	    output.append('"').append(',').append('"');
	    output.append("Weight");
	    output.append('"');
	    return output.toString();
	}
    }
    
    /**
     * Add any node below the root
     * 
     * @param requirement node to add
     */
    void addNode(final TraceableArtifact requirement) {
	if (doNothing()) return;
	assert requirement != null;
	final String tracestring = requirement.getHumanReadableManager().getTag();
	final int level = requirement.getHumanReadableManager().getHierarchicalLevel();
	final boolean implementationStatus = requirement.getImplementationStatus();
	final String identifier = SpecObjectsWriter.queryIdentifier(requirement);
	this.addNode(identifier, tracestring, requirement.getContent(), level, implementationStatus);
    }
    
    /**
     * Add the root node
     * 
     * @param identifier identifier of the root node
     */
    void addRootNode(final String identifier) {
	if (doNothing()) return;
	assert identifier != null;
	this.addNode(identifier, "ROOT", "", 0, false);
    }
    
    private void addNode(final String identifier, final String tracestring, final String content, final int level, final boolean implementationStatus) {
	if (identifier == null) throw new IllegalArgumentException("id cannot be null.");
	if (content == null) throw new IllegalArgumentException("content cannot be null.");
	
	final StringBuilder labelBuilder = new StringBuilder();	
	labelBuilder.append(tracestring);
	if (!"".equals(content)) {	    
	    labelBuilder.append(' ');
	    // since this is going to be CSV we need to make sure to strip any newlines
	    final String contentCleaned = content.replaceAll("(\r\n|\n)", " ").trim();
	    if (content.length() > 25) {
		labelBuilder.append(contentCleaned.substring(0, 22));
		labelBuilder.append("...");		
	    }
	    else {
		labelBuilder.append(contentCleaned);
	    }
	}

	final Node node = new Node(identifier, labelBuilder.toString(), level, implementationStatus);
	this.nodes.add(node);
    }
           
    /**
     * Add an edge representing an ordinary hierarchical relation between two artifacts
     * 
     * @param sourceId identifier (GUID) of the source (parent)
     * @param targetId identifier (GUID) of the target (child)
     */
    void addParentChildEdge(final String sourceId, final String targetId) {
	if (doNothing()) return;
	assert sourceId != null && targetId != null;
	final Edge edge = new Edge(sourceId, targetId, Edge.EdgeType.PARENT_CHILD);
	this.edges.add(edge);
    }
    
    /**
     * Add an edge representing a link (SpecRelation) for conventional cross-references
     * 
     * @param sourceId identifier (GUID) of the source (parent)
     * @param targetId identifier (GUID) of the target (child)
     */
    void addLinkEdge(final String sourceId, final String targetId) {
	if (doNothing()) return;
	assert sourceId != null && targetId != null;
	final Edge edge = new Edge(sourceId, targetId, Edge.EdgeType.LINK);
	this.edges.add(edge);
    }
    
    /**
     * Add an edge representing a link (SpecRelation) for defining terms
     * 
     * @param sourceId identifier (GUID) of the source (parent)
     * @param targetId identifier (GUID) of the target (child)
     */
    void addKnownTermLinkEdge(final String sourceId, final String targetId) {
	if (doNothing()) return;
	assert sourceId != null && targetId != null;
	final Edge edge = new Edge(sourceId, targetId, Edge.EdgeType.KNOWN_TERM_LINK);
	this.edges.add(edge);
    }
    
    /**
     * Serialize all accumulated data to CSV-files
     * 
     * @param absolutePathPrefix global path to the base directory where the output shall be written
     */
    void writeStatisticsData(final String absolutePathPrefix) {
	if (doNothing()) return;
	assert absolutePathPrefix != null;
		
	final String statisticsStoreDirAbsolute = absolutePathPrefix + File.separator + STATISTICS_STORE_DIR;
	final File statisticsStoreDirHandler = new File(statisticsStoreDirAbsolute);
	if (statisticsStoreDirHandler.exists()) throw new IllegalStateException("The statisticsStoreDir already exists. Please delete it first. Path: " + statisticsStoreDirAbsolute);
	else if (!statisticsStoreDirHandler.mkdir()) throw new IllegalStateException("The statisticsStoreDir cannot be created. Please check permissions. Path: " + statisticsStoreDirAbsolute);

		
	final Runnable nodeSerializer = new Runnable() {
	    @Override
	    public void run() {
		serializeNodesToCSV(statisticsStoreDirAbsolute);
	    }
	};
	final Runnable edgeSerializer = new Runnable() {
	    @Override
	    public void run() {
		serializeEdgesToCSV(statisticsStoreDirAbsolute);
	    }
	};
	
	ParallelExecutor.execute("Statistics", nodeSerializer, edgeSerializer);
    }
    
    private void serializeNodesToCSV(final String outputDir) {
	assert outputDir != null;
	try (final PrintWriter printWriter = new PrintWriter(outputDir + File.separator + "nodes.csv")) {
	    printWriter.println(Node.getCSVdescription()); // write CSV-header
	    for (final Node currentNode : this.nodes) {
		printWriter.println(currentNode.toString());
	    }
	} catch (FileNotFoundException e) {
	    throw new IllegalStateException("Cannot write CSV for nodes", e);
	}
    }
    
    private void serializeEdgesToCSV(final String outputDir) {
	assert outputDir != null;
	try (final PrintWriter printWriter = new PrintWriter(outputDir + File.separator + "edges.csv")) {
	    printWriter.println(Edge.getCSVdescription()); // write CSV-header
	    for (final Edge currentEdge : this.edges) {
		printWriter.println(currentEdge.toString());
	    }
	} catch (FileNotFoundException e) {
	    throw new IllegalStateException("Cannot write CSV for edges", e);	    
	}
    }
    
    /**
     * @return if {@code true} then behave transparently and do not carry out any actual work
     */
    private static boolean doNothing() {
	return !WRITE_STATISTICAL_DATA;
    }
}