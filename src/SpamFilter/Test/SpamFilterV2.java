package SpamFilter.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.*;
import java.util.*;
import java.text.DecimalFormat;

public class SpamFilterV2 {
	private Map<List<Double>, Integer> resultMap = new HashMap<>(); // records calculated results
	private final Queue<File> fileQueue = new LinkedList<File>(); // restore file entries
	private ExecutorService executor;

	// read file entries into fileQueue
	public void readFilesToQueue(String DirectoryRoute) {
		final File folder = new File(DirectoryRoute);
		for (final File fileEntry : folder.listFiles()) {
			this.fileQueue.add(fileEntry);
	    }
	}
	
	// synchronized the polling fileQueue to avoid racing
	public synchronized File pollQueue() {
		return this.fileQueue.poll();
	}
	
	// synchronized the putting into resultMap to avoid racing
	public synchronized void mapPut(List<Double> pair) {
		int currentVal = this.mapGetOrDefault(pair, 0);
		this.resultMap.put(pair, ++currentVal);
		// print result after finished map put
		DecimalFormat df = new DecimalFormat("#.#####");
		System.out.println(String.valueOf((int)(double)pair.get(0)) + ", " + String.valueOf(df.format(pair.get(1))) + ", " + this.resultMap.get(pair));
	}

	// synchronized the getting value from resultMap to avoid racing
	public synchronized int mapGetOrDefault(List<Double> pair, int defVal) {
		if(this.resultMap.get(pair) != null) 
			return this.resultMap.get(pair);
		else return defVal;
	}
	
	// read file and return String array contain only letters and digits
	public String[] readFile(File fileName) {
    	StringBuilder data = new StringBuilder();
		try {
			Scanner reader = new Scanner(fileName);
			while (reader.hasNextLine()) {
				data.append(reader.nextLine());
				data.append(" ");
			}
			reader.close();
		} catch(FileNotFoundException e) {
			System.out.println("File is not found.");
		}
		return data.toString().replaceAll("\\p{P}", "").toLowerCase().split("\\s+");
	}

	// Calculate bi-gram probability of an input data from one file
	public double calBiGramProb(String[] inputData, int K) {
		long prob = 1;
		Map<String, Integer> map = new HashMap<>();
		for(int i = 0; i < inputData.length-1; i++) {
			String s = inputData[i] + " " + inputData[i+1]; // bi-gram: concatenate 2 consecutive words
			map.put(s, map.getOrDefault(s, 0)+1);
		}
		for(String s : map.keySet()) {
			prob *= (double) Math.pow(map.get(s), map.get(s));
		}
		return (double) Math.pow(prob, (double)1/K);
	}
	
	// runnable task for each threads to execute
	public static class Task implements Runnable {
		public SpamFilterV2 spamFilter;
		
		public Task(SpamFilterV2 spamFilter) {
			this.spamFilter = spamFilter;
		}
		
		// for each file, execute 3 steps as following:
		public void run() {
			File workFile = null;
            while((workFile = spamFilter.pollQueue()) != null) {
            	String[] inputData = spamFilter.readFile(workFile); // 1. read file
            	int K = inputData.length-1;
        		double prob = spamFilter.calBiGramProb(inputData, K); // 2. calculate bi-gram probability
        		List<Double> pair = Arrays.asList((double)K, prob);
        		spamFilter.mapPut(pair); // 3. recorded into resultMap and print out result
        	}
		}
	}
	
	public void createThreadPool(int maxNumOfThreads) {
		this.executor = Executors.newFixedThreadPool(maxNumOfThreads);
	}
	
	public void executeThreads(int maxNumOfThreads) {
		for(int i = 1; i <= maxNumOfThreads; i++) {
			// run maximum of 12 threads to calculate bi-gram probability and print out its result
			this.executor.submit(new Task(this));
		}
	}
	
	public void shutDownExecutor() {
		this.executor.shutdown();
		try { // shutdown if doesn't finished after 1 day
		    if (!executor.awaitTermination(1, TimeUnit.DAYS)) {
		    	this.executor.shutdownNow();
		    } 
		} catch (InterruptedException e) {
			this.executor.shutdownNow();
		}
	}

	public static void main(String[] args) {
		String fileDirectory = "./Files";
		int maxNumOfThreads = 12;
		SpamFilterV2 spamFilter = new SpamFilterV2();
		
		// output format:
		System.out.println("k, bi-gram probability, occurrence of (k, bi-gram probability) pair so far:");

		spamFilter.readFilesToQueue(fileDirectory);
		spamFilter.createThreadPool(maxNumOfThreads);
		spamFilter.executeThreads(maxNumOfThreads);
		spamFilter.shutDownExecutor();
	}
}
