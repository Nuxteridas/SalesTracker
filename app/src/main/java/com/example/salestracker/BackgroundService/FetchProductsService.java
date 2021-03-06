package com.example.salestracker.BackgroundService;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.example.salestracker.ItemRecommendation;
import com.example.salestracker.MainActivity;
import com.example.salestracker.R;
import com.example.salestracker.SingleProduct;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.example.salestracker.BackgroundService.NotificationChan.CHANNEL_1_ID;
import static com.example.salestracker.MainActivity.favourites;

public class FetchProductsService extends JobService {

    private final String LOG_TAG = "Dennis";
    private String keyword;
    //private String title = "Apple iPhone 5s 16GB Factory Unlocked SRF A1586";
    //private double currentPrice = 50.00;
    private SingleProduct product;
    private ItemRecommendation recProduct;
    private String jsonString;
    //private String code = "283492091677";
    private String globalId = "0";




    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("BootReceiver", "Job started");
        doBackgroundWork(params, this);
        //FetchProductsTask task = new FetchProductsTask("aKeyword", null);
        //task.execute();

        return true;
    }

    private void doBackgroundWork(final JobParameters params, final Context c) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0; i < favourites.size(); i++) {
                    int action = 0;
                    String code = favourites.get(i).getItemId().get(0);
                    CheckAvailability( code, action );
                    if (product.getAck().equals( "Failure" ) || !product.getItem().getTitle().equals( favourites.get(i).getTitle(0) )) {

                        //Product doesn't exist anymore! (RIP you should have order it earlier)
                        BuildNotification( getApplicationContext(), "Product out of stock", favourites.get(i).getTitle(0) + " has gone out of stock", action );
                    } else if (product.getItem().getConvertedCurrentPrice().getValue() < favourites.get(i).getSellingStatus().get(0).getPriceDetails().get(0).get__value__()) {
                        //Notification to inform the user about the price drop.
                        BuildNotification( getApplicationContext(), "Price dropped", favourites.get(i).getTitle(0) + " price has dropped", action );

                    } else {
                        action = 1;
                        //CheckAvaialability(code, action);
                        ItemRecommendation item = CheckSimilar( code );
                        Log.d( "JOJO", String.valueOf( recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getTitle() ) );
                        if (recProduct.getGetSimilarItemsResponse().getAck().equals( "Success" )) {
                            if (recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().size() != 0) {
                                if (recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getCurrentPrice() == null) {
                                    if (recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getBuyItNowPrice() != null ||
                                            !recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getBuyItNowPrice().get__value__().equals( "0.0" ) &&
                                                    Double.parseDouble( recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getBuyItNowPrice().get__value__() ) < favourites.get(i).getSellingStatus().get(0).getPriceDetails().get(0).get__value__())
                                        BuildNotification( FetchProductsService.this, "Similar Product", "There's a new product similar to the one that you have in your list", action );
                                    else
                                        Log.v( "Dennis", "Buy it now is null or 0.0 or bigger than current price" );
                                } else if (Double.parseDouble( recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get( 0 ).getCurrentPrice().get__value__() ) < favourites.get(i).getSellingStatus().get(0).getPriceDetails().get(0).get__value__()) {
                                    BuildNotification( FetchProductsService.this, "Similar Product", "There's a new product similar to the one that you have in your list", action );
                                } else
                                    Log.v( "Dennis", "There is not a cheaper similar product" );
                            } else
                                Log.v( "Dennis", "List with similar products is empty" );

                        } else
                            Log.d( "JOJO", "Failed Similar Products" );

                    }

                    Log.d( "BootReceiver", "Thread running" );
                    jobFinished( params, false );
                }
            }
        }).start();
    }

    public void CheckAvailability(String code, int action){
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        Log.d(LOG_TAG,"CheckAvailability");

        try{
            Uri builtUri;
            String basicUrl;
            String appid;
            String version;
            String itemId;

            if(action == 0) {
                basicUrl = "https://open.api.ebay.com/shopping?";
                String call = "callname";
                String datatype = "responseencoding";
                appid = "appid";
                String siteId = "siteid";
                version = "version";
                itemId = "itemID";

                builtUri = Uri.parse( basicUrl ).buildUpon()
                        .appendQueryParameter(call, "GetSingleItem" )
                        .appendQueryParameter(datatype, "JSON" )
                        .appendQueryParameter(appid, "Dionisis-SmartSho-PRD-0388a6d5f-56b83621" )
                        .appendQueryParameter(siteId, globalId)
                        .appendQueryParameter(version, "967" )
                        .appendQueryParameter("includeSelector", "Details,ShippingCost")
                        .appendQueryParameter(itemId, code )
                        .build();
            }
            else{

                basicUrl = "https://svcs.ebay.com/MerchandisingService?";
                String operation = "OPERATION-NAME";
                String servName = "SERVICE-NAME";
                version = "SERVICE-VERSION";
                appid = "CONSUMER-ID";
                String dataType = "RESPONSE-DATA-FORMAT";
                String payload = "REST-PAYLOAD";
                itemId = "itemId";
                String results = "maxResults";

                builtUri = Uri.parse( basicUrl ).buildUpon()
                        .appendQueryParameter( operation, "getSimilarItems" )
                        .appendQueryParameter( servName, "MerchandisingService" )
                        .appendQueryParameter( version, "1.1.0" )
                        .appendQueryParameter( appid, "Dionisis-SmartSho-PRD-0388a6d5f-56b83621" )
                        .appendQueryParameter(dataType, "JSON")
                        .appendQueryParameter(payload, "")
                        .appendQueryParameter( itemId, code)
                        .appendQueryParameter(results, "1")
                        .build();

            }


            URL url = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream != null) {
                // Nothing to do.
                //return null;
                reader = new BufferedReader(new InputStreamReader(inputStream));
            }


            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
               //return null;
            }

            jsonString = buffer.toString();
            Log.v(LOG_TAG, "Forecast JSON String: " + jsonString);
            Gson gson = new Gson();
            product = gson.fromJson(jsonString, SingleProduct.class );


        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            //return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }


        Log.v("JOJO", product.getAck());
       //return product;
    }

    public ItemRecommendation CheckSimilar(String code){
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;


        try{

            String basicUrl = "https://svcs.ebay.com/MerchandisingService?";
            String operation = "OPERATION-NAME";
            String servName = "SERVICE-NAME";
            String version = "SERVICE-VERSION";
            String appid = "CONSUMER-ID";
            String dataType = "RESPONSE-DATA-FORMAT";
            String payload = "REST-PAYLOAD";
            String itemId = "itemId";
            String results = "maxResults";

            Uri builtUri = Uri.parse( basicUrl ).buildUpon()
                    .appendQueryParameter(operation, "getSimilarItems" )
                    .appendQueryParameter(servName, "MerchandisingService" )
                    .appendQueryParameter(version, "1.1.0" )
                    .appendQueryParameter(appid, "Dionisis-SmartSho-PRD-0388a6d5f-56b83621" )
                    .appendQueryParameter(dataType, "JSON")
                    .appendQueryParameter(payload, "")
                    .appendQueryParameter( itemId, code)
                    .appendQueryParameter(results, "1")
                    .build();

            URL url = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }

            jsonString = buffer.toString();
            Log.v(LOG_TAG, "Forecast JSON String: " + jsonString);
            Gson gson = new Gson();
            recProduct = gson.fromJson(jsonString, ItemRecommendation.class);

        }catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        Log.v( "Look at me", String.valueOf( recProduct.getGetSimilarItemsResponse().getItemRecommendations().getItem().get(0).getTitle() ));
        return recProduct;
    }

    public void BuildNotification(final Context context, final String Title, final String msg, int action){
        Intent intent = new Intent(context, MainActivity.class);
        if(action == 1)
            intent.putExtra("favourite", "popUp");
        else
            intent.putExtra("favourite", "favouriteMenu");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder( Objects.requireNonNull(context), CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_favourites)
                .setContentTitle(Title)
                .setContentText(msg)
                .setVibrate(new long[] {1000, 1000})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setColor( Color.BLUE )
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .setOnlyAlertOnce( true )
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, notification);
    }


    @Override
    public boolean onStopJob(JobParameters params) {
        Log.v("BootReceiver", "Job cancelled");
        return true;
    }
}
