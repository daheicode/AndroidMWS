package com.example.administrator.ymx;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.text)
    TextView text;
    HttpParams params=new HttpParams();
    private static String DEFAULT_ENCODING = "UTF-8";
    private static String BASEURL="https://mws.amazonservices.com/";
    private Map<String,String> params01=new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
       /* params01.put("AWSAccessKeyId","AKIAJDWNQRA5PGZXBRWA");
        params01.put("MWSAuthToken","904349549961");
        params01.put("SellerId"," A20KUOQYMO6PIE");*/
//        params01.put("Action","SubmitFeed");
        params01.put("Version","2009-1-1");
        params01.put("SignatureVersion","2");
        params01.put("Timestamp",getISO8601Timestamp(new Date()));
        params01.put("AWSAccessKeyId","AKIAJDWNQRA5PGZXBRWA");
       /* params01.put("SignatureMethod","HmacSHA256");
        params01.put("FeedType","_POST_PRODUCT_DATA_");*/

        params.put("AWSAccessKeyId","AKIAJDWNQRA5PGZXBRWA");
        params.put("MWSAuthToken","904349549961");
        params.put("SellerId"," A20KUOQYMO6PIE");
        params.put("Action","SubmitFeed");
        params.put("SignatureVersion","2");
//        params.put("Signature","U5jjw6ZcBS1juVhk81n0DTtVqmhMKmvRJGIdWjUS");
        try {
            params.put("Signature",sign(calculateStringToSignV2(params01),"U5jjw6ZcBS1juVhk81n0DTtVqmhMKmvRJGIdWjUS","HmacSHA256"));
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        params.put("SignatureMethod","HmacSHA256");
        params.put("Timestamp",getISO8601Timestamp(new Date()));
        params.put("FeedType","_POST_PRODUCT_DATA_");
        params.put("Version","2009-1-1");
    }

   /* private String signParameters(Map<String, String> parameters, String key)
            throws  SignatureException {
        String signatureVersion = parameters.get("SignatureVersion");
        String algorithm = "HmacSHA1";
        String stringToSign = null;
        if ("2".equals(signatureVersion)) {
            algorithm = config.getSignatureMethod();
            parameters.put("SignatureMethod", algorithm);
            stringToSign = calculateStringToSignV2(parameters);
        } else {
            throw new SignatureException("Invalid Signature Version specified");
        }
//        log.debug("Calculated string to sign: " + stringToSign);
        return sign(stringToSign, key, algorithm);
    }*/

    private String calculateStringToSignV2(Map<String, String> parameters)
            throws SignatureException {
        StringBuilder data = new StringBuilder();
        data.append("POST");
        data.append("\n");
        URI endpoint = null;
        try {
            endpoint = new URI(BASEURL.toLowerCase());
        } catch (URISyntaxException ex) {
//            log.error("URI Syntax Exception", ex);
            throw new SignatureException("URI Syntax Exception thrown " +
                    "while constructing string to sign", ex);
        }
        data.append(endpoint.getHost());
        if (!usesAStandardPort(BASEURL)) {
            data.append(":");
            data.append(endpoint.getPort());
        }
        data.append("\n");
        String uri = endpoint.getPath();
        if (uri == null || uri.length() == 0) {
            uri = "/";
        }
        data.append(uri);
        data.append("\n");
        Map<String, String> sorted = new TreeMap<String, String>();
        sorted.putAll(parameters);
        Iterator<Map.Entry<String, String>> pairs = sorted.entrySet().iterator();
        while (pairs.hasNext()) {
            Map.Entry<String, String> pair = pairs.next();
            String key = pair.getKey();
            data.append(urlEncode(key));
            data.append("=");
            String value = pair.getValue();
            data.append(urlEncode(value));
            if (pairs.hasNext()) {
                data.append("&");
            }
        }
        return data.toString();
    }

    private String urlEncode(String rawValue) {
        String value = rawValue==null ? "" : rawValue;
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, DEFAULT_ENCODING)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E","~");
        } catch (UnsupportedEncodingException ex) {
//            log.error("Unsupported Encoding Exception", ex);
//            throw new RuntimeException(ex);
        }
        return encoded;
    }

    private static boolean usesAStandardPort(String url) {
        boolean usesHttps = usesHttps(url);
        int portNumber = extractPortNumber(url, usesHttps);
        return usesHttps && portNumber == 443
                || !usesHttps && portNumber == 80;
    }

    private static boolean usesHttps(String url){
        URL urlToCheck;
        try {
            urlToCheck = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        if (urlToCheck.getProtocol().equals("https")){
            return true;
        }else
        {
            return false;
        }
    }

    private static int extractPortNumber(String url, boolean usesHttps) {
        URL urlToCheck;
        try {
            urlToCheck = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("not a URL", e);
        }

        int portNumber = urlToCheck.getPort();
        if (portNumber == -1){
            // no port was specified
            if (usesHttps){
                // it uses https, so we should return the standard https port number
                return 443;
            }else
            {
                // it uses http, so we should return the standard http port number
                return 80;
            }
        }else
        {
            return portNumber;
        }
    }

    private String sign(String data, String key, String algorithm) throws SignatureException {
        byte[] signature;
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(), algorithm));
            signature = Base64.encodeBase64(mac.doFinal(data.getBytes(DEFAULT_ENCODING)));
//            signature=Base64.getEncoder().encode(mac.doFinal(data.getBytes(DEFAULT_ENCODING)));
//            signature=Base64.encode(mac.doFinal(data.getBytes(DEFAULT_ENCODING)));
        } catch (Exception e) {
            throw new SignatureException("Failed to generate signature: " + e.getMessage(), e);
        }

        return new String(signature);
    }
    public  String getISO8601Timestamp(Date date){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(date);
        return nowAsISO;
    }


    @OnClick(R.id.but)
    public void onViewClicked() {
        OkGo.<String>post("https://mws.amazonservices.com/").params(params).tag(this).execute(new Callback<String>() {
            @Override
            public void onStart(Request<String, ? extends Request> request) {
                    Log.e("okgo",request.getParams().toString());
            }

            @Override
            public void onSuccess(Response<String> response) {
                text.setText(response.body());
            }

            @Override
            public void onCacheSuccess(Response<String> response) {

            }

            @Override
            public void onError(Response<String> response) {
                text.setText(response.body());
            }

            @Override
            public void onFinish() {
                Log.e("okgo","onfinish");
            }

            @Override
            public void uploadProgress(Progress progress) {

            }

            @Override
            public void downloadProgress(Progress progress) {

            }

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                return null;
            }
        });
    }
}
