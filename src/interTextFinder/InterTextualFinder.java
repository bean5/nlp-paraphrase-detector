package interTextFinder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import NGramSet.NGramSet;
import NGramSet.NGramSetImpl;

public class InterTextualFinder
{
	private String															paramString;
	private static FuzzyNGramDocumentComparer<NGramSetImpl>	comparer;
	private static HashSet<NGramSet>									commonNGrams;

	private String															primarySourcePath;
	private String															secondarySourcePath;
	private int																minimumMatches					= 1;
	private int																minimumSecondaryMatches		= 1;
	private int																windowSize						= 1;

	boolean																	matchCase						= false;
	boolean																	maximizePrimaryWindowSize	= false;
	boolean																	strictSearch					= true;
	boolean																	usePorterStemmer				= true;
	boolean																	useStopWords					= true;
	boolean																	printBestOnly					= true;

	public void findIntertextQuotesGivenParamsFromStrings(String primarySourceText,
					String secondarySourceText)
	{
		// if(maximizePrimaryWindowSize) minimumMatches = 1;

		comparer = new FuzzyNGramDocumentComparer<NGramSetImpl>();
		comparer.setMatchCase(matchCase);
		comparer.setStrict(strictSearch);
		comparer.setPorterStemmerUsage(usePorterStemmer);
		comparer.setUseStopWords(useStopWords);
		commonNGrams = comparer.findCommonNGrams(primarySourceText, secondarySourceText,
						minimumMatches, windowSize, maximizePrimaryWindowSize);

		filterNGrams(minimumSecondaryMatches);
	}

	public void findIntertextQuotesFromFiles()
	{
		double start = System.currentTimeMillis();

		File[] fileList =
		{ new File(primarySourcePath), new File(secondarySourcePath) };

		List<String> files = new ArrayList<String>(2);

		readInFiles(fileList, files);

		findIntertextQuotesGivenParamsFromStrings(files.get(0), files.get(1));

		double end = System.currentTimeMillis();
		double totalTime = end - start;
		totalTime /= (1000 * 6);// convert to minutes
		totalTime = totalTime / 10;

		paramString = convertParametersToString(totalTime, comparer.errorsToString());
	}

	private void filterNGrams(int minimumSecondaryMatches)
	{
		HashSet<NGramSet> filteredCommonNGrams = new HashSet<NGramSet>(commonNGrams.size());

		for (NGramSet ngram : commonNGrams)
		{
			if (ngram.size() >= minimumSecondaryMatches)
			{
				filteredCommonNGrams.add(ngram);
			}
		}
		commonNGrams = filteredCommonNGrams;
	}

	public static String toString(String paramsAsString, HashSet<NGramSet> commonNGrams,
					boolean printBestOnly)
	{
		StringBuilder str = new StringBuilder();

		int leftCount = 0;
		int rightCount = 0;

		int minscore = 1;
		if (printBestOnly) minscore = findBest(commonNGrams);

		int matchCount = 0;

		Iterator<NGramSet> itr = commonNGrams.iterator();

		// for(NGramSet n : commonNGrams) {
		while (itr.hasNext())
		{
			NGramSet nGram = itr.next();
			// str.append(nGram.toString());
			if (nGram.hasMatchesOfAtLeastScore(minscore))
			{
				str.append(nGram.toStringAtLeast(minscore));
				str.append("\n\n");
				leftCount++;
				rightCount += nGram.countMatchesOfAtLeastScore(minscore);
			}
		}

		String stringToSaveToFile = paramsAsString + "Left Match Count: " + leftCount + "\n"
						+ "Right Match Count: " + rightCount + "\n\n" + str.toString();

		return stringToSaveToFile;
	}

	private static int findBest(HashSet<NGramSet> commonNGrams)
	{
		int best = 1;

		Iterator<NGramSet> itr = commonNGrams.iterator();

		// for(NGramSet n : commonNGrams) {
		while (itr.hasNext())
		{
			NGramSet nGram = itr.next();
			// str.append(nGram.toString());

			int bestScoreOfNGram = nGram.findBestScore();

			if (bestScoreOfNGram > best) best = bestScoreOfNGram;
		}

		return best;
	}

