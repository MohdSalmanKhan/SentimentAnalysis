
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class testClass {
	
	public static TreeMap<String, Integer> support_Vector = new TreeMap<String, Integer>();
	
	public static List<String> ngrams(int n, String str) {
		List<String> ngrams = new ArrayList<String>();
		String[] words = str.split(" ");
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i+n));
		return ngrams;
	}

	public static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
	}
	public static int counts = 0;
	public static void addInMap(String str){
		
		for (int n = 1; n <= 2; n++) {
			for (String ngram : ngrams(n, str))
			{
				if(!support_Vector.containsKey(ngram))
					support_Vector.put(ngram, counts++);
				//System.out.println(ngram);
			}
			//System.out.println();
		}
		
	}


	public static void main(String[] args) {


		BufferedReader br = null;
		BufferedWriter bw = null;

		try {

			String sCurrentLine;
			int count1 = 0 ;
			br = new BufferedReader(new FileReader("/home/ubuntu/DWDM/test_1.tsv"));
			br.readLine();
			while ((sCurrentLine = br.readLine()) != null) {
				count1++;
				if(count1 == 156038)
					System.out.println(sCurrentLine + "!!!!\t" + sCurrentLine.split("\t")[2] + "!!!" );
				addInMap(sCurrentLine.split("\t")[2]);
				
				
			}
		//	System.out.println(support_Vector);
			
			int []arr = new int[support_Vector.size()+1];
			
			
			System.out.println("djfgsdjgfsdgfhjsgdhfg    " + support_Vector.size());
			
			java.util.Arrays.fill(arr, 0);
			br.close();
			
			br = new BufferedReader(new FileReader("/home/ubuntu/DWDM/test_1.tsv"));
			bw = new BufferedWriter(new FileWriter("/home/ubuntu/DWDM/test_1"));
			br.readLine();
			int count = 0 ;
			while ((sCurrentLine = br.readLine()) != null) {
				count++;
				if(count == 156038)
					System.out.println(sCurrentLine + "----");
			//	String []strArr = sCurrentLine.split("\t")[2].split(" ");
				
				String strArr[] = new String[10000] ;
				int sentenceCount = 0;
				for (int n = 1; n <= 2; n++) {
					for (String ngram : ngrams(n, sCurrentLine.split("\t")[2]))
					{
						if(count == 156038)
							System.out.println(ngram + "--sa-");
						strArr[sentenceCount++] = ngram;
					}
					//System.out.println();
				}
				
				String classLabel = sCurrentLine.split("\t")[3];
				for(int i=0; i< sentenceCount; i++)
				{
					if(support_Vector.containsKey(strArr[i]))
					{
						//System.out.println(strArr[i] + " :::  "+ support_Vector.get(strArr[i]));
						if(count == 5868)
							System.out.println("hello");
						arr[support_Vector.get(strArr[i])] = 1;
					}
				}
				bw.write(classLabel);
				for(int i=0; i<arr.length; i++)
				{
					if(arr[i]==1)
					{
						bw.write(" "+(i+1)+":1");
					}
				}
				bw.write("\n");
				java.util.Arrays.fill(arr, 0);
			}
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null){
					br.close();
					bw.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}