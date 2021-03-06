package com.itpteam11.visualisemandai;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 *  This service retrieves climatic data from NEA and OpenWeather with the XML text from their API
 *  There are 3 climate types: PSI, temperature and weather
 *  Notification and notification-lookup node in Firebase will be updated if the data retrieved
 *  met certain conditions.
 *  Notifications will then be sent (refer to NotificationFragment)
 */
public class CheckClimateService extends IntentService {
    private static final String TAG = "CheckClimateService";

    public final static String USER_ID = "userID";
    public final static String[] CLIMATE_TYPES = {"psi", "temperature", "weather"};

    private String userID;

    public CheckClimateService() {
        super("CheckClimateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Get authenticated user ID from Intent
        userID = intent.getStringExtra(USER_ID);

        //Check value of every climate type
        for (int i = 0; i < CLIMATE_TYPES.length; i++) {
            //Get database reference node of the climate type
            Log.d("CLIMATETYPE", CLIMATE_TYPES[i]);
            FirebaseDatabase.getInstance().getReference().child("service").child(CLIMATE_TYPES[i]).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Store climate's details into Climate object
                    Climate climate = new Climate();
                    climate.setValue(dataSnapshot.child("value").getValue(Object.class));

                    if(dataSnapshot.child("valueLong").exists()) {
                        climate.setValueLong(dataSnapshot.child("valueLong").getValue(String.class));
                    }

                    //Retrieve HTML document from a URL that is contain inside the newly created climate object
                    //Process is done using AsyncTask method
                    HTMLParsingAsyncTask htmlParsing = new HTMLParsingAsyncTask();
                    htmlParsing.execute(climate, dataSnapshot.getKey(), userID);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to get climate value
                    Log.v(TAG, "Failed to get climate: " + error.toException());
                }
            });
        }
    }


    /**
     * AsyncTask for getting and parsing XML content to get climate type's value
     */
    private class HTMLParsingAsyncTask extends AsyncTask<Object, Void, String> {
        private Climate climate;
        private String climateType;
        private String userID;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * To download URL content of API and parse XML of the content after the user
         * has logged in
         * @param params
         * @return
         */
        @Override
        protected String doInBackground(Object... params) {
            climate = (Climate) params[0];
            climateType = (String) params[1];
            userID = (String) params[2];

            String result = "";

            String weatherURL = "http://www.nea.gov.sg/api/WebAPI/?dataset=2hr_nowcast&keyref=781CF461BB6606ADEA01E0CAF8B35274D184749DB043FB09";
            String psiURL = "http://www.nea.gov.sg/api/WebAPI/?dataset=psi_update&keyref=781CF461BB6606ADEA01E0CAF8B35274D184749DB043FB09";
            String tempURL = "http://api.openweathermap.org/data/2.5/weather?lat=1.404043&lon=103.793045&appid=179acd6a18cfec63680175ff28ffdb06&mode=xml";
            String url = "";
            if (climateType.equals("weather")) {
                url = weatherURL;
            } else if (climateType.equals("psi")) {
                url = psiURL;
            } else if (climateType.equals("temperature")) {
                url = tempURL;
            }

            Log.v(TAG, "Climate type: " + climateType);

            try {
                String data = downloadUrlHTTP(url);
                Log.v(TAG, "Done downloading URL");
                result = parseXML(data);
                Log.v(TAG, "Done parsing XML");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return result;
        }

        /**
         * Update notification and notification-lookup nodes based on the result after parsing the XML
         * from NEA and OpenWeather API
         * @param result
         */
        @Override
        protected void onPostExecute(final String result) {
            Log.v(TAG, "In onPostExecute");
            Log.v(TAG, "Climate value in database: " + climate.getValue());
            Log.v(TAG, "Climate value from API: " + result);

            if (!climate.getValue().equals(result)) {
                //Create notification

                final DatabaseReference climateRef = FirebaseDatabase.getInstance().getReference().child("service").child(climateType);
                Notification notification = new Notification();
                String content = "";
                String sender = "";

                climateRef.child("value").setValue(result);

                // If climate type 'weather' is being processed from NEA
                if (climateType.equals("weather")) {
                    // Retrieves the translated weather description from the abbreviation parsed
                    climateRef.child("abbreviations").child(result).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            // Save the long description of abbreviation in db
                            String valueLong = dataSnapshot.getValue().toString();
                            climateRef.child("valueLong").setValue(valueLong);

                            // Set up content for notification
                            Notification weatherNotification = new Notification();
                            String content = "Weather alert: " + valueLong;
                            String sender = "Climatic live alerts - Weather ";

                            // Abbreviations of weather considered as rainy
                            String[] rainyWeather = new String[]{"DR", "HG", "HR", "HS", "HT", "LR", "LS", "PS", "RA", "SH", "SK", "SR", "TL", "WR", "WS"};
                            List rainyAbbrList = Arrays.asList(rainyWeather);

                            if (rainyAbbrList.contains(result) || result.equals("SU")) {
                                if (rainyAbbrList.contains(result)) {
                                    weatherNotification.setSender(sender + "(Rain)");
                                } else if (result.equals("SU")) {
                                    weatherNotification.setSender(sender + "(Sun)");
                                }
                                weatherNotification.sendServiceNotification(Notification.WEATHER_SERVICE, content, sender, result);
                                Log.v(TAG, "Climate Service - Weather value changes");
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.v(TAG, "Failed to get description for weather abbreviation: " + databaseError.toException());
                        }
                    });

                }
                // If climate type 'psi' is being processed from NEA
                else if (climateType.equals("psi")) {
                    if(!result.equals("")) {
                        int psi = Integer.parseInt(result);
                        if (psi >= 101 || psi > 300) {
                            content = "Haze alert: PSI " + psi; //getRangeDesriptor(psi);
                            sender = "Climatic live alerts - PSI";

                            notification.sendServiceNotification(Notification.PSI_SERVICE, content, sender, result);

                            Log.v(TAG, "PSI value changes");
                        }
                        else {
                            climateRef.child("value").setValue(result);
                        }
                    }
                    else {
                        climateRef.child("value").setValue("0");
                    }
                }

                // If climate type 'temperature' is being processed from OpenWeather
                else if (climateType.equals("temperature")) {
                    if (!result.equals("")) {
                        double temp = Double.parseDouble(result);
                        if (temp > 30) {
                            content = "Temperature alert: " + temp;
                            sender = "Climatic live alerts - Temperature";

                            notification.sendServiceNotification(Notification.TEMPERATURE_SERVICE, content, sender, result);

                            Log.v(TAG, "Temperature value changes");
                        } else {
                            climateRef.child("value").setValue(result);
                        }
                    }
                }
                else {
                    climateRef.child("value").setValue("0");
                }


            } else {
                //Create no time change notification to the requested user
                Log.v(TAG, "User ID: " + userID);
            }
        }

        /**
         * Download of XML from NEA/OpenWeather API
         * @param myurl
         * @return
         * @throws IOException
         */
        private String downloadUrlHTTP(String myurl) throws IOException {
            BufferedReader reader = null;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                int read;
                char[] chars = new char[1024];
                while ((read = reader.read(chars)) != -1)
                    buffer.append(chars, 0, read);

                myurl = buffer.toString();
            } finally {
                if (reader != null)
                    reader.close();
            }
            return myurl;
        }

        /**
         * To parse the XML from url content of API and grab the value that is required from the XML
         * @param xml
         * @return
         * @throws XmlPullParserException
         * @throws IOException
         */
        private String parseXML(String xml) throws XmlPullParserException, IOException {

            // Create the pull parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            // Set the parser's input to be the XML document in the HTTP Response
            parser.setInput(new StringReader(xml));

            // Get the first parser event and start iterating over the XML document
            int eventType = parser.getEventType();
            DatabaseReference envRef = FirebaseDatabase.getInstance().getReference().child("service");
            String value = "";

            loop:
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        // If weather API
                        if (tagName.equals("area")) {
                            if (parser.getAttributeValue(null, "name").equals("Mandai")) {
                                value = parser.getAttributeValue(null, "forecast");
                                break loop;
                            }
                        }
                        // If psi API
                        else if (tagName.equals("id")) {
                            if (parser.next() == XmlPullParser.TEXT) {
                                if (parser.getText().equals("rNO")) {
                                    while (true) {
                                        if (parser.next() == XmlPullParser.START_TAG) {
                                            if (parser.getName().equals("reading")) {
                                                if (parser.getAttributeValue(null, "type").equals("NPSI_PM25_3HR")) {
                                                    value = parser.getAttributeValue(null, "value");
                                                    break loop;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // If temperature API
                        else if (tagName.equals("temperature")) {
                            double tempKelvin = Double.parseDouble(parser.getAttributeValue(null, "value"));
                            double tempCelsius = Math.round((tempKelvin / 10.554) * 100.0) / 100.0;
                            value = Double.toString(tempCelsius);
                            break loop;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
            return value;
        }

        /**
         * Return descriptor - what PSI value represents to set in notification
         * @param psi
         * @return
         */
        private String getRangeDesriptor(int psi){
            String descriptor = "";
            if (psi>=101 && psi<=200) {
                descriptor = "PSI is in unheathy range";
            } else if (psi>=201 && psi<=300){
                descriptor = "PSI is in very unheathy range";
            } else if (psi>300) {
                descriptor = "PSI is in hazardous range";
            }
            return descriptor;
        }

    }
}
