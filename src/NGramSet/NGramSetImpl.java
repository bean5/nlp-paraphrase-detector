package NGramSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class NGramSetImpl implements NGramSet
{
	protected double							minScore				= 0.0D;
	public int									maxSize;
	protected static boolean				matchCase			= false;
	protected static boolean				useStopWords		= true;
	protected static boolean				useSrictMatching	= true;

	protected int								position				= 0;
	protected int								totalCount			= 0;

	protected List<String>					document;

	protected List<String>					words;
	protected List<String>					modifiedWords;

	protected HashMap<String, Integer>	wordCounts;
	protected HashMap<NGramSet, Double>	matches;
	protected TreeMap<Double, Integer>	ordered_scores;
	private double								score					= 0.0D;

	public NGramSetImpl(int size)
	{
		initialize(size);
	}

	public NGramSetImpl(NGramSet other)
	{
		initialize(other.getMaxSize());

		document = other.getDocument();
		position = 0;

		for (String word : other.getModifiedWordList())
		{
			processModifiedWord(word);
		}
		position = other.getPosition();
	}

	protected void initialize(int size)
	{
		words = new ArrayList<String>(size);
		modifiedWords = new ArrayList<String>(size);
		wordCounts = new HashMap<String, Integer>(size);
		matches = new HashMap<NGramSet, Double>();
		this.maxSize = size;
		ordered_scores = new TreeMap<Double, Integer>();
	}

	public String processWord(String word)
	{
		String modifiedWord = modifyWord(word);

		return processModifiedWord(modifiedWord);
	}

	protected String modifyWord(String word)
	{
		String modifiedWord;
		if (!matchCase)
		{
			modifiedWord = new String(word).toLowerCase();
		}
		else
		{
			modifiedWord = word;
		}
		return modifiedWord;
	}

	protected String processModifiedWord(String word)
	{
		addProcessedWord(word);
		incrementPosition();

		return incrementWordCount(word);
	}

	protected void addProcessedWord(String stemmedWord)
	{
		modifiedWords.add(stemmedWord);
	}

	public int consume(HashMap<String, List<NGramSet>> map)
	{
		return consume(map, wordCounts);
	}

	protected int consume(HashMap<String, List<NGramSet>> map, HashMap<String, Integer> wordCounts)
	{
		// assert (minScore != 0);
		assert (maxSize != 0);
		assert (score == 0.0D);

		// System.out.print("\nSearching for matches for:\n\n" + toString());

		HashMap<NGramSet, Integer> potentialMatches = new HashMap<NGramSet, Integer>();

		// accumulate all possible matches
		// non-strict matching
		if (useSrictMatching)
		{
			for (Entry<String, Integer> e : wordCounts.entrySet())
			{
				String s = e.getKey();

				List<NGramSet> oneWordMatches = map.get(s);

				if (oneWordMatches == null) continue;

				Set<NGramSet> oneWordMatchesUnique = new HashSet<NGramSet>(oneWordMatches);

				assert (oneWordMatches != null);
				assert (oneWordMatches.size() > 0);
				assert (oneWordMatchesUnique.size() > 0);
				assert (oneWordMatchesUnique.size() <= oneWordMatches.size());

				if (oneWordMatches != null)
				{
					for (NGramSet possibleMatchNGram : oneWordMatchesUnique)
					{
						int currentCount = 0;
						int increment = 1;

						if (potentialMatches.containsKey(possibleMatchNGram))
						{
							currentCount = potentialMatches.get(possibleMatchNGram);
						}
						increment = (e.getValue() > possibleMatchNGram.getCountOfWord(s)) ? possibleMatchNGram
										.getCountOfWord(s) : e.getValue();

						increment += currentCount;

						potentialMatches.put(possibleMatchNGram, increment);
					}
				}
			}
		}
		else
		{
			// non-STRICT matching
			for (Entry<String, Integer> e : wordCounts.entrySet())
			{
				String s = e.getKey();

				for (int j = e.getValue(); j > 0; j--)
				{
					List<NGramSet> oneWordMatches = map.get(s);

					if (oneWordMatches == null) break;

					for (NGramSet possibleMatchNGram : oneWordMatches)
					{
						int currentCount = 0;
						int increment = 1;

						if (potentialMatches.containsKey(possibleMatchNGram))
						{
							currentCount = potentialMatches.get(possibleMatchNGram);
						}
						increment += currentCount;
						increment = (increment > maxSize) ? maxSize : increment;

						potentialMatches.put(possibleMatchNGram, increment);
					}
				}
			}
		}

		// accumulate matches that meet minimum requirement
		int c = 0;
		for (Entry<NGramSet, Integer> e : potentialMatches.entrySet())
		{
			NGramSet set = e.getKey();
			// set.computeScore(e.getValue());
			set.computeScore();
			if (set.getScore() >= minScore)
			{
				int count = 1;
				if (ordered_scores.containsKey(set.getScore()))
				{
					count += ordered_scores.get(set.getScore());
				}
				ordered_scores.put(set.getScore(), count);

				matches.put(set, set.getScore());

				// System.out.println("Found Match: " + e.getKey().toString());
				// potentialMatches.remove(e.getValue());
				c++;
			}
			if (set.getScore() > score) score = set.getScore();
		}

		// if(c > 0) System.out.println("Size: " + c);
		return c;
	}

	public void filterMatchesWithScoresLowerThan(double bestScore)
	{
		HashMap<NGramSet, Double> filteredMap = new HashMap<NGramSet, Double>();
		for (Entry<NGramSet, Double> e : matches.entrySet())
		{
			if (e.getKey().getScore() >= bestScore) filteredMap.put(e.getKey(), e.getValue());
			// if (e.getKey().getScore() < bestScore)
			// matches.remove(e.getValue());
		}
		matches = filteredMap;
	}

	protected void incrementPosition()
	{
		assert (position < document.size());
		position++;

		// System.out.println("Position: " + position);
		// if(position - maxSize > 0)
		// System.out.println("New Word: " + document.get(position - maxSize));
	}

	protected String incrementWordCount(String word)
	{
		if (isStopWord(word)) return null;

		if (wordCounts.containsKey(word))
		{
			wordCounts.put(word, wordCounts.get(word) + 1);
		}
		else
		{
			wordCounts.put(word, 1);
		}

		totalCount++;

		return word;
	}

	public void popFirstWord()
	{
		// assert(words.size() > 0);
		assert (modifiedWords.size() > 0);
		assert (wordCounts != null);
		// assert(words.get(0).equals(document.get(position - maxSize)));

		String firstWord = modifiedWords.get(0);

		decrementWordCount(firstWord);

		// words.remove(0);
		modifiedWords.remove(0);
	}

	protected void decrementWordCount(String word)
	{
		assert (wordCounts != null);
		if (isStopWord(word)) return;

		assert (wordCounts.get(word) >= 1);
		assert (wordCounts.containsKey(word));

		if (wordCounts.get(word) > 1)
		{
			wordCounts.put(word, wordCounts.get(word) - 1);
		}
		else
		{
			wordCounts.remove(word);
		}
	}

	public int getCountOfWord(String key)
	{
		if (wordCounts.containsKey(key)) { return wordCounts.get(key); }
		return 0;
	}

	public boolean isStopWord(String word)
	{
		if (useStopWords == false) return false;
		return word.equals("The") || word.equals("the") || word.equals("And") || word.equals("and")
						|| word.equals("Of") || word.equals("of") || word.equals("That")
						|| word.equals("that") || word.equals("To") || word.equals("to")
						|| word.equals("They") || word.equals("they") || word.equals("Unto")
						|| word.equals("unto") || word.equals("I") || word.equals("i")
						|| word.equals("He") || word.equals("he") || word.equals("It")
						|| word.equals("it");
	}

	public String toString()
	{
		StringBuilder st = new StringBuilder();
		st.append("Primary Match (size: " + maxSize + "): " + leftToString() + "\n");

		appendRightToStringBuilder(st);

		return st.toString();
	}

	protected void appendRightToStringBuilder(StringBuilder st)
	{
		for (Entry<NGramSet, Double> e : matches.entrySet())
		{
			NGramSetImpl set = (NGramSetImpl) e.getKey();

			st.append("Secondary Match [" + set.getTotalCount() + " words match of "
							+ set.getMaxSize() + " (" + set.getScore() + ")]:" + set.leftToString());
			st.append("\n");
		}
	}

	private void appendRightToStringBuilderAtLeast(StringBuilder st, double minscore)
	{
		for (Entry<NGramSet, Double> e : matches.entrySet())
		{
			if (e.getKey().getScore() < minscore) continue;
			NGramSetImpl set = (NGramSetImpl) e.getKey();

			st.append("Secondary Match [" + e.getValue() + " (" + set.getScore() + ")"
							+ " words match]:" + set.leftToString());
			st.append("\n");
		}
	}

	public String leftToString()
	{
		StringBuilder st = new StringBuilder();
		st.append(" Position: " + (position - maxSize) + "/" + document.size());
		st.append(' ');

		st.append(' ');
		st.append(' ');
		for (int i = position - maxSize; i < position; i++)
		{
			assert (i < document.size());
			st.append(document.get(i));
			st.append(' ');
		}
		// st.append("|||| ");
		//
		// for(String word : words) {
		// st.append(word);
		// st.append(' ');
		// }

		st.append("\tbasis [ ");
		for (String word : modifiedWords)
		{
			st.append(word);
			st.append(' ');
		}
		st.append("]");

		return st.toString();
	}

	public boolean hasMatches()
	{
		return matches.entrySet().size() > 0;
	}

	// public List<String> getWordList() {return words;}
	public List<String> getModifiedWordList()
	{
		return modifiedWords;
	}

	public boolean getStrictness()
	{
		return STRICT;
	}

	public static void setMatchCase(boolean mc)
	{
		matchCase = mc;
	}

	public int size()
	{
		return matches.entrySet().size();
	}

	public void computeScore(int value)
	{
		this.score = (double) value / (double) maxSize;
	}

	public void setMinScore(double d)
	{
		minScore = d;
	}

	public void setMaxSize(int size)
	{
		maxSize = size;
	}

	public static void setUseStopWords(boolean USESTOPWORDS)
	{
		useStopWords = USESTOPWORDS;
	}

	public void setDocument(List<String> words)
	{
		document = words;
	}

	public int getPosition()
	{
		return position;
	}

	public double getMinScore()
	{
		return minScore;
	}

	public int getMaxSize()
	{
		return maxSize;
	}

	public List<String> getDocument()
	{
		return document;
	}

	// public int hashCode() {return wordCounts.hashCode();}
	public static void setStrictness(boolean STRICT)
	{
		useSrictMatching = STRICT;
	}

	public double findBestScore()
	{
		 return score;
//		if (ordered_scores.size() == 0) return -0.0D;
//
//		return ordered_scores.lastKey();
	}
	public double lowestScore()
	{
		if (ordered_scores.size() == 0) return -0.0D;
		return ordered_scores.firstKey();
	}

	public boolean hasMatchesOfAtLeastScore(double minscore)
	{
		return ordered_scores.ceilingKey(minscore) != null;
	}

	public String toStringAtLeast(double minscore)
	{
		StringBuilder st = new StringBuilder();
		st.append("Primary Match: " + leftToString() + "\n");

		appendRightToStringBuilderAtLeast(st, minscore);

		return st.toString();
	}

	public int countMatchesOfAtLeastScore(double minscore)
	{
		int count = 0;
		for (Entry<NGramSet, Double> e : matches.entrySet())
		{
			if (e.getValue() >= minscore) count++;
		}

		return count;
	}

	public TreeMap<Double, Integer> getScores()
	{
		return ordered_scores;
	}

	public void computeScore()
	{
		this.score = (double) totalCount / (double) maxSize;
	}

	public double getScore()
	{
		return score;
	}

	public int getTotalCount()
	{
		return totalCount;
	}
}
