/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retrievability;

/**
 *
 * @author Debasis
 */
public class RetrievabilityScore implements Comparable<RetrievabilityScore> {
    String docName;
    int docId;
    int score;

    public RetrievabilityScore(String docName, int docId) {
        this.docName = docName;
        this.docId = docId;
        this.score = 1;
    }

    public RetrievabilityScore(String docName, int docId, int score) {
        this.docName = docName;
        this.docId = docId;
        this.score = score;
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(docName)
                .append('\t')
                .append(docId)
                .append('\t')
                .append(score)
                .append('\n');
        return buff.toString();
    }

    @Override
    public int compareTo(RetrievabilityScore that) { // descending
        return -1*Float.compare(this.score, that.score);
    }
    
    public int getDocID() { return docId; }
    public float getScore() { return score; }
    
}



