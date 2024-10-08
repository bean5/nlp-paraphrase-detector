package interTextFinder;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import fileio.DocumentSaver;
import fileio.DocumentScanner;

import CharacterData.StopWordChecker;
import NGramSet.NGramSet;
import NGramSet.NGramSetImpl;

public class InterTextualFinder
{
	protected String															paramString;
	protected static FuzzyNGramDocumentComparer<NGramSetImpl>	comparer							= new FuzzyNGramDocumentComparer<NGramSetImpl>();
	protected HashSet<NGramSet>											commonNGrams;

	protected String															primarySourcePath;
	protected String															secondarySourcePath;
	private double																minimum_score					= 0.0;
	private int																	minimumSecondaryMatches		= 1;
	private int																	windowSize						= 1;

	private boolean															matchCase						= false;
	private boolean															maximizePrimaryWindowSize	= false;
	private boolean															strictSearch					= true;
	private boolean															usePorterStemmer				= true;
	private boolean															useStopWords					= true;
	private boolean															printBestOnly					= true;

	protected ComparerStatistics											stats;
	protected double	lastRunTime;
	protected long	lastStartTime;

	/*
	 * Starts a timer, reads in files, and calls string version of function
	 */
	public void findIntertextQuotesFromFiles() throws IOException
	{
		List<String> word_list1 = DocumentScanner.tokenizeFromFile(primarySourcePath);
		List<String> word_list2 = DocumentScanner.tokenizeFromFile(secondarySourcePath);

		findIntertextQuotesGivenParamsFromTokenizedLists(word_list1, word_list2);
	}

	public void findIntertextQuotesGivenParamsFromStrings(String text_1, String text_2)
	{
		setStart();

		List<String> words1 = DocumentScanner.tokenize_string_with_punctuation(text_1);
		List<String> words2 = DocumentScanner.tokenize_string_with_punctuation(text_2);

		findIntertextQuotesGivenParamsFromTokenizedLists(words1, words2);

		setRunTime();

		setParamString();
		print();
	}

	public void findIntertextQuotesGivenParamsFromTokenizedLists(List<String> word_list1, List<String> word_list2)
	{
		setStart();

		// if (maximizePrimaryWindowSize) minimum_score = 1;
		comparer.setMatchCase(matchCase);
		comparer.setStrict(strictSearch);
		comparer.setPorterStemmerUsage(usePorterStemmer);
		comparer.setUseStopWords(useStopWords);

		commonNGrams = comparer.findCommonNGrams(word_list1, word_list2, minimum_score, windowSize,
						maximizePrimaryWindowSize);

		setRunTime();

		stats = comparer.getStats();

		filterNGrams(minimumSecondaryMatches);

		setParamString();
		print();
	}

	protected void setStart()
	{
		lastStartTime = System.currentTimeMillis();
	}

	protected void setRunTime()
	{
		double end = System.currentTimeMillis();
		lastRunTime = end - lastStartTime;
		lastRunTime /= (1000 * 6);// convert to minutes
		lastRunTime = lastRunTime / 10;
	}

	private void print()
	{
		System.out.println(paramString);
	}

	protected void setParamString()
	{
		paramString = convertParametersToString(lastRunTime, comparer.errorsToString());
	}

	/*
	 * Filters the ngrams that don't have the minimum number of secondary matches
	 */
	private void filterNGrams(int minimumSecondaryMatches)
	{
		if (minimumSecondaryMatches <= 1) return;

		HashSet<NGramSet> filteredCommonNGrams = new HashSet<NGramSet>(commonNGrams.size());

		for (NGramSet ngram : commonNGrams)
		{
			if (ngram.size() >= minimumSecondaryMatches)
				filteredCommonNGrams.add(ngram);
		}
		commonNGrams = filteredCommonNGrams;
	}

	/*
	 * Outputs the settings as a string, as well as some of the ngrams
	 */
	public String toString(String paramsAsString, HashSet<NGramSet> commonNGrams,
					boolean printBestOnly)
	{
		StringBuilder str = new StringBuilder();

		int leftCount = 0;
		int rightCount = 0;

		double minscore = 0D;
		if (printBestOnly) minscore = findBestScore();
		// System.out.println("Here" + minscore);

		for (NGramSet nGram : commonNGrams)
		{
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

	protected double findBestScore()
	{
		return stats.getHighestScore();
	}

	protected HashSet<NGramSet> getBestRightMatches()
	{
		double bestScore = findBestScore();
		HashSet<NGramSet> best = new HashSet<NGramSet>();

		for (NGramSet nGram : commonNGrams)
		{
			double bestScoreOfNGram = nGram.findBestScore();

			if (bestScoreOfNGram >= bestScore)
				best.add(nGram);
		}

		return best;
	}

	protected void filterOutNowNonOptimalMatches()
	{
		double bestScore = findBestScore();

		filterOutMatchesOfLessThanScore(bestScore);
	}

	private void filterOutMatchesOfLessThanScore(double bestScore)
	{
		HashSet<NGramSet> filteredSet = new HashSet<NGramSet>();
		for (NGramSet nGram : commonNGrams)
		{
			nGram.filterMatchesWithScoresLowerThan(bestScore);

			if (nGram.size() != 0)
				filteredSet.add(nGram);
		}
		commonNGrams = filteredSet;
	}

	/*
	 * Converts parameters and total time to string
	 */
	protected String convertParametersToString(double totalTime, String errorString)
	{
		String params = new String();

		params += sourcePathsAsString();

		params += checkBoxParamsAsString();

		params += "Fuzzy Search Parameters: " + minimum_score + "/" + windowSize + "\n";

		params += "Require at least " + minimumSecondaryMatches + " secondary matches\n";

		params += timeToCompleteAsString(totalTime);

		params += errorString;

//System.out.println(params);
		return params;
	}

	protected String timeToCompleteAsString(double totalTime)
	{
		return "Time to complete (search): " + totalTime + " minutes.\n";
	}

	protected String sourcePathsAsString()
	{
		return "Primary Source: " + primarySourcePath + "\n" + "Secondary Source: "
						+ secondarySourcePath + "\n";
	}

	protected String checkBoxParamsAsString()
	{
		String params = "";
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
		return params;
	}

	public void saveTo(String outFilePath)
	{
		DocumentSaver.saveToFile(outFilePath, toString());
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

	public void setMinimumScore(double i)
	{
		this.minimum_score = i;
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

	public void setStopWords(String string, boolean ignoreCase)
	{
		StopWordChecker.setStopWords(DocumentScanner.tokenize_string_as_delimited(string, ','), ignoreCase);
	}
}
