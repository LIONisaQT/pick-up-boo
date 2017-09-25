package io.github.lionisaqt.pickupboo;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

// Class for shortening a link with Google's URL shortener API
// Needs to be done asynchronously (makeShort() will need some help with that)
public class ShortURL {

    private static ShortUrlListener _listener = null;

    public abstract static interface ShortUrlListener {
        public abstract void OnFinish(String url);
    }

    /**
     * Create short URL's synchronously
     * @param longUrl
     */
    public static String makeShort(final String longUrl) {

        String url = longUrl;
        String json = connect(longUrl);

        try{
            JSONObject jsonObj = new JSONObject(json);

            if(jsonObj.has("id")){
                String id = jsonObj.getString("id");
                if(id != null && 0 < id.length()) {
                    url = id;
                } else {
                    Log.e("ShortURL", "error");
                }
            } else {
                Log.e("ShortURL", "error");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * Create short URL's asynchronously
     * @param longUrl
     * @param listener
     */
    public static void makeShortUrl(final String longUrl, ShortUrlListener listener) {

        _listener = listener;

        new Thread(new Runnable() {
            @Override
            public void run() {

                String url = longUrl;
                String json = connect(longUrl);

                try{

                    JSONObject jsonObj = new JSONObject(json);

                    if(jsonObj.has("id")){
                        String id = jsonObj.getString("id");
                        if(id != null && 0 < id.length()) {
                            url = id;
                        }
                    }

                } catch(Exception e) {
                    Log.e("ShortURL", "error");
                }

                Message msg = new Message();
                msg.what = 100;
                msg.obj = url;
                mHandler.sendMessage(msg);

            }
        }).start();
    }

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    if(_listener != null){
                        _listener.OnFinish((String)msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private static String connect(String longUrl) {
        /*
        Please create a server key or browser key.
        Please refer to the part “Acquiring and using an API key” in the below URL.
        In regard to Quotas, please refer to the part “Quotas” in the below URL.
        https://developers.google.com/url-shortener/v1/getting_started
        */
        String KEY = "AIzaSyBzNaA7v1ZW8fpM4FoPuXi0g3fx9p3ZnXc";
        String URL = "https://www.googleapis.com/urlshortener/v1/url?shortUrl=http://goo.gl/fbsS&key=";
        String res = "";

        try
        {
            URLConnection conn = new URL(URL+KEY).openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("{\"longUrl\":\"" + longUrl + "\"}");
            wr.flush();

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String sResponse;
            StringBuilder s = new StringBuilder();

            while ((sResponse = rd.readLine()) != null)
            {
                s = s.append(sResponse);
            }

            res = s.toString();

            wr.close();
            rd.close();
        } catch (Exception e) {
            Log.e("ShortURL", "error");
        }

        return res;
    }

}
