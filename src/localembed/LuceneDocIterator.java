/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package localembed;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import retrievability.RetrievabilityScore;

/**
 *
 * @author Debasis
 */
public class LuceneDocIterator implements SentenceIterator {
    IndexReader reader;
    int docIndex;
    Analyzer analyzer;
    int numDocs;
    String contentFieldName;
    List<RetrievabilityScore> docIds;

    public LuceneDocIterator(File indexDir,
                            Analyzer analyzer,
                            String contentFieldName,
                            List<RetrievabilityScore> docIds) throws Exception {
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));        
        this.analyzer = analyzer;
        this.docIds = docIds;
        docIndex = 0;
        numDocs = docIds.size();
        this.contentFieldName = contentFieldName;
    }

    @Override
    public String nextSentence() {
        String content = null;
        try {
            int docId = docIds.get(docIndex).getDocID();
            Document doc = reader.document(docId);
            content = preProcess(analyzer, doc.get(contentFieldName));
            docId++;
        }
        catch (Exception ex) { ex.printStackTrace(); }
        return content;
    }

    @Override
    public boolean hasNext() {
        return docIndex < numDocs;
    }

    @Override
    public void reset() {
        docIndex = 0;
    }

    @Override
    public void finish() {
        try {
            reader.close();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override
    public SentencePreProcessor getPreProcessor() {
        return null;
    }

    @Override
    public void setPreProcessor(SentencePreProcessor spp) {
    }
 
    String preProcess(Analyzer analyzer, String text) throws Exception {

        StringBuffer tokenizedContentBuff = new StringBuffer();
        TokenStream stream = analyzer.tokenStream("words", new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            tokenizedContentBuff.append(term).append(" ");
        }
        
        stream.end();
        stream.close();
        return tokenizedContentBuff.toString();
    }
}

