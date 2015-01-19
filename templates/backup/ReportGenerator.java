package item.analysis.report;

import au.com.bytecode.opencsv.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.awt.Desktop;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

//imports for jasper reports
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.*;
import net.sf.jasperreports.engine.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class ReportGenerator{
        private static String[] args;
	private static String filename;
        private static String courseTitle;
	private static double passingPercent;
	private static double passingScore;
	private static double maxPossibleScore;
	private static ArrayList<Item> items;
	private static ArrayList<Student> students;
        	
	public ReportGenerator(String[] _args){
            args = _args;
        }
        
        public void setArgs(String[] _args){
            args = _args;
        }
        
	public void generateReport (boolean generateReport, JComboBox cbox, JFreeChart chart, JPanel chartPanel) throws FileNotFoundException, IOException{	
            filename = args[0].toString();
            
            if (filename.equals("")){
                JOptionPane.showMessageDialog(null, "Please choose a .csv file", "Missing file", JOptionPane.ERROR_MESSAGE);
                return;
            }
            //test file
            File f = new File(filename);
            if(!f.exists()){
                JOptionPane.showMessageDialog(null, "File '" + filename + "'doesn't exist", "File doesn't exist", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String[] fn = filename.split("\\.");
            if (!fn[1].equals("csv")){
                JOptionPane.showMessageDialog(null, "Invalid file format '" + fn[1] + "'. Please choose a .csv file", "Invalid file", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (args[1].equals("")){
                JOptionPane.showMessageDialog(null, "Please enter a passing grade", "Missing grade", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try{
                passingPercent  = Double.parseDouble(args[1]);
                if (passingPercent < 1 ){
                    JOptionPane.showMessageDialog(null, "Please enter passing percent from 1-100", "Invalid grade", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                if (passingPercent > 100 ){
                    JOptionPane.showMessageDialog(null, "Please enter passing percent from 1-100", "Invalid grade", JOptionPane.WARNING_MESSAGE);
                    return;
                }

            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Please enter a valid passing grade (integer or decimal)", "Invalid grade", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            //initialize arrays
            items = new ArrayList<Item>();
            students = new ArrayList<Student>();

            CSVReader reader = new CSVReader(new FileReader(filename));
            int lineCount = 1;
		
	    String [] line;
            String titleArray[];
	    while ((line = reader.readNext()) != null) {
                if (lineCount == 1){                                            //read course title on first line first row
                    titleArray = line[0].split("\\-");
                    courseTitle = titleArray[0];
                } else if (lineCount < 6){						//skip first 5 lines
		        lineCount++;
	    		continue;	
	    	} else if(lineCount == 6){					//scan in the student ids
	    		int rowCount = 1;
	    		
	    		for(String studentId : line){
	    			if (rowCount < 6){			//skip first 6 rows
	    				rowCount++;
	    				continue;
	    			} else {
	    				if (studentId.equals("")){	//dont scan in blank column
	    					rowCount++;
	    					continue;
	    				}
	    				students.add(new Student(studentId));
	    			}
	    			rowCount++;
	    		}
	    	} else if (lineCount > 6){					//scan in all the items and answers
	    		String itemId = line[0];
	    		String itemType = line[2];
	    		if (!checkType(itemType)) return;
	    		int correct = Integer.parseInt(line[3]);
	    		double weight = Double.parseDouble(line[4]);
	    		Item newItem = new Item(itemId, itemType, correct, weight);	    		
	    		
	    		//scan in answer for each student for that item
	    		for(int i = 5; i < students.size()+5; i++){
	    			int answer;
	    			if (!line[i].equals(""))
	    				answer = Integer.parseInt(line[i]);
	    			else
	    				answer = -1;	    				
	    			
	    			if (answer == 0) answer = -1;
	    			
	    			students.get(i-5).addAnswer(itemType, correct, weight, answer);
	    			newItem.addAnswer(answer);
	    		}
	    		
	    		items.add(newItem);
	    	}
	    	
	    	
	        lineCount++;
	    }
	    
	    //TEMPORARY CSV OUTPUT #############################################
	    generatePdfReport(generateReport, cbox, chart, chartPanel);
	   
	}
	
        public void generatePdfReport(boolean doGenerateReport, JComboBox itemChartSelector, JFreeChart chart, JPanel chartPanel) {
            HashMap<String,Object> parameters = new HashMap<>();
            String[] fn = filename.split("\\\\");
            fn = fn[fn.length-1].split("\\.");
            String outFilename = fn[0] + "-out.pdf";
	    
	    maxPossibleScore = students.get(0).getMaxPossibleScore();
	    double minScore = getMinimumScore();
	    double maxScore = getMaximumScore();
	    double meanCorrect = getMeanCorrect();
	    double variance = getVariance();
	    double stdDev = Math.sqrt(variance);
	    
	    double itemVariance = getItemVariance();
	    double reliabilityEstimate = (((stdDev*stdDev) - itemVariance)/(stdDev*stdDev))*(items.size()/(items.size()-1));
	    double stdErrorMeasurement = Math.sqrt(1-reliabilityEstimate)*stdDev;
	    double passRate = getPassRate();
	    
	    //Descriptive statistics
	    int highestOption = getHighestOption();
	    for (int i = 1; i < 17; i++){
                if (i < highestOption+1)
                    parameters.put("optLabel"+ i, i);
                else 
                    parameters.put("optLabel"+ i, "");
	    }
            
            //calc data for discrimination vs difficulty table
            int diffTotals[] = new int[10];
            int discTotals[] = new int[9];
            
            int matrix[][] = new int[9][10];
            
            for (Item item : items){
	    	double pbis = item.getPointBisDisc(meanCorrect, stdDev, students);
                double diff = item.getDifficulty()*100;
                pbis = Double.isNaN(pbis) ? -2 : pbis*100;
                
                if (pbis == -2) continue;
                
                int i = 0;
                if (pbis > 70) i = 0;
                else if (pbis < 0) i = 8;
                else i =  (7 - (int) Math.floor(pbis/10));
                
                int j = 0;
                if (diff >= 90) j = 9;
                else j = (int) Math.floor(diff/10);
                
                discTotals[i]++;
                diffTotals[j]++;
                matrix[i][j]++;
            }
            
            for (int i = 0; i < 9; i++){
                for (int j = 0; j < 10; j++){
                    if (matrix[i][j] != 0) parameters.put("discDiff_" + (i+1) + "-" + (j+1), matrix[i][j]);
                    else parameters.put("discDiff_" + (i+1) + "-" + (j+1), "");
                }
                
                if (discTotals[i] != 0) parameters.put("discTotals_" + (i+1), discTotals[i]);
                else parameters.put("discTotals_" + (i+1), "");
            }
            
            for (int j = 0; j < 10; j++){
                if (diffTotals[j] != 0) parameters.put("diffTotals_" + (j+1), diffTotals[j]);
                else parameters.put("diffTotals_" + (j+1), "");
            }
            
            
            parameters.put("courseTitle", courseTitle);
            parameters.put("numCandidates", students.size());
            parameters.put("numItems", items.size());
            parameters.put("minScoreRaw", String.format("%.0f", minScore));
            parameters.put("minScorePercent", String.format("%.0f", (minScore/maxPossibleScore*100)) + "%");
            parameters.put("maxScoreRaw", String.format("%.0f", maxScore));
            parameters.put("maxScorePercent", String.format("%.0f", (maxScore/maxPossibleScore*100)) + "%");
            parameters.put("meanScoreRaw", String.format("%.0f", meanCorrect));
            parameters.put("meanScorePercent", String.format("%.0f", (meanCorrect/maxPossibleScore*100)) + "%");
            parameters.put("variance", String.format("%.2f",variance));
            parameters.put("stdDev", String.format("%.2f", stdDev));
            parameters.put("stdErrMeas", String.format("%.2f", stdErrorMeasurement));      
	    parameters.put("reliability", String.format("%.3f", reliabilityEstimate));
            parameters.put("passScoreRaw", String.format("%.0f", passingScore));
            parameters.put("passScorePercent", String.format("%.0f", (passingScore/maxPossibleScore*100)) + "%");
            parameters.put("passRateRaw", String.format("%.0f", passRate));
            parameters.put("passRatePercent", String.format("%.0f", (passRate/students.size()*100)) + "%");
           
            
            //item statistics
	    Collection<ItemRecord> data = new ArrayList<>();
	    
	    //stats for each item
	    for (Item item : items){
	    	double pBD = item.getPointBisDisc(meanCorrect, stdDev, students);
	    	String pointBisDisc = Double.isNaN(pBD) ? "N/A" : String.format("%.2f", pBD);
	    	
                ItemRecord newRec = new ItemRecord (item.getId(), String.format("%d", item.getCorrect()), String.format("%.0f", item.getDifficulty()*100) + "%",
                        pointBisDisc, String.format("%d", item.getModeChoice()), String.format("%d", item.getCountAnswered()), String.format("%.2f", item.getVariance()));
	    	
	    	//print each option's count
	    	for (int i = 1; i < 17; i++){
                    if (i < item.getHighestOption()+1)
                        newRec.setOptCount(i, String.format("%d", item.getOptionCount(i)));
	    	}
                
                data.add(newRec);
	    }
            
            //setup chart stuff
            String itemList[] = new String[items.size()];            
            int i = 0;
            for (Item item : items){
                itemList[i] = item.getId() + " (" + (i+1) + ")";
                i++;
            }
            itemChartSelector.setModel(new DefaultComboBoxModel(itemList));
            //create chart for first item
            CategoryDataset dataset = createDataset(0);
            chart = createChart(dataset, 0);
            ChartPanel chartPanelOuter = new ChartPanel(chart);
            chartPanelOuter.setFillZoomRectangle(true);
            chartPanelOuter.setMouseWheelEnabled(true);
            chartPanelOuter.setPreferredSize(new Dimension(585, 268));

            chartPanel.setLayout(new BorderLayout());
            chartPanel.add(chartPanelOuter,BorderLayout.CENTER);
            chartPanel.validate();            
            
            //if selected, generate the pdf report and open it
            if (doGenerateReport){
                InputStream reportStream;

                String templateFilename = (highestOption < 8) ? "templates/reportTemplatePortrait.jrxml" : "templates/reportTemplateLandscape.jrxml";

                try {
                    reportStream = new FileInputStream(templateFilename);
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "Could not find report template (" + templateFilename + ")", "Missing template", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JRDataSource datasource = new JRBeanCollectionDataSource(data, true);
                // Compile and print the jasper report

                try {
                    JasperReport report = JasperCompileManager.compileReport(reportStream);
                    JasperPrint print = JasperFillManager.fillReport(report, parameters, datasource);
                    JasperExportManager.exportReportToPdfFile(print, outFilename);
                } catch (JRException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Error generating report", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                //open the generated file
                try {
                    File pdfFile = new File(outFilename);
                    if (pdfFile.exists()) {

                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(pdfFile);
                        } else {
                            System.out.println("Awt Desktop is not supported");
                        }

                    } else {
                        System.out.println("File doesn't exist");
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        public JFreeChart createChart(final CategoryDataset dataset, int index) {
            String title = items.get(index).getId() + " (" + (index+1) + ")";
            //ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
            // create the chart...
            final JFreeChart chart = ChartFactory.createBarChart(
                "Option Counts for " + title,         // chart title
                "Option",               // domain axis label
                "Count",                  // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
            );

            // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

            // set the background color for the chart...
            chart.setBackgroundPaint(Color.white);

            // get a reference to the plot for further customisation...
            final CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);

            // set the range axis to display integers only...
            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

            // disable bar outlines...
            final BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);

            renderer.setSeriesPaint(0, Color.blue);

            chart.removeLegend();
            ((BarRenderer) plot.getRenderer()).setBarPainter(new StandardBarPainter());

            return chart;

        }

        public CategoryDataset createDataset(int index) {
            // create the dataset...
            final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            Item item = items.get(index);
            
            for(int i = 1; i < item.getHighestOption()+1; i++){
                dataset.addValue(item.getOptionCounts().get(i), "1", String.format("%d", i));
            }

            return dataset;

        }
        
	//output report to csv file
	public void outputData() throws IOException{
            Writer writer = null;

            String[] fn = filename.split("\\\\");
            fn = fn[fn.length-1].split("\\.");
            String outFilename = fn[0] + "-out.csv";
	    try{
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilename), "utf-8"));
            } catch (FileNotFoundException e){
                JOptionPane.showMessageDialog(null, "The output file '" + outFilename + "' is currently open. Please close the file and try again.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
	    
	    maxPossibleScore = students.get(0).getMaxPossibleScore();
	    double minScore = getMinimumScore();
	    double maxScore = getMaximumScore();
	    double meanCorrect = getMeanCorrect();
	    double variance = getVariance();
	    double stdDev = Math.sqrt(variance);
	    
	    double itemVariance = getItemVariance();
	    double reliabilityEstimate = (((stdDev*stdDev) - itemVariance)/(stdDev*stdDev))*(items.size()/(items.size()-1));
	    double stdErrorMeasurement = Math.sqrt(1-reliabilityEstimate)*stdDev;
	    double passRate = getPassRate();
	    
	    //Descriptive statistics
	    writer.write("DESCRIPTIVE STATISTICS,,\n");
	    writer.write(",RAW,Proportion\n");
	    writer.write("NUMBER OF CANDIDATES," + students.size() + ",\n");
	    writer.write("NUMBER OF ITEMS," + items.size() + ",\n");
	    writer.write("Min Score," + minScore + "," + String.format("%.0f", (minScore/maxPossibleScore*100)) + "%\n");
	    writer.write("Max Score," + maxScore + "," + String.format("%.0f", (maxScore/maxPossibleScore*100)) + "%\n");
	    writer.write("Mean Correct," + String.format("%.2f",meanCorrect) + "," + String.format("%.0f", (meanCorrect/maxPossibleScore*100)) + "%\n");
	    writer.write("Variance," + String.format("%.2f",variance) + ",\n");
	    writer.write("Standard Deviation," + String.format("%.2f", stdDev) + ",\n");
	    writer.write("Standard Error of Measurement," + String.format("%.2f", stdErrorMeasurement) + ",\n");
	    writer.write("Reliability Estimate," + String.format("%.3f", reliabilityEstimate) + ",\n");
	    writer.write("Pass Score," + String.format("%.0f", passingScore) + "," + String.format("%.0f", (passingScore/maxPossibleScore*100)) + "%\n");
	    writer.write("Pass Rate," + String.format("%.0f", passRate) + "," + String.format("%.0f", (passRate/students.size()*100)) + "%\n");

	    int highestOption = getHighestOption();
	    String commas = "";
	    String options = "";
	    for (int i = 0; i < highestOption; i++){
	    	commas += ",";
	    	options += (i+1) + ",";
	    }
	    
	    //item statistics
	    writer.write("\n\n\nITEM STATISTICS\n");
	    writer.write("Item ID,Correct Answer,Difficulty,Point Biserial Discrimination,Mode Choice,Count,,Option Count" + commas + "Item Variance\n");
	    writer.write(",,,,,,," + options + itemVariance + "\n");
	    
	    //stats for each item
	    for (Item item : items){
	    	double pBD = item.getPointBisDisc(meanCorrect, stdDev, students);
	    	String pointBisDisc = Double.isNaN(pBD) ? "N/A" : String.format("%.4f", pBD);
	    	
	    	writer.write(item.getId() + "," + item.getCorrect() + "," + String.format("%.0f", item.getDifficulty()*100) + "%," + 
	    				 pointBisDisc + "," + item.getModeChoice() + "," + item.getCountAnswered() + ",,");
	    	
	    	//print each option's count
	    	for (int i = 1; i < highestOption+1; i++){
	    		writer.write(item.getOptionCount(i) + ",");
	    	}
	    	
	    	writer.write(String.format("%.3f", item.getVariance()) + "\n");
	    }
	    	        
	    writer.close();

	    //open the generated file
	    try {
			File pdfFile = new File(outFilename);
			if (pdfFile.exists()) {
	 
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(pdfFile);
				} else {
					System.out.println("Awt Desktop is not supported");
				}
	 
			} else {
				System.out.println("File doesn't exist");
			}
	 
	    } catch (Exception ex) {
			ex.printStackTrace();
	    }
	}
	
	//get lowest score
	public double getMinimumScore(){
		double minScore = students.get(0).getScore();
		
		for(Student student : students){
			if (student.getScore() < minScore) minScore = student.getScore();
		}
		
		return minScore;
	}
	
	//get highest score
	public double getMaximumScore(){
		double maxScore = students.get(0).getScore();
		
		for(Student student : students){
			if (student.getScore() > maxScore) maxScore = student.getScore();
		}
		
		return maxScore;
	}
	
	//get mean score
	public double getMeanCorrect(){
		double scoreSum = 0;
		
		for(Student student : students){
			scoreSum += student.getScore();
		}
		
		return scoreSum/students.size();
	}
	
	//get variance of scores
	public double getVariance(){
            double mean = getMeanCorrect();
            double temp = 0;

            for(Student s : students)
                temp += (mean-s.getScore())*(mean-s.getScore());

            return temp/students.size();
        }
	
	//get sum of item variances (for use in calc for descriptive stats)
	public double getItemVariance(){
		double varianceSum = 0;
		
		for(Item item: items){
			varianceSum += item.getVariance();
		}
		
		return varianceSum;
	}
	
	//get passing rate
	public int getPassRate(){
		passingScore = maxPossibleScore * passingPercent / 100;
		int passRate = 0;
		
		for(Student s: students){
			if (s.getScore() > passingScore) passRate++;
		}
		
		return passRate;
	}
	
	//check that the item type is a known valid one
	public boolean checkType(String type){
		if (!type.equals("MULTICHOICE")){
                    JOptionPane.showMessageDialog(null, "Unknown item type '" + type + "'", "Unknown type", JOptionPane.ERROR_MESSAGE);
                    return false;
		}
                
                return true;
	}
	
	//get the highest number of options in any item
	public int getHighestOption(){
		int highestOption = 0;
		
		for (Item i : items)
			if (i.getHighestOption() > highestOption) highestOption = i.getHighestOption();
		
		return highestOption;
	}

    private void validate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