	private String convertParametersToString(double totalTime, String errorString)
	{
		String params = new String();

		params += "Primary Source: " + primarySourcePath + "\n";
		params += "Secondary Source: " + secondarySourcePath + "\n";

		params += "Match Case: ";
		if (matchCase)
			params += "Yes" + "\n";
		else params += "No" + "\n";

		params += "Use Porter Stemmer: ";
		if (usePorterStemmer)
			params += "Yes" + "\n";
		else params += "No" + "\n";

		params += "Strict Search: ";
		if (strictSearch)
			params += "Yes" + "\n";
		else params += "No" + "\n";

		params += "Use Stop Words: ";
		if (useStopWords)
			params += "Yes " + comparer.toString() + "\n";
		else params += "No" + "\n";

		params += "Maximize Primary Window Size: ";
		if (maximizePrimaryWindowSize)
			params += "Yes" + "\n";
		else params += "No" + "\n";

		params += "Print Best Scores Only: ";
		if (printBestOnly)
			params += "Yes" + "\n";
		else params += "No" + "\n";

		params += "Fuzzy Search Parameters: " + minimumMatches + "/" + windowSize + "\n";

		params += "Require at least " + minimumSecondaryMatches + " secondary matches\n";

<<<<<<< HEAD
		params += "Time to complete: " + totalTime + " minutes.\n";
=======
		params += "Time to complete (search): " + totalTime + " minutes.\n";
>>>>>>> version8.1

		params += errorString;

		return params;
	}

	private static void readInFiles(File[] fileList, List<String> files)
	{
		// TODO
		System.out.println("Consider making everything compatible with unicode.\n");
		for (File f : fileList)
		{
			FileInputStream fis = null;
			// InputStreamReader in = null;

			try
			{
				fis = new FileInputStream(f);
				if (fis != null)
				{
					// in = new InputStreamReader(fis, "UTF-8");
					String newLine = read(f.toString(), "UTF-8");
					// String newLine = read(f.toString(), "unicode");

					files.add(newLine);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (FileNotFoundException e1)
			{
				e1.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private static String read(String filename, String fEncoding) throws IOException
	{
		File fFilename = new File(filename);

		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(fFilename), fEncoding);
		try
		{
			while (scanner.hasNextLine())
			{
				text.append(scanner.nextLine() + NL);
			}
		}
		finally
		{
			scanner.close();
		}
		return text.toString();
	}

	public void saveTo(String outFilePath)
	{
		try
		{
			// Create file
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(toString());
			// Close the output stream
			out.close();
		}
		catch (Exception e)
		{// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public String toString()
	{
		if (paramString == null || commonNGrams == null)
			return "No data. ";
		else return toString(paramString, commonNGrams, printBestOnly).replaceAll("\n",
						System.getProperty("line.separator"));
	}

	// setters
	public void setPrimaryPath(String path)
	{
		this.primarySourcePath = path;
	}

	public void setSecondaryPath(String path)
	{
		this.secondarySourcePath = path;
	}

	public void setMinimumMatches(int i)
	{
		this.minimumMatches = i;
	}

	public void setWindowSize(int i)
	{
		this.windowSize = i;
	}

	public void setMatchCase(boolean selected)
	{
		this.matchCase = selected;
	}

	public void setStrictness(boolean selected)
	{
		this.strictSearch = selected;
	}

	public void setUsePorterStemmer(boolean selected)
	{
		this.usePorterStemmer = selected;
	}

	public void setMinimumSecondaryMatches(int i)
	{
		this.minimumSecondaryMatches = i;
	}

	public void setMaximizePrimaryWindow(boolean selected)
	{
		this.maximizePrimaryWindowSize = selected;
	}

	public void setUseStopWords(boolean selected)
	{
		this.useStopWords = selected;
	}

	public void setPrintBestScoresOnly(boolean selected)
	{
		this.printBestOnly = selected;
	}

}
