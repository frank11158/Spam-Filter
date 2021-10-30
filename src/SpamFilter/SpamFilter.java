package SpamFilter.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.*;

public class SpamFilter {
	private int K;
	
	public double calBiGramProb(String[] inputData) {
		this.K = inputData.length-1;
		long prob = 1;
		Map<String, Integer> map = new HashMap<>();
		for(int i = 0; i < inputData.length-1; i++) {
			String s = inputData[i] + " " + inputData[i+1];
			map.put(s, map.getOrDefault(s, 0)+1);
		}
		for(String s : map.keySet()) {
			prob *= (double) Math.pow(map.get(s), map.get(s));
		}
		return (double) Math.pow(prob, (double)1/K);
	}

	public static void main(String[] args) {
		SpamFilter spamFilter = new SpamFilter();
		
		// Read file
		StringBuilder data = new StringBuilder();
		try {
			File file = new File("./Files/SampleData3.txt");
			Scanner reader = new Scanner(file);
			while (reader.hasNextLine()) {
				data.append(reader.nextLine());
				data.append(" ");
			}
			reader.close();
		} catch(FileNotFoundException e) {
			System.out.println("File is not found.");
		}

		// calculate prob
		double prob = spamFilter.calBiGramProb(data.toString().replaceAll("\\p{P}", "").toLowerCase().split("\\s+"));
		
		// Write file
		File newFile = new File("Result.txt");
		try {
			System.out.println("File created: " + newFile.getName());
			FileWriter writer = new FileWriter("Result.txt");
			writer.write("K: " + String.valueOf(spamFilter.K) + System.lineSeparator());
			DecimalFormat df = new DecimalFormat("#.#####");
			writer.write("bi-gram probability: " + String.valueOf(df.format(prob)));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
