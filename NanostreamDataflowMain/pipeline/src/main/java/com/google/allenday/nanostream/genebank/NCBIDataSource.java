package com.google.allenday.nanostream.genebank;

import com.google.allenday.nanostream.http.NanostreamResponseHandler;
import com.google.allenday.nanostream.util.HttpHelper;
import com.google.allenday.nanostream.util.StringUtils;
import com.google.allenday.nanostream.util.XMLUtils;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DefaultCoder(SerializableCoder.class)
public class NCBIDataSource implements Serializable {

    private final static String NCBI_EUTILS_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    private final static String NCBI_E_FETCH_ENDPOINT = "efetch.fcgi";
    private final static String NCBI_E_SEARCH_ENDPOINT = "esearch.fcgi";

    private final static String NUCLEOTIDE_DB_NAME = "Nucleotide";
    private final static String USE_HISTORY_YES_VALUE = "y";
    private final static String RETMODE_XML_VALUE = "xml";

    @DefaultCoder(SerializableCoder.class)
    private enum EUtilRequestParams implements Serializable {
        DB("db"),
        WEB_ENV("WebEnv"),
        QUERY_KEY("query_key"),
        RETMODE("retmode"),
        USE_HISTORY("usehistory"),
        TERM("term");

        public final String key;

        EUtilRequestParams(String key) {
            this.key = key;
        }
    }

    private final static String E_SEARCH_WEB_ENV_RESPONSE_KEY = "WebEnv";
    private final static String E_SEARCH_QUERY_KEY_RESPONSE_KEY = "QueryKey";
    private final static String E_FETCH_TAXONOMY_RESPONSE_KEY = "GBSeq_taxonomy";
    private final static String E_FETCH_LOCUS_RESPONSE_KEY = "GBSeq_locus";
    private final static String E_FETCH_DEFINITION_RESPONSE_KEY = "GBSeq_definition";

    private final static String TAXONOMY_LIST_DIVIDER = ";";

    private final static List<String> OTHER_TAXONOMY_GROUP =
            Stream.of("Other").collect(Collectors.toList());
    private final static List<String> FAILED_SEARCH_TAXONOMY_GROUP =
            Stream.of("Failed search").collect(Collectors.toList());

    private HttpHelper httpHelper;
    private Logger LOG = LoggerFactory.getLogger(NCBIDataSource.class);

    public NCBIDataSource(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    public GeneTaxonomyInfo getTaxonomyFromNCBI(String nanostreamGenomeName) {
        String searchQuery = null;
        try {
            if (nanostreamGenomeName.contains("NC")) {
                searchQuery = Stream.of(nanostreamGenomeName.split("\\|")).filter(part -> part.contains("NC")).findFirst().orElse(null);
                if (searchQuery != null) {
                    searchQuery = searchQuery.split("\\.")[0];
                }
            } else {
                throw new UnknownNameExeption();
            }
            CloseableHttpClient httpClient = httpHelper.createHttpClient();
            Document searchDocument = proceedNCBISearch(httpClient, searchQuery);
            httpClient.close();
            String webEnv = XMLUtils.getElementTextFromDocument(searchDocument, E_SEARCH_WEB_ENV_RESPONSE_KEY);
            String querykey = XMLUtils.getElementTextFromDocument(searchDocument, E_SEARCH_QUERY_KEY_RESPONSE_KEY);

            httpClient = httpHelper.createHttpClient();
            Document fetchDocument = proceedNCBIFetch(httpClient, webEnv, querykey);
            httpClient.close();

            String taxonomy = XMLUtils.getElementTextFromDocument(fetchDocument, E_FETCH_TAXONOMY_RESPONSE_KEY);
            String locus = XMLUtils.getElementTextFromDocument(fetchDocument, E_FETCH_LOCUS_RESPONSE_KEY);
            String definition = XMLUtils.getElementTextFromDocument(fetchDocument, E_FETCH_DEFINITION_RESPONSE_KEY);
            if (taxonomy == null) {
                throw new NCBISearchExeption();
            }
            return new GeneTaxonomyInfo(nanostreamGenomeName, locus, searchQuery,
                    Stream.of(StringUtils.removeWhiteSpaces(taxonomy).split(TAXONOMY_LIST_DIVIDER)).collect(Collectors.toList()),
                    definition);
        } catch (UnknownNameExeption exeption) {
            return new GeneTaxonomyInfo(nanostreamGenomeName, null, searchQuery,
                    OTHER_TAXONOMY_GROUP,
                    null);
        } catch (URISyntaxException | IOException | SAXException | ParserConfigurationException e) {
            return new GeneTaxonomyInfo(nanostreamGenomeName, null, searchQuery,
                    FAILED_SEARCH_TAXONOMY_GROUP,
                    null);
        }
    }

    private Document proceedNCBISearch(HttpClient httpClient, String query) throws URISyntaxException, IOException, SAXException, ParserConfigurationException {
        URI uri = httpHelper.buildURI(NCBI_EUTILS_URL + NCBI_E_SEARCH_ENDPOINT,
                new HashMap<String, String>() {{
                    put(EUtilRequestParams.DB.key, NUCLEOTIDE_DB_NAME);
                    put(EUtilRequestParams.USE_HISTORY.key, USE_HISTORY_YES_VALUE);
                    put(EUtilRequestParams.TERM.key, query);
                }});
        HttpUriRequest httpRequest = httpHelper.buildRequest(uri);
        String response = httpHelper.executeRequest(httpClient, httpRequest, new NanostreamResponseHandler());

        return XMLUtils.getDocumentFromXMLString(response);
    }

    private Document proceedNCBIFetch(HttpClient httpClient, String webEnv, String queryKey) throws URISyntaxException, IOException, SAXException, ParserConfigurationException {
        URI uri = httpHelper.buildURI(NCBI_EUTILS_URL + NCBI_E_FETCH_ENDPOINT,
                new HashMap<String, String>() {{
                    put(EUtilRequestParams.DB.key, NUCLEOTIDE_DB_NAME);
                    put(EUtilRequestParams.WEB_ENV.key, webEnv);
                    put(EUtilRequestParams.QUERY_KEY.key, queryKey);
                    put(EUtilRequestParams.RETMODE.key, RETMODE_XML_VALUE);
                }});
        HttpUriRequest httpRequest = httpHelper.buildRequest(uri);

        String response = httpHelper.executeRequest(httpClient, httpRequest, new NanostreamResponseHandler());
        return XMLUtils.getDocumentFromXMLString(response);
    }

    public class UnknownNameExeption extends IOException {
        UnknownNameExeption() {
            super();
        }
    }

    public class NCBISearchExeption extends IOException {
        NCBISearchExeption() {
            super("NCBI database doesn`t contain any info for: %s");
        }
    }
}
