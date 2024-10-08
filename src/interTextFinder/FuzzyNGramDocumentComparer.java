package interTextFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.List;

import NGramSet.NGramSet;
import NGramSet.NGramSetImplStemmed;
import NGramSet.NGramSetImpl;

public class FuzzyNGramDocumentComparer<T1 extends NGramSet> implements DocumentCommonalityFinder
{
	protected boolean					isTesting			= false;
	protected boolean					STRICT				= false;
	protected boolean					usePorterStemmer	= false;
	protected boolean					matchCase			= true;
	protected int						totalRightMatches	= 0;
	private boolean					USESTOPWORDS		= true;
	private List<Error>				errors;

	protected ComparerStatistics	stats;

	public HashSet<NGramSet> findCommonNGrams(List<String> words1, List<String> words2,
					double min_score, int max, boolean maximizePrimaryWindowSize)
	{
		stats = new ComparerStatistics();
		errors = new ArrayList<Error>();
		HashSet<NGramSet> NGramsWithMatches = new HashSet<NGramSet>();

		NGramSetImpl.setMatchCase(matchCase);
		NGramSetImpl.setUseStopWords(USESTOPWORDS);
		NGramSetImpl.setStrictness(STRICT);

		int leftMax = (words1.size() <= max || maximizePrimaryWindowSize) ? words1.size() : max;
		int rightMax = (words2.size() <= max) ? words2.size() : max;

		HashMap<String, List<NGramSet>> map = new HashMap<String, List<NGramSet>>();

		if (rightMax < max)
		{
			logError("Window size greater than number of length of secondary text; decreasing secondary window size to: "
							+ rightMax);
		}
		if (leftMax < max && maximizePrimaryWindowSize)
		{
			logError("Maximizing primary window");
		}
		else if (leftMax < max)
		{
			logError("Max out of range for primary source.  Scaling down to: " + leftMax);
		}

		ArrayList<NGramSet> nGrams1 = null;

		nGrams1 = getAllNGramsOfSize(words1, min_score, leftMax, null);

		getAllNGramsOfSize(words2, min_score, rightMax, map);

		findAllCommon(NGramsWithMatches, nGrams1, map);

		return NGramsWithMatches;
	}

	private boolean maxSizeOutOfRangeForSource(int max, List<String> words2)
	{
		return max > words2.size();
	}

	private void logError(String message)
	{
		if (errors == null)
			System.out.println("Error list does not exist; aborting log of error: " + message);

		else
		{
			errors.add(new Error(message));
			System.out.println(message);
		}
	}

	protected void findAllCommon(HashSet<NGramSet> NGramsWithMatches, ArrayList<NGramSet> nGrams,
					HashMap<String, List<NGramSet>> map)
	{
		totalRightMatches = 0;
		for (NGramSet set : nGrams)
		{
			totalRightMatches += set.consume(map);
			if (set.hasMatches())
			{
				updateScores(set.getScores());
				stats.incrementLeftCount();
				stats.setRightCount(set.size());

				NGramsWithMatches.add(set);
			}
		}
	}

	private void updateScores(TreeMap<Double, Integer> scores)
	{
		stats.ordered_scores.putAll(scores);
	}

	private ArrayList<NGramSet> getAllNGramsOfSize(List<String> words, double min_score,
					int window_size, HashMap<String, List<NGramSet>> map)
	{
		String processedWord = null;
		ArrayList<NGramSet> sets = new ArrayList<NGramSet>(words.size());
		int documentSize = words.size();

		if (usePorterStemmer)
		{
			NGramSetImplStemmed current = new NGramSetImplStemmed(window_size);
			current.setMinScore(min_score);
			current.setMaxSize(window_size);
			current.setDocument(words);

//			assert(window_size == documentSize);

			for (int i = 0; i < window_size && i < documentSize; i++)
			{
				processedWord = current.processWord(words.get(i));

				// if map is null, then tracking doesn't matter
				// if processWord was null, it was a stop-original
				if (map != null && processedWord != null)
				{
					// System.out.println("Mapping: " + current.toString() + " for "
					// + processedWord);
					List<NGramSet> nGrams = map.get(processedWord);
					if (nGrams != null)
					{
						final int prevSize = nGrams.size();

						nGrams.add(current);

						assert (prevSize != map.get(processedWord).size());
					}
					else
					{
						List<NGramSet> l = new ArrayList<NGramSet>();
						l.add(current);
						map.put(processedWord, l);
					}
				}
			}
			sets.add(current);

			NGramSetImplStemmed prev = current;
			for (int i = window_size; i < documentSize; i++)
			{
				current = new NGramSetImplStemmed((NGramSet) prev);

				processedWord = current.processWord(words.get(i));
				current.popFirstWord();

				sets.add(current);
				prev = current;

				if (map == null || processedWord == null) continue;

				List<String> relevantWords = current.getModifiedWordList();
				for (String relevantWord : relevantWords)
				{
					List<NGramSet> nGrams = map.get(relevantWord);
					if (nGrams != null)
					{
						nGrams.add(current);
					}
					else
					{
						List<NGramSet> l = new ArrayList<NGramSet>();
						l.add(current);
						map.put(relevantWord, l);
					}
				}
			}
		}
		else
		{
			NGramSetImpl current = new NGramSetImpl(window_size);

			current.setDocument(words);

			for (int i = 0; i < window_size && i < documentSize; i++)
			{
				processedWord = current.processWord(words.get(i));
				if (map != null && processedWord != null)
				{
					List<NGramSet> nGrams = map.get(processedWord);
					if (nGrams != null)
					{
						nGrams.add(current);
					}
					else
					{
						List<NGramSet> l = new ArrayList<NGramSet>();
						l.add(current);
						map.put(processedWord, l);
					}
				}
			}
			sets.add(current);

			NGramSetImpl prev = current;
			for (int i = window_size; i < documentSize; i++)
			{
				current = new NGramSetImpl((NGramSet) prev);

				processedWord = current.processWord(words.get(i));
				current.popFirstWord();

				sets.add(current);
				prev = current;

				if (map == null || processedWord == null) continue;

				List<String> relevantWords = current.getModifiedWordList();
				for (String relevantWord : relevantWords)
				{
					List<NGramSet> nGrams = map.get(relevantWord);
					if (nGrams != null)
					{
						nGrams.add(current);
					}
					else
					{
						List<NGramSet> l = new ArrayList<NGramSet>();
						l.add(current);
						map.put(relevantWord, l);
					}
				}
			}
		}
		// if(map != null) System.out.println("Map size: " +
		// map.entrySet().size());
		return sets;
	}

	public void setStrict(boolean strictness)
	{
		STRICT = strictness;
	}

	public void setPorterStemmerUsage(boolean usePorterStemmer)
	{
		this.usePorterStemmer = usePorterStemmer;
	}

	public void setMatchCase(boolean matchCase)
	{
		this.matchCase = matchCase;
	}

	public String toString()
	{
		return new String("");
	}

	public void setUseStopWords(boolean useStopWords)
	{
		USESTOPWORDS = useStopWords;
	}

	public String errorsToString()
	{
		if (errors == null || errors.size() == 0) return "";

		String errorString = "\nNotes:\n";
		for (Error e : errors)
		{
			errorString += e.getMessage() + "\n";
		}

		return errorString + "\n\n";
	}

	public ComparerStatistics getStats()
	{
		return stats;
	}
}
