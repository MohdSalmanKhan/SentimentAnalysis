import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;


class ScoreAndLabel
{
	String classLabel;
	double score;
	
	public String getClassLabel() {
		return classLabel;
	}
	public void setClassLabel(String classLabel) {
		this.classLabel = classLabel;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	
	
}

public class TrainClass
{
	
	public static TreeMap<String, Integer> support_Vector = new TreeMap<String, Integer>();
	public static HashMap<String, String> wordToPOSMap = new HashMap<String, String>();
	public static int counts = 0;
	public static HashMap<String, ScoreAndLabel> sentiScoreMap = new HashMap<String, ScoreAndLabel>();
	public static SentiWordNet test = new SentiWordNet();
	public static float count_v=0;
	public static float count_a=0;
	public static float count_r=0;
	public static float total_count=0;
	
	

	public static List<String> ngrams(int n, String str)
	{
		List<String> ngrams = new ArrayList<String>();
		String[] words = str.split(" ");
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i+n));
		return ngrams;
	}

	public static String concat(String[] words, int start, int end)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
	}

	public static void addInMap(String str)
	{
		for (int n = 1; n <= 2; n++)
			for (String ngram : ngrams(n, str))
				if(!support_Vector.containsKey(ngram))
					support_Vector.put(ngram, counts++);
	}

	public static void addPosWords(){
		support_Vector.put("V", counts++); 
		support_Vector.put("A", counts++); 
		support_Vector.put("R", counts++); 
	}

	public static void addSenti(){
		support_Vector.put("#strongneg", counts++);
		support_Vector.put("#neg", counts++);
		support_Vector.put("#neutral", counts++);
		support_Vector.put("#pos", counts++);
		support_Vector.put("#strongpos", counts++);
	}

	private static void tagAndAdd(float[] arr) {
		try 
		{
			if(total_count!=0)
			{
				arr[support_Vector.get("V")] = count_v;
				arr[support_Vector.get("A")] = count_a;
				arr[support_Vector.get("R")] = count_r;
			}
			else
			{
				//System.out.println(support_Vector.get("A") + "   skdjhfskjhfdskjdh");
				arr[support_Vector.get("V")] = 0;
				arr[support_Vector.get("A")] = 0;
				arr[support_Vector.get("R")] = 0;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void negationHandler(float []arr, String sentence, LexicalizedParser lp, TokenizerFactory<CoreLabel> tokenizerFactory)
	{
		List<CoreLabel> rawWords = tokenizerFactory.getTokenizer(new StringReader(sentence)).tokenize(); 
		Tree parse = lp.apply(rawWords);
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

		ArrayList<String> negReview=new ArrayList<String>();

		for(int index=0;index<tdl.size();index++)
		{
			String relation= tdl.get(index).reln().toString();
			if(relation.equals("neg"))
			{
				negReview.add(tdl.get(index).gov().nodeString().toString());
				//System.out.println(tdl.get(index).gov().toString());

			}
		}
		for(int i = 0 ; i < negReview.size() ; i++)
		{
			double wordScore = getScore(negReview.get(i));
			String wordClass = getPolarity(wordScore);
			if(wordToPOSMap.containsKey(negReview.get(i)));
			{
				switch(wordClass)
				{
				case "#strongpos" :	 arr[support_Vector.get(wordClass)] -= wordScore;
				arr[support_Vector.get("#strongneg")] -= wordScore;
				break;
				case "#pos" : arr[support_Vector.get(wordClass)] -= wordScore;
				arr[support_Vector.get("#neg")] -= wordScore;
				break;
				case "#neg" :	 arr[support_Vector.get(wordClass)] -= wordScore;
				arr[support_Vector.get("#pos")] -= wordScore;
				break;
				case "#strongneg" :	 arr[support_Vector.get(wordClass)] -= wordScore;
				arr[support_Vector.get("#strongneg")] -= wordScore;
				break;
				}
			}
		}

	}

	public static void sentiWordAdd(String brTags, float[] arr) {
		//unigram

		try {
			String []brTags_arr = brTags.split("\t");
			if(brTags_arr[1].equals("V") || brTags_arr[1].equals("A") || brTags_arr[1].equals("R"))
			{
				if(!wordToPOSMap.containsKey(brTags_arr[0]))
					wordToPOSMap.put(brTags_arr[0], brTags_arr[1]);
				if(sentiScoreMap.containsKey(brTags_arr[0]))
				{
					ScoreAndLabel obj = sentiScoreMap.get(brTags_arr[0]);
					arr[support_Vector.get(obj.getClassLabel())] += obj.getScore();
				}	
				else
				{
					double polarityScore = getScore(brTags_arr[0]);
					String catchVal = getPolarity(polarityScore);
					//System.out.println("sdfsdfsfd:  "+catchVal+"    asjgdajsgd     "+strArr[i]);
					arr[support_Vector.get(catchVal)] += polarityScore;
					//System.out.println("word uni =" + words_arr[i]);
					ScoreAndLabel obj = new ScoreAndLabel();
					obj.setClassLabel(catchVal);
					obj.setScore(polarityScore);
					sentiScoreMap.put(brTags_arr[0], obj);
				}
				if(brTags_arr[1].equalsIgnoreCase("V"))
					count_v++;
				if(brTags_arr[1].equalsIgnoreCase("A"))
					count_a++;
				if(brTags_arr[1].equalsIgnoreCase("R"))
					count_r++;
			}

			/*	//bigram

			for(int i = 0, j= 0 ; i < (brTags_arr.length - 1) ; i++, j++)
			{
				if(brTags_arr[i].equals("V") || brTags_arr[i].equals("A") || brTags_arr[i].equals("R") || brTags_arr[i + 1].equals("V") || brTags_arr[i + 1].equals("A") || brTags_arr[i + 1].equals("R"))
				{
					//System.out.println("i="+i + "AARR[i]="+arr[i]+"   Length: "+brTags_arr.length);
					if(sentiScoreMap.containsKey(words_arr[j] + " " + words_arr[j + 1]))
						arr[support_Vector.get(sentiScoreMap.get(words_arr[j] + " " + words_arr[j + 1]))]++;
					else
					{
						double polarityScore = getScore(words_arr[j] + " " + words_arr[j + 1]);
						String catchVal = getPolarity(polarityScore);
						//System.out.println("sdfsdfsfd:  "+catchVal+"    asjgdajsgd     "+strArr[i]);
						arr[support_Vector.get(catchVal)]++;
						//System.out.println("words bi="+words_arr[i] + " " + words_arr[i + 1]);
						sentiScoreMap.put(words_arr[j] + " " + words_arr[j + 1], catchVal);
					}
				}
				if(words_arr[j].contains("'"))
					i++;
			}*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//System.out.println("brTags="+brTags);
			//e.printStackTrace();
		}
	}

	public static String getPolarity(double totalScore) {

		String polarity = "#neutral";               
		if(totalScore>0.75)
			polarity = "#strongpos";
		else if(totalScore > 0.25 && totalScore<=0.75)
			polarity = "#pos";
		else if(totalScore >= 0 && totalScore<=0.25)
			polarity = "#neutral";
		else if(totalScore < 0 && totalScore>=-0.25)
			polarity = "#neutral";
		else if(totalScore < -0.25 && totalScore>=-0.75)
			polarity = "#neg";
		else if(totalScore<-0.75)
			polarity = "#strongneg";
		return polarity;
	}

	public static double getScore(String o) {

		//SentiWordNet test = new SentiWordNet();
		String sentence=o;
		String[] words = sentence.split("\\s+"); 
		double totalScore = 0;
		for(String word : words) {
			word = word.replaceAll("([^a-zA-Z\\s])", "");
			if (test.extract(word) == null)
				continue;
			totalScore += test.extract(word);
		}
		return totalScore;
	}

	public static void main(String[] args)
	{
		System.out.println("Creating Vector..");
		long t1 = System.currentTimeMillis();
		BufferedReader br = null;
		BufferedReader br_tags = null;
		BufferedWriter bw = null;
		//for negation handling
		LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");

		if(args.length != 4){
			System.out.println("Usage: java TrainClass TraininputFileName TestInput outputFileName");
			System.exit(0);
		}
		String input_train = args[0];
		String input_test = args[1];
		String output_train = args[2];
		String output_test = args[3];
		try
		{
			String sCurrentLine;

			br = new BufferedReader(new FileReader(input_train));
			br_tags = new BufferedReader(new FileReader("/home/ubuntu/DWDM/main_data/train_tags.txt"));
			//br.readLine();
			while ((sCurrentLine = br.readLine()) != null)
			{
				sCurrentLine = sCurrentLine.toLowerCase();
				addInMap(sCurrentLine.split("\t")[0]);
			}

			addPosWords();
			addSenti();
			System.out.println("SIZE: "+support_Vector.size());
			float []arr = new float[support_Vector.size()+1];
			java.util.Arrays.fill(arr, 0);
			br.close();
			br = new BufferedReader(new FileReader(input_train));
			bw = new BufferedWriter(new FileWriter(output_train));
			//br.readLine();
			while ((sCurrentLine = br.readLine()) != null)
			{
				sCurrentLine = sCurrentLine.toLowerCase();
				//String brTags = br_tags.readLine();
				String strArr[] = new String[10000];
				int sentenceCount = 0;
				String words = sCurrentLine.split("\t")[0];
				for (int n = 1; n <= 2; n++)
				{
					for (String ngram : ngrams(n, words))
					{
						strArr[sentenceCount++] = ngram;
					}
				}
				String classLabel = sCurrentLine.split("\t")[1];
				for(int i=0; i < sentenceCount; i++)
				{
					if(support_Vector.containsKey(strArr[i]))
					{
						arr[support_Vector.get(strArr[i])] = 1;
					}
				}
				String str;
				count_a = count_r = count_v = total_count = 0;
				//System.out.println("count="+count_a);
				while(!(str=br_tags.readLine()).equals(""))
				{
					sentiWordAdd(str, arr);
				}
				tagAndAdd(arr);
				//negation handler
				if(words.toLowerCase().contains("n't"))
					words = words.replaceAll("n't", " not ");
				if(words.toLowerCase().contains(" not ") || words.toLowerCase().contains(" never ") || words.toLowerCase().contains(" nor ") 
						|| words.toLowerCase().contains(" neither ") || words.toLowerCase().contains(" no ") 
						|| words.toLowerCase().contains(" nowhere "))
					negationHandler(arr, words, lp, tokenizerFactory);

				bw.write(classLabel);
				for(int i=0; i<arr.length; i++)
					if(arr[i]!=0)
						bw.write(" "+(i+1)+":"+arr[i]);
				bw.write("\n");
				java.util.Arrays.fill(arr, 0);
			}
			bw.close();

			System.out.println("Index of v: "+support_Vector.get("V"));
			System.out.println("Index of a: "+support_Vector.get("A"));
			System.out.println("Index of r: "+support_Vector.get("R"));
			System.out.println("Index of strongpos: "+support_Vector.get("#strongpos"));
			System.out.println("Index of pos: "+support_Vector.get("#pos"));
			System.out.println("Index of neutral: "+support_Vector.get("#neutral"));
			System.out.println("Index of neg: "+support_Vector.get("#neg"));
			System.out.println("Index of strongneg: "+support_Vector.get("#strongneg"));
			//System.out.println("     gjhynjfhnjf\n\n"+sentiScoreMap);

			long t2 = System.currentTimeMillis();
			System.out.println("Feature vector created in "+(t2-t1)+" sec. Output File Name: "+output_train);





			System.out.println("Creating Vector..");
			//long t1 = System.currentTimeMillis();
			//BufferedReader br = null;
			//BufferedReader br_tags = null;
			//BufferedWriter bw = null;


			//	String sCurrentLine;

			br = new BufferedReader(new FileReader(input_test));
			br_tags = new BufferedReader(new FileReader("/home/ubuntu/DWDM/main_data/test_tags.txt"));
			/*br.readLine();
			/*while ((sCurrentLine = br.readLine()) != null)
			{
				sCurrentLine = sCurrentLine.toLowerCase();
				//addInMap(sCurrentLine.split("\t")[0]);
			}

			//addPosWords();
			//addSenti();

			//float []arr = new float[support_Vector.size()+1];
			//java.util.Arrays.fill(arr, 0);
			br.close();*/
			br = new BufferedReader(new FileReader(input_test));
			bw = new BufferedWriter(new FileWriter(output_test));
			//br.readLine();
			while ((sCurrentLine = br.readLine()) != null)
			{
				sCurrentLine = sCurrentLine.toLowerCase();
				//String brTags = br_tags.readLine();
				String strArr[] = new String[10000];
				int sentenceCount = 0;
				String words = sCurrentLine.split("\t")[0];
				for (int n = 1; n <= 2; n++)
				{
					for (String ngram : ngrams(n, words))
					{
						strArr[sentenceCount++] = ngram;
					}
				}
				String classLabel = sCurrentLine.split("\t")[1];
				for(int i=0; i < sentenceCount; i++)
				{
					if(support_Vector.containsKey(strArr[i]))
					{
						arr[support_Vector.get(strArr[i])] = 1;
					}
				}
				String str;
				count_a = count_r = count_v = total_count = 0;
				//System.out.println("count="+count_a);
				while(!(str=br_tags.readLine()).equals(""))
				{
					sentiWordAdd(str, arr);
				}
				tagAndAdd(arr);
				if(words.toLowerCase().contains("n't"))
					words = words.replaceAll("n't", " not ");
				if(words.toLowerCase().contains(" not ") || words.toLowerCase().contains(" never ") || words.toLowerCase().contains(" nor ") 
						|| words.toLowerCase().contains(" neither ") || words.toLowerCase().contains(" no ") 
						|| words.toLowerCase().contains(" nowhere "))
					negationHandler(arr, words, lp, tokenizerFactory);
				
				bw.write(classLabel);
				for(int i=0; i<arr.length; i++)
					if(arr[i]!=0)
						bw.write(" "+(i+1)+":"+arr[i]);
				bw.write("\n");
				java.util.Arrays.fill(arr, 0);
			}
			bw.close();
			br.close();
			System.out.println("Index of v: "+support_Vector.get("V"));
			System.out.println("Index of a: "+support_Vector.get("A"));
			System.out.println("Index of r: "+support_Vector.get("R"));
			System.out.println("Index of strongpos: "+support_Vector.get("#strongpos"));
			System.out.println("Index of pos: "+support_Vector.get("#pos"));
			System.out.println("Index of neutral: "+support_Vector.get("#neutral"));
			System.out.println("Index of neg: "+support_Vector.get("#neg"));
			System.out.println("Index of strongneg: "+support_Vector.get("#strongneg"));

			//System.out.println("     gjhynjfhnjf\n\n"+sentiScoreMap);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (br != null)
				{
					br.close();
					bw.close();
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}