package interTextFinder;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import NGramSet.NGramSet;

public class InterTextualFinderLooper extends InterTextualFinder
{
	TreeMap<Integer, HashSet<NGramSet>>	results	= new TreeMap<Integer, HashSet<NGramSet>>();
	double best_score = 0.0D;

	public InterTextualFinderLooper()
	{
		super();
	}

	public void findIntertextQuotesGivenParamsFromTokenizedLists(List<String> words1, List<String> words2)
	{
		best_score = 0D;
		setStart();

		// double min_score = 0.0D;
		double min_score = 0.3D;
		final int max = (words1.size() > words2.size()) ? words1.size() : words2.size();
		for (int i = max; i > 1; i--)
		{
			System.out.println("\n\nAttempting size: " + i);

			super.setMinimumScore(min_score);

			super.setWindowSize(i);

			super.findIntertextQuotesGivenParamsFromTokenizedLists(words1, words2);

			super.filterOutNowNonOptimalMatches();

			if (commonNGrams.size() > 0)// && (stats.getHighestScore() == 1 || stats.get_left_count() < 10 && stats.get_right_count() < 10))
				results.put(i, super.commonNGrams);

			best_score = findBestScore();

			if (best_score > min_score)
				min_score = best_score;

			System.out.println("Number of matches: " + getBestRightMatches().size());
			if (best_score == 1.0D) break;
		}

		String all = "";
		for (Entry<Integer, HashSet<NGramSet>> e : results.entrySet())
		{
			// System.out.println("Number of matches: " + e.getValue().size());

			all += "\nNumber of primary matches: " + e.getValue().size() + "\n";

			for (NGramSet set : e.getValue())
			{
				all += set.toString() + "\n\n";
			}
		}

		// all += "\nNumber of matches: " + count + " out of " + max +
		// " searches\n";
		// System.out.println("Number of matches: " + count + " out of " + max +
		// " searches");

		setRunTime();

		setParamString(all);

		// System.out.println(super.paramString);
	}

	protected void setParamString(String all)
	{
		super.paramString = convertParametersToStringWithoutWindowParameters(lastRunTime,
						InterTextualFinder.comparer.errorsToString()) + all;
	}

	private String convertParametersToStringWithoutWindowParameters(double totalTime,
					String errorsToString)
	{
		String params = "";

		params += sourcePathsAsString();

		params += checkBoxParamsAsString();

		params = timeToCompleteAsString(totalTime);

		// params += errorString;

		return params;
	}

	public String toString()
	{
		return super.paramString + "\n" + super.toString();// + "Higest Score: " + best_score;
	}
}
