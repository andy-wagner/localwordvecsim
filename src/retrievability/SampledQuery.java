/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retrievability;

import java.util.Objects;
import localembed.WebDocAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */
public class SampledQuery {
    RetrievabilityFinder sampler;
    String queryBody;
    Query lucquery;
    Analyzer analyzer;
    
    public SampledQuery(RetrievabilityFinder sampler, String queryBody) throws Exception {
        this.sampler = sampler;
        this.queryBody = this.queryBody;
        analyzer = new WebDocAnalyzer("stopfile");
    }
    
    Query initQuery() {
        QueryParser parser = new QueryParser(queryBody, analyzer);
        lucquery = parser.createBooleanQuery(sampler.getProperties().getProperty("content.field.name", "content"),
                queryBody,
                BooleanClause.Occur.SHOULD);
        return lucquery;
    }
    
    TopDocs execute(IndexSearcher searcher) throws Exception {
        TopScoreDocCollector collector = TopScoreDocCollector.create(sampler.getNumWanted());
        searcher.search(this.lucquery, collector);
        return collector.topDocs();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return this.queryBody.equals(((SampledQuery)obj).queryBody);
    }
    
    @Override
    public int hashCode() {
        return queryBody.hashCode();
    }
}

