package com.company.rest.web.screens;

import com.company.rest.dto.AccessToken;
import com.google.common.collect.Lists;
import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.security.entity.User;
import org.apache.http.HttpHeaders;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

public class RestApiCallScreen extends AbstractWindow {

    @Inject
    protected EntityImportExportService entityImportExportService;

    @Inject
    protected RichTextArea restApiResult;

    public void fillBox() {
        Collection<Entity> users = getUsers();
        StringBuilder result = new StringBuilder();
        for (Entity user : users) {
            result.append(((User) user).getCaption())
                    .append("<br>");
        }
        restApiResult.setValue(result.toString());
    }

    private Collection<Entity> getUsers() {
        AccessToken accessToken = getRestAccessToken();
        HttpGet usersRequest = prepareGetUsersRequest(accessToken);
        return getEntities(usersRequest);
    }

    protected AccessToken getRestAccessToken() {
        JSONObject obj;
        HttpPost authRequest = prepareAuthRequest();
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(authRequest)) {

            obj = new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new AccessToken((String) obj.get("access_token"));
    }

    protected HttpPost prepareAuthRequest() {
        HttpPost authRequest = new HttpPost("http://localhost:8080/app/rest/v2/oauth/token");
        authRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50OnNlY3JldA==");
        authRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        List<BasicNameValuePair> nameValuePairs = Lists.newArrayList();

        nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
        nameValuePairs.add(new BasicNameValuePair("username", "admin"));
        nameValuePairs.add(new BasicNameValuePair("password", "admin"));
        try {
            authRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return authRequest;
    }


    protected HttpGet prepareGetUsersRequest(AccessToken accessToken) {
        HttpGet usersRequest = new HttpGet("http://localhost:8080/app/rest/v2/entities/sec$User");
        usersRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getToken());
        return usersRequest;
    }

    Collection<Entity> getEntities(HttpGet request) {
        String users;
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            users = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        EntityImportView importView = new EntityImportView(User.class)
                .addLocalProperties();

        return entityImportExportService.importEntitiesFromJSON(users, importView);
    }
}