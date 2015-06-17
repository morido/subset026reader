package helper.nlp;

import java.util.Queue;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

class NLPWorkerThread implements Runnable {
    private final static String PARSER_MODEL = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    private final Queue<NLPJob> tasks;

    public NLPWorkerThread(final Queue<NLPJob> tasks) {
	assert tasks != null;	
	this.tasks = tasks;
    }

    @Override
    public void run() {
	// Step 1: load our parser (warmup)
	final LexicalizedParser lexicalizedParser = LexicalizedParser.loadModel(PARSER_MODEL);
	
	// Step 2: work loop (process actual NLP tasks)
	while(!(Thread.currentThread().isInterrupted())) {
	    NLPJob task;
	    do {
		// this is a busy wait; but it should only be iterated n>1-times in the very beginning (i.e. while the producer is still active)
		task = this.tasks.poll();
	    } while (task == null);

	    if (!task.process(lexicalizedParser)) {
		this.tasks.add(task); // put back the pill
		break; // ordinary end of this thread
	    }
	}
    }
}
