package com.nl2go.rest;

/*

  This is an example class not meant for use in a production environment.
  The idea is to help you get started using the NL2GO REST API.
  This examples shows how to:
    - Get an access token
    - Create a mailing
    - Send an email using an existing mailing

  For further details please check: https://docs.newsletter2go.com

*/

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Collections;

public class API {

    private static final String GRANT_TYPE = "https://nl2go.com/jwt";
    private static final String OAUTH_URL = "https://api.newsletter2go.com/oauth/v2/token";
    private String accessToken;
    private String refreshToken;

    private String username;
    private String password;
    private String authKey;

    /**
     * Required parameters for requesting an access token
     *
     * @param username
     * @param password
     * @param authKey
     */
    public API(String username, String password, String authKey) {
        this.username = username;
        this.password = password;
        this.authKey = authKey;
    }

    /**
     * In order to retrieve the access token that you will need to send with each consecutive call,
     * you have to authenticate once.
     */
    private void authenticate() {
        try {
            byte[] encodedBytes = Base64.getEncoder().encode(authKey.getBytes());
            String base64_auth_key = new String(encodedBytes);

            String bodyJson = new JSONObject()
                .put("username", username)
                .put("password", password)
                .put("grant_type", GRANT_TYPE)
                .toString();

            HttpResponse<String> response = Unirest.post(OAUTH_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + base64_auth_key)
                .header("Cache-Control", "no-cache")
                .body(bodyJson)
                .asString();

            String jsonString = response.getBody();

            JSONObject jsonObject = new JSONObject(jsonString);

            accessToken = jsonObject.getString("access_token");
            refreshToken = jsonObject.getString("refresh_token");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Creates a mailing
     *
     * If your list has default values for header_from_email, header_from_name, header_reply_email and/or
     * header_reply_name, those will be used in case you don't pass them in this call. In this example we pass
     * those values every time.
     *
     * @param listId
     * @param name
     * @param subject
     * @param fromEmail
     * @param fromName
     * @param html
     * @return String
     */
    private String createMailing(String listId, String name, String subject, String fromEmail, String fromName, String html) {
        String newsletterId = "";

        try {
            String bodyJson = new JSONObject()
                .put("type", "transaction")
                .put("subject", subject)
                .put("name", name)
                .put("header_from_email", fromEmail)
                .put("header_from_name", fromName)
                .put("header_reply_email", fromEmail)
                .put("html", html)
                .put("state", "active")
                .toString();

            HttpResponse<String> response = Unirest.post("https://api.newsletter2go.com/lists/" + listId + "/newsletters")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Cache-Control", "no-cache")
                .body(bodyJson)
                .asString();

            String jsonString = response.getBody();

            JSONObject jsonObject = new JSONObject(jsonString);

            JSONArray value = jsonObject.getJSONArray("value");

            for (int i = 0; i < value.length(); ++i) {
                JSONObject rec = value.getJSONObject(i);
                newsletterId = rec.getString("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsletterId;
    }

    /**
     * Send a one-time Mailing
     *
     * @param newsletterId
     * @param email
     */
    private void sendEmail(String newsletterId, String email) {
        try {
            String bodyJson = new JSONObject()
                .put(
                    "contexts",
                    new JSONArray().put(
                        new JSONObject()
                            .put("recipient", Collections.singletonMap("email", email))
                    )
                )
                .toString();

            HttpResponse<String> response = Unirest
                .post("https://api.newsletter2go.com/newsletters/" + newsletterId + "/send")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Cache-Control", "no-cache")
                .body(bodyJson)
                .asString();

            String jsonString = response.getBody();

            JSONObject jsonObject = new JSONObject(jsonString);

            System.out.println(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        /*
         * Replace the dummy values with your credentials,
         * check here for more information: https://docs.newsletter2go.com/#intro
         */
        API api = new API("username@mail.com", "password123", "authKey");
        api.authenticate();

        String html = "<h1>hello world</h1>";
        String newsletterId = api.createMailing(
            "list_id", // provide a valid list id, check the documentation for more details.
            "My mailing name",
            "my subject",
            "from@example.org",
            "John Doe",
            html
        );
        String recipient = "to@example.org";

        api.sendEmail(newsletterId, recipient);
    }
}
