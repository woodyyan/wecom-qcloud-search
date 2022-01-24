package woody;

import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent;
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Application {
    public static final String EM_BEGIN = "<em>";
    public static final String EM_END = "</em>";

    public static void main(String[] args) {
//        new Application().searchCloudDoc("函数计费");
    }

    public String mainHandler(APIGatewayProxyRequestEvent req) throws IOException {
        System.out.println("start main handler");
        String body = req.getBody();
        System.out.println("Body: " + body);
        JSONObject json = new JSONObject(body);
        String msgContent = json.getString("msgContent");
        System.out.println(msgContent);

        System.out.println("send request");
        APIGatewayProxyResponseEvent resp = new APIGatewayProxyResponseEvent();
        resp.setStatusCode(200);
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        resp.setHeaders(new com.alibaba.fastjson.JSONObject(headers));
        resp.setBody(searchCloudDoc(msgContent));
        System.out.println(resp.getBody());
        return resp.toString();
    }

    public String searchCloudDoc(String query) throws IOException {
        String url = "https://cloud.tencent.com/search/ajax/searchdoc?keyword=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=1&pagesize=10";

        JSONObject json = get(url, null);
        JSONArray jsonArray = json.getJSONObject("data").getJSONArray("dataList");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getString("title");
            String urlString = jsonObject.getString("url");
            String content = jsonObject.getString("content");
            System.out.println("title:" + title + ";urlString:" + urlString);
            System.out.println(content);
            stringBuilder.append(String.format("[%s](%s) \n > %s \n", title, urlString, replaceFont(content)));
            stringBuilder.append(System.lineSeparator());
        }
        System.out.println("查询结果：");
        System.out.println(stringBuilder);

        JSONObject result = new JSONObject();
        result.put("msgType", "markdown");
        result.put("msgContent", stringBuilder.toString());
        System.out.println(result);
        return result.toString();
    }

    public String replaceAllFont(String content) {
        if (content.contains(EM_BEGIN)) {
            content = replaceFont(content);
            return replaceAllFont(content);
        } else {
            return content;
        }
    }

    public String replaceFont(String content) {
        int begin = content.indexOf(EM_BEGIN);
        int end = content.indexOf(EM_END);
        if (begin < 0 || end < 0) {
            return content;
        }
        String emString = content.substring(begin, end + EM_END.length());
        String fontString = emString.replace(EM_BEGIN, "<font color=red>");
        fontString = fontString.replace(EM_END, "</font>");
        String replace = content.replace(emString, fontString);
        if (replace.contains("。")) {
            int index = replace.indexOf("。");
            replace = replace.substring(0, index + 1);
        }
        return replace;
    }

    private JSONObject getJsonObject(BufferedReader in) throws IOException {
        String inputLine;
        StringBuffer stringBuffer = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            stringBuffer.append(inputLine);
        }
        in.close();
        System.out.println(stringBuffer);
        return new JSONObject(stringBuffer.toString());
    }

    public JSONObject get(String url, String authorization) throws IOException {
        System.out.println(url);
        URL obj = new URL(url);

        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        if (Strings.isNotEmpty(authorization)) {
            con.setRequestProperty("Authorization", authorization);
        }

        return getJsonObject(url, con);
    }

    private JSONObject getJsonObject(String url, HttpURLConnection con) throws IOException {
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        return getJsonObject(in);
    }
}
