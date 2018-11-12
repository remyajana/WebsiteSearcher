package com.wework.main;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application extends Thread {

	/* ConcurrentLinkedQueue is wait-free and lock-free for storing the urls */
	static Queue<String> queue = new ConcurrentLinkedQueue<String>();
	/* SynchronizedList used to manage the result written on by multiple threads */
	static List<String> result = Collections.synchronizedList(new ArrayList<>());

	static String inp_urlFile = null;
	//static String out_resultFile = null;
	static Pattern pattern;
	
	public static void processQueue(String searchKeyWord) {
		URL url = null;
		String line = null;
		BufferedReader reader = null;
		try {
			while (!queue.isEmpty()) { // threads will keep polling from the queue till it is empty
				url = new URL(queue.poll());
				reader = new BufferedReader(new InputStreamReader(url.openStream()));

				while ((line = reader.readLine()) != null) {
					Matcher matcher = pattern.matcher(line); // pattern matching
					if (matcher.find()) {
						result.add(String.valueOf(url));
					}
				}
			}
		} catch (Exception e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void readFromURLFile() {
		FileInputStream inputStream = null;
		Scanner scanner = null;
		try {
			// inputStream = new FileInputStream(System.getProperty("user.dir") + "/src/main/java/resources/url.txt");

			inputStream = new FileInputStream(inp_urlFile);
			scanner = new Scanner(inputStream, "UTF-8");
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] domain = line.split(",");
				queue.offer("http://" + ((domain[1]).replaceAll("\"", "")));
			}

			if (scanner.ioException() != null) {
				throw scanner.ioException();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// PrintWriter writer = new PrintWriter(System.getProperty("user.dir") + "/src/main/java/resources/results.txt", "UTF-8");

		System.out.println("Please provide the path for url.txt file (For Eg- E:\\Users\\url.txt ) : ");
		inp_urlFile = reader.readLine();		

		System.out.println("Enter the search word (For Eg - is, has, you, open.. ) : ");
		String searchKeyWord = reader.readLine();
		pattern = Pattern.compile(searchKeyWord); // compiling the pattern for multiple match

		System.out.println("No. of concurrent searches at a given time (For Eg- 5, 10, max upto 20 ) : ");
		int no_concurrentSearches = Math.max(20, Integer.valueOf(reader.readLine()));
		
		
		System.out.println("Search in progress...");

		readFromURLFile(); // populating the queue with the urls to be searched

		Thread[] searchInEachURL = new Thread[no_concurrentSearches]; // Maximum of 20 threads doing concurrent search
		for (int i = 0; i < searchInEachURL.length; i++) {
			searchInEachURL[i] = new Thread(() -> {
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 5000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				processQueue(searchKeyWord);
			});
		}

		for (int i = 0; i < searchInEachURL.length; i++) {
			searchInEachURL[i].start(); // the above created 20 threads start - calls the method processQueue
		}

		for (int i = 0; i < searchInEachURL.length; i++) {
			searchInEachURL[i].join(); // waiting for all threads to complete
		}

		System.out.println("Printing results");
		PrintWriter writer = new PrintWriter(System.getProperty("user.dir") +"/results.txt", "UTF-8");

		/*
		 * removing multiple entries of url - if there are multiple occurrences of the
		 * search word in the page
		 */
		List<String> listWithoutDuplicates = new ArrayList<>(new HashSet<>(result));
		listWithoutDuplicates.forEach(url -> {
			System.out.println("==> " + url);
			writer.println(url);
		});
		writer.close();

		System.out.println("Please view the results.txt file.");

	}
}
