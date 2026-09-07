package com.inandout.fieldphotoprep;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DriveClient {
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files";

    public List<DriveFolder> listFolders(String accessToken, String parentId) throws Exception {
        List<DriveFolder> all = new ArrayList<>();
        String pageToken = null;
        do {
            URI uri = URI.create(buildListFoldersUrl(parentId, pageToken));
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);

            int status = connection.getResponseCode();
            String body = readBody(status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            connection.disconnect();

            if (status < 200 || status >= 300) {
                throw new IOException("Drive request failed (HTTP " + status + "). " + body);
            }

            JSONObject root = new JSONObject(body);
            JSONArray files = root.optJSONArray("files");
            if (files != null) {
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    all.add(new DriveFolder(file.getString("id"), file.getString("name")));
                }
            }
            pageToken = root.optString("nextPageToken", null);
            if (pageToken != null && pageToken.isBlank()) {
                pageToken = null;
            }
        } while (pageToken != null);

        Collections.sort(all);
        return all;
    }

    static String buildListFoldersUrl(String parentId, String pageToken) {
        String query = "'" + escapeDriveLiteral(parentId) + "' in parents and "
                + "mimeType = '" + FOLDER_MIME + "' and trashed = false";
        StringBuilder url = new StringBuilder(FILES_URL)
                .append("?q=").append(encode(query))
                .append("&fields=").append(encode("nextPageToken,files(id,name)"))
                .append("&orderBy=").append(encode("name"))
                .append("&pageSize=1000")
                .append("&spaces=drive")
                .append("&supportsAllDrives=true")
                .append("&includeItemsFromAllDrives=true");
        if (pageToken != null && !pageToken.isBlank()) {
            url.append("&pageToken=").append(encode(pageToken));
        }
        return url.toString();
    }

    static String escapeDriveLiteral(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
}
