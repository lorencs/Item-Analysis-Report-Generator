package item.analysis.report;

import javax.swing.JOptionPane;

public class Answer{
	private String type;
	private int correct;
	private double weight;
	private int answer;
	
	public Answer(String _type, int _correct, double _weight, int _answer){
		type = _type;
		correct = _correct;
		weight = _weight;
		answer = _answer;
	}
	
	public int getAnswer(){
		return answer;
	}
	
	public double getWeight(){
		return weight;
	}
	
	public boolean isCorrect(){
            if (type.equals("MULTICHOICE")){
                return (answer == correct);
            } else{
                JOptionPane.showMessageDialog(null, "Unknown item type '" + type + "'", "Unkown type", JOptionPane.ERROR_MESSAGE);
            }

            return false;
	}
}