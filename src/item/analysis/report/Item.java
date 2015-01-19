package item.analysis.report;

import java.util.*;

public class Item{
    private ArrayList<Integer> answers;
    private ArrayList<Double> scores;
    private ArrayList<Integer> corrects;
    private Map<Integer,Integer> optionCounts;
    private String id;
    private String type;
    private int correct;
    private double weight;
    private double variance;
    public int countAnswered;
    private int highestOption;
    private double difficulty;
    private int modeChoice;
    private double pbis;

    public Item(String _id, String _type, int _correct, double _weight) {
    	id = _id;
    	type = _type;
    	correct = _correct;
    	weight = _weight;
    	variance = -0.01;
    	answers = new ArrayList<Integer>();
    	scores = new ArrayList<Double>();
    	corrects = new ArrayList<Integer>();
    	optionCounts = new HashMap<Integer,Integer>();
    	countAnswered = 0;
    	highestOption = 0;
    	difficulty = -1;
    	modeChoice = -1;
        pbis = -2;
    	
    	if (weight == 0) variance = 0;
    }

    public void addAnswer(int answer){
    	answers.add(answer);
    	
    	//score the answer (with weight), save 1 or 0 for wether it is correct
    	if (answer == correct){
            scores.add(weight);
            corrects.add(1);
        } else{
            scores.add(0.0);
            corrects.add(0);
    	}
    	
    	//update highest option
    	if (answer > highestOption) highestOption = answer;
    	
    	//if answer nto blank, update option count
    	if (answer != -1 && answer != 0){
            countAnswered++;
            if (!optionCounts.containsKey(answer)) optionCounts.put(answer, 0);

            optionCounts.put(answer, optionCounts.get(answer)+1);
    	}
    }

	public String getId() {
            return id;
	}

	public String getType() {
            return type;
	}

	public int getCorrect() {
            return correct;
	}

	public double getWeight() {
            return weight;
	}
	
	public int getHighestOption() {
            return highestOption;
	}
	
        public Map<Integer,Integer> getOptionCounts(){
            return optionCounts;
        }
        
	public double getMean(){
            double scoreSum = 0;

            for(double s : scores)
                scoreSum += s;		

            return scoreSum/scores.size();
	}
	
	public double calculateVariance(){
            double mean = getMean();
            double temp = 0;

            for(double s : scores)
                temp += (mean-s)*(mean-s);
        
            variance = temp/scores.size();

            return variance;
	}
	
	public double getVariance(){
            if (variance == -0.01) calculateVariance();
    	
            return variance;
	}
	
	public int getCountAnswered(){
            return countAnswered;
	}
	
	public double getDifficulty(){
            if (difficulty != -1) return difficulty;

            double correctSum = 0;
            for(double c : corrects)
                correctSum += c;

            difficulty = correctSum/corrects.size();

            return difficulty;
	}
	
	public int getModeChoice(){
            if (modeChoice != -1) return modeChoice;

            int modeCount = 0;
            modeChoice = 0;

            for(int i = 1; i < highestOption+1; i++){
                if (!optionCounts.containsKey(i)) continue;
                int iCount = optionCounts.get(i);
                if (iCount > modeCount){
                    modeCount = iCount;
                    modeChoice = i;
                }
            }

            return modeChoice;
	}
	
	public int getOptionCount(int opt){
            if (!optionCounts.containsKey(opt)) return 0;
            return optionCounts.get(opt);
	}
	
	//calculate the point biserial discrimination for the item,
	//given the array of total student scores ordered in the same way
	//as the score arraylist in item
	public double getPointBisDisc(double meanScore, double stdDev, ArrayList<Student> students){
            if (pbis != -2) return pbis;
            
            double _pbis = 0;
            double mean = meanScore;
            double meanCorrect = 0;
            double p = this.getDifficulty();
            double q = 1-p;
            
            //get mean correct
            int i = 0;
            double totalScoreSum = 0;
            int scoreCount = 0;
            for(double s : scores){
                if (s != 0){
                    totalScoreSum += students.get(i).getScore();
                    scoreCount++;
                }
                
                i++;
            }
            
            meanCorrect = totalScoreSum/scoreCount;

            _pbis = (meanCorrect - mean)/stdDev;
            
            if(q != 0)
                _pbis *= Math.sqrt(p/q);
            else
                _pbis *= Math.sqrt(p);
            
            this.pbis = _pbis;
            return _pbis;
	}
}
