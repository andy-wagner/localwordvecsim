/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retrievability;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelBE;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DistributionLL;
import org.apache.lucene.search.similarities.IBSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.LambdaDF;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.NormalizationH1;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */

public class RetrievabilityFinder {
    
    RetrievabilityScore[] retrScores;
    Properties prop;
    // Retrievability parameters
    int nwanted;
    int rankCutoff;  // threshold
    
    IndexReader reader;  // the combined index to search
    IndexSearcher searcher;
    Similarity sim;
    List<String> queryVocab;
    
    static Random randomizer = new Random(123456);
    
    static final Similarity[] simArray = {
            new BM25Similarity(),
            new LMDirichletSimilarity(),
            new LMJelinekMercerSimilarity(0.6f),
            new DFRSimilarity(new BasicModelBE(), new AfterEffectB(), new NormalizationH1()),
            new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationH1())
    };

    /**
     * Constructor
     * Initializes the query vocabulary upon which the queries will be generated
     * @param prop
     * @param queryVocab
     * @throws Exception 
     */
    public RetrievabilityFinder(Properties prop, List<String> queryVocab) throws Exception {

        this.prop = prop;
        nwanted = Integer.parseInt(prop.getProperty("retrievability.nretrieve", "100"));
        rankCutoff = Integer.parseInt(prop.getProperty("retrievability.c", "100"));        
        
        this.queryVocab = queryVocab;
    }

    int getNumWanted() { return nwanted; }

    Properties getProperties() { return prop; }
    
    void initSearch() throws Exception {
        
        File indexDir = new File(prop.getProperty("index"));
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        sim = new MultiSimilarity(simArray);
        int nDocs = reader.numDocs();        
        
        searcher = new IndexSearcher(reader);
        retrScores = new RetrievabilityScore[nDocs];
    }
    
    HashSet<SampledQuery> sampleQueries() throws Exception {
        HashSet<SampledQuery> querySamples = new HashSet<>();
        
        int numTermsInQry;
        int maxTermsInQry = Integer.parseInt(prop.getProperty("retrievability.query.maxterms", "4"));
        int numSamples = Integer.parseInt(prop.getProperty("retrievability.query.numsamples", "500"));
        
        for (int i=0; i < numSamples; i++) {
            Collections.shuffle(queryVocab, randomizer);
            numTermsInQry = randomizer.nextInt(maxTermsInQry) + 1;
            //System.out.println(i +" " + maxTermsInQry +" " + numTermsInQry);
            
            List<String> qryTermsSampled = queryVocab.subList(0, numTermsInQry);
            StringBuffer concatenatedQryTerms = new StringBuffer();
            for (String qryTerms : qryTermsSampled) {
                concatenatedQryTerms.append(qryTerms).append(" ");
            }
            
            SampledQuery sampledQuery = new SampledQuery(this, concatenatedQryTerms.toString());
            querySamples.add(sampledQuery);
        }
        
        return querySamples;
    }

    /**
     * 1: Generates the set of queries
     * 2: Executes the queries to get topdocs for each of the query
     * 3: Makes a list of documents with the retrievability score, 
     *      calculated based on the retrievability status of that document for
     *      that set of queries.
     * @return
     * @throws Exception 
     */
    public List<RetrievabilityScore> getTopRetrievableDocs() throws Exception {

        // number of topdocs using which the local vectors will be trained
        int numTopRetrDocs = Integer.parseInt(prop.getProperty("localvec.numtopdocs", "1000"));

        // Init searcher objects
        initSearch();
    
        System.out.println("Generating query samples...");
        HashSet<SampledQuery> queries = sampleQueries();
        
        System.out.println("Executing query samples...");
        int count = 0, nqueries = queries.size();
        
        for (SampledQuery query: queries) {
            System.out.println("Executing query: " + query.lucquery + " (" + count + " of " + nqueries + ")");
            
            TopDocs topDocs = query.execute(searcher);
            updateScoresForThisQuerySample(topDocs);
            count++;
        }
        System.out.println();
        Arrays.sort(retrScores, new Comparator<RetrievabilityScore>() {

        @Override
        public int compare(RetrievabilityScore o1, RetrievabilityScore o2) {
            
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return -1*Float.compare(o1.score, o2.score);
        }});
        return Arrays.asList(retrScores).subList(0, Math.min(numTopRetrDocs, retrScores.length));        
    }
    
    void updateScoresForThisQuerySample(TopDocs topDocs) throws Exception {
        
        String idFldName = prop.getProperty("id.field.name", "id");
        
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {            
            int docId = topDocs.scoreDocs[i].doc;
            Document doc = reader.document(docId);
            String docName = doc.get(idFldName);
            
            // cut-off at rank rankCutoff
            if (i >= rankCutoff)
                continue;
            
            if (retrScores[docId] == null)
                retrScores[docId] = new RetrievabilityScore(docName, docId);
            else
                retrScores[docId].score++;
        }        
    }
}
