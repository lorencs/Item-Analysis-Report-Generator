package item.analysis.report;

import java.util.*;

public class Student{
    private ArrayList<Answer> answers;
    private String id;
    private double score;

    public Student(String _id) {
    	id = _id;
    	score = -1;
    	answers = new ArrayList<Answer>();
    }

    public void addAnswer(String type, int correct, double weight, int answer){
    	answers.add(new Answer(type, correct, weight, answer));
    }
    
    public String getId(){
    	return id;
    }
    
    public ArrayList<Answer> getAnswers(){    	
    	return answers;
    }
    
    public double getMaxPossibleScore(){
    	double maxScore = 0;
    	
    	for(Answer ans : answers){
    		maxScore += ans.getWeight();
    	}
    	
    	return maxScore;
    }
    
    //calculate score, save internally and return it
    public double calculateScore(){
    	double _score = 0;
    	
    	for(Answer ans : answers){
    		if(ans.isCorrect()){
    			_score += ans.getWeight();
    		}
    	}
    	
    	score = _score;
    	
    	return _score;
    }
    
    public double getScore(){
    	if (score == -1) calculateScore();
    	
    	return score;
    }
}


