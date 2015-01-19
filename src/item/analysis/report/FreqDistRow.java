
package item.analysis.report;

public class FreqDistRow {
    private String score;
    private String freq;
    private String percentile;
    private String cumpercent;

   public FreqDistRow(String score, String freq, String percentile, String cumpercent){
        this.score = score;
        this.freq = freq;
        this.percentile = percentile;
        this.cumpercent = cumpercent;
    }

    /**
     * @return the score
     */
    public String getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(String score) {
        this.score = score;
    }

    /**
     * @return the freq
     */
    public String getFreq() {
        return freq;
    }

    /**
     * @param freq the freq to set
     */
    public void setFreq(String freq) {
        this.freq = freq;
    }

    /**
     * @return the percentile
     */
    public String getPercentile() {
        return percentile;
    }

    /**
     * @param percentile the percentile to set
     */
    public void setPercentile(String percentile) {
        this.percentile = percentile;
    }

    /**
     * @return the cumpercent
     */
    public String getCumpercent() {
        return cumpercent;
    }

    /**
     * @param cumpercent the cumpercent to set
     */
    public void setCumpercent(String cumpercent) {
        this.cumpercent = cumpercent;
    }
   
   

}