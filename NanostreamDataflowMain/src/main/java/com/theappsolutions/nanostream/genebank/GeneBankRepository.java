package com.theappsolutions.nanostream.genebank;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.theappsolutions.nanostream.http.NanostreamResponseHandler;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class GeneBankRepository {

    private Gson gson;

    public GeneBankRepository(Gson gson) {
        this.gson = gson;
    }

    public String[] getHierarchyByName(String name){
        HttpHelper httpHelper = new HttpHelper();
        HttpClient httpClient = httpHelper.createHttpClient();

        try {
            URI uri = httpHelper.buildURI("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
                    new HashMap<String, String>(){{
                        put("db", "Nucleotide");
                        put("usehistory", "y");
                        put("retmode", "json");
                        put("term", name);
                    }});
            HttpUriRequest httpRequest = httpHelper.buildRequest(uri);
            String response = httpHelper.executeRequest(httpClient, httpRequest, new NanostreamResponseHandler());
            JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

            JsonObject eSearchResult =  jsonObject.getAsJsonObject("esearchresult");
            String webEnv = eSearchResult.get("webenv").getAsString();
            String querykey = eSearchResult.get("querykey").getAsString();
            uri = httpHelper.buildURI("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi",
                    new HashMap<String, String>(){{
                        put("db", "Nucleotide");
                        put("WebEnv",webEnv);
                        put("query_key", querykey);
                    }});
            httpRequest = httpHelper.buildRequest(uri);
            response = httpHelper.executeRequest(httpClient, httpRequest, new NanostreamResponseHandler());
            System.out.println(response);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return new String[0];
    }
}
