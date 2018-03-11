
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nordicsemi.nrfUARTv2;




import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();


    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    int LocpermissionCheck;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend,btn1,btn2,btn3;
    private EditText edtMessage;
    private TextView stateText;
    private TextView redText;
    private TextView greenText;
    private TextView elapsedText;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private int ALPDelay = 10000;
    public static Location currentLocation;
    public static Location destination;
    private float  timeRed=0.0f;
    private float timeGreen=0.0f;
    private float elapsedTime=0.0f;
    private int state=0;
    private int ledControl=0;
    private int locationUpdates=0;
    private Date timeOfmeasurement;
    FirebaseDatabase database;
    DatabaseReference myRef;


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        LocpermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (LocpermissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "The permission to get BLE and GPS location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }

        }else{
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationRequest();
        }
        timeOfmeasurement=new Date();
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect=(Button) findViewById(R.id.btn_select);
        btnSend=(Button) findViewById(R.id.sendButton);
        btn1 =(Button) findViewById(R.id.button2);
        btn2=(Button) findViewById(R.id.button3);
        btn3=(Button) findViewById(R.id.button4);

        edtMessage = (EditText) findViewById(R.id.sendText);
        stateText = (TextView) findViewById(R.id.stateText);
        redText = (TextView) findViewById(R.id.redTime);
        greenText = (TextView) findViewById(R.id.greenTime);
        elapsedText = (TextView) findViewById(R.id.elapsedTime);
        if(state==0)
            stateText.setText("Red");
        else
            stateText.setText("Green");
        redText.setText(Float.toString(timeRed));
        greenText.setText(Float.toString(timeGreen));
        elapsedText.setText(Float.toString(elapsedTime));


        service_init();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                }
                if(ledControl==1&&timeRed!=0&&timeGreen!=0) {
                    calculateLed();
                }
            }
        };


        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                	if (btnConnectDisconnect.getText().equals("Connect")){
                		
                		//Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                		
            			Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            			startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        			} else {
        				//Disconnect button pressed
        				if (mDevice!=null)
        				{
        					mService.disconnect();
                            ledControl=0;
        					
        				}
        			}
                }
            }
        });
        // Handle Send button
        btnSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
            	EditText editText = (EditText) findViewById(R.id.sendText);
                String message = editText.getText().toString();

                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    listAdapter.add("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    edtMessage.setText("");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        btn1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(locationUpdates==0) {
                    startLocationUpdates();
                    locationUpdates = 1;
                }
                else if(locationUpdates==1) {
                    stopLocationUpdates();
                    locationUpdates=0;
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(ledControl==0)
                    ledControl=1;
                else if (ledControl==1)
                    ledControl=0;
                }
        });

        btn3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                destination.setLatitude(currentLocation.getLatitude());
                destination.setLongitude(currentLocation.getLongitude());
                myRef.setValue(String.valueOf(String.valueOf(destination.getLatitude())+"-"+destination.getLongitude()));
            }
        });

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("destination");
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                destination= new Location("");
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String temp= dataSnapshot.getValue(java.lang.String.class);
                if(temp!=null) {
                    String[] parts = temp.split("-");
                    destination.setLatitude(Double.parseDouble(parts[0]));
                    destination.setLongitude(Double.parseDouble(parts[1]));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000, 10000); //
    }
    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        double lat = (currentLocation.getLatitude());
                        double lng =  (currentLocation.getLongitude());
                        double spd =  (currentLocation.getSpeed());
                        String latString=String.valueOf(lat);
                        String lngString=(String.valueOf(lng));
                        String spdString=String.valueOf(spd);
                        String message=latString+"-"+lngString+"-"+spdString;
                        //String message = currentLocation.toString();
                        byte[] value;
                        try {
                            //send data to service
                            value = message.getBytes("UTF-8");
                            mService.writeRXCharacteristic(value);
                            //Update the log with time stamp
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            edtMessage.setText("");
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                });
            }
        };
    }



    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

        }

        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        
        //Handler events that received from UART service 
        public void handleMessage(Message msg) {
  
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
           //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_CONNECT_MSG");
                             btnConnectDisconnect.setText("Disconnect");
                             edtMessage.setEnabled(true);
                             btnSend.setEnabled(true);
                             ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                             listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                             mState = UART_PROFILE_CONNECTED;
                     }
            	 });
            }
           
          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                    	 	 String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_DISCONNECT_MSG");
                             btnConnectDisconnect.setText("Connect");
                             edtMessage.setEnabled(false);
                             btnSend.setEnabled(false);
                             ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                             listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                             mState = UART_PROFILE_DISCONNECTED;
                             mService.close();
                            //setUiState();
                         
                     }
                 });
            }
            
          
          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	 mService.enableTXNotification();
            }
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
              
                 final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                 runOnUiThread(new Runnable() {
                     public void run() {
                         try {
                         	String text = new String(txValue, "UTF-8");
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             setTimingvalues(text,new Date());
                             if(text.length()!=20) {
                                 listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                                 messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                             }

                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });
             }
           //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
            	showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
            }
            
            
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
       
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
               
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
                            

            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong request code");
            break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
       
    }

    
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.popup_title)
            .setMessage(R.string.popup_message)
            .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
   	                finish();
                }
            })
            .setNegativeButton(R.string.popup_no, null)
            .show();
        }
    }
    public void calculateLed()
    {
        int filled=0;
        boolean foundCurrentSpeed=false;
        boolean foundClosestLowestSpeed=false;
        boolean foundClosestHigherSpeed=false;
        float lowerSpeed1;
        float lowerSpeed2;
        float lowerSpeedAvg;
        float higherSpeed1;
        float higherSpeed2;
        float higherSpeedAvg;

        Date currentTime=new Date();

        float timeDifference=currentTime.getTime()-timeOfmeasurement.getTime(); //inMiliseconds

        //float distance=currentLocation.distanceTo(destination); // in meters
        float[] distance={0.0f};
        Location.distanceBetween(currentLocation.getLatitude(),currentLocation.getLongitude(),destination.getLatitude(),destination.getLongitude(),distance);
        listAdapter.add("lat: "+ String.valueOf(destination.getLatitude())+"lon:"+String.valueOf(destination.getLongitude())+"distance:"+String.valueOf(distance[0]));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        int numberOfGreenTimes=10;
        int index;

        float[] times=new float[numberOfGreenTimes];
        float[] speedIntervals=new float[numberOfGreenTimes];
        float realTimeElapsed=elapsedTime+(timeDifference/1000);
        int realState=state;

        //update state and realElapsedTime
        while(((realState==1)&&realTimeElapsed>timeGreen)||((realState==0)&&(realTimeElapsed>timeRed)))
        {
            if(realState==1)
            {
                realTimeElapsed=realTimeElapsed-timeGreen;
                realState=0;
               // Log.w(TAG, "State was 1 but 10 seconds have elapsed"+Float.toString(realTimeElapsed));
            }
            else if (realState==0)
            {
                realTimeElapsed=realTimeElapsed-timeRed;
                realState=1;
                //Log.w(TAG, "State was 0 but 10 seconds have elapsed"+Float.toString(realTimeElapsed));
            }
        }
        elapsedText.setText(Float.toString(realTimeElapsed));
        /*
        listAdapter.add("realTimeElapsed: "+ String.valueOf(realTimeElapsed));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        listAdapter.add("timeGreen: "+ String.valueOf(timeGreen));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        listAdapter.add("timeRed: "+ String.valueOf(timeRed));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        listAdapter.add("state: "+ String.valueOf(state));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        */
        if(realState==0)
            stateText.setText("Red");
        else
            stateText.setText("Green");


        if(distance[0]!=0) {
            //set first 2 time points
            if (state == 1) {
                times[0] = 0;
                speedIntervals[0] = 100;
                times[1] = (timeGreen - realTimeElapsed);
                speedIntervals[1] = (distance[0] / times[1])*3.6f;
            } else if (state == 0) {
                times[0] = timeRed - realTimeElapsed;
                speedIntervals[0] = (distance[0] / times[0])*3.6f;
                times[1] = times[0] + timeGreen;
                speedIntervals[1] = (distance[0] / times[1])*3.6f;
            }
            index = 2;
            while (index < numberOfGreenTimes) //fill up array with additional time points
            {
                times[index] = times[index - 1] + timeRed;
                speedIntervals[index] = (distance[0] / times[index])*3.6f;
                listAdapter.add("Time-"+index+": "+ String.valueOf(times[index]));
                messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                listAdapter.add("Speeds-"+index+": "+ String.valueOf(speedIntervals[index]));
                messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                index++;
                times[index] = times[index - 1] + timeGreen;
                speedIntervals[index] = (distance[0] / times[index])*3.6f;
                index++;
            }
            index = 0;
            while (index < numberOfGreenTimes) {
                if (currentLocation.getSpeed() > speedIntervals[index])  //meters/second
                {
                    if ((!foundCurrentSpeed) && (index+1)<numberOfGreenTimes&&(currentLocation.getSpeed() < speedIntervals[index + 1]))
                        foundCurrentSpeed = true;
                    if (!foundClosestLowestSpeed) {
                        foundClosestLowestSpeed = true;
                        lowerSpeed1 = speedIntervals[index];
                        lowerSpeed2 = speedIntervals[index++];
                        lowerSpeedAvg = (lowerSpeed2 + lowerSpeed1) / 2;
                        listAdapter.add("Recommended lowerSpeed: "+ String.valueOf(lowerSpeedAvg));
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    }
                    if (!foundClosestHigherSpeed && index >= 2) {
                        foundClosestHigherSpeed = true;
                        higherSpeed1 = speedIntervals[index - 2];
                        higherSpeed2 = speedIntervals[index - 1];
                        higherSpeedAvg = (higherSpeed1 + higherSpeed2) / 2;
                        listAdapter.add("Recommended higherSpeed: "+  String.valueOf(higherSpeedAvg));
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    }
                }
                index++;
            }
        }



        String message ="1111111";
        sendBLE(message);

    }
    public void sendBLE(String message)
    {
        byte[] value;
        try {
            //send data to service
            value = message.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
            edtMessage.setText("");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setTimingvalues(String message,Date time)
    {
        if(message.length()>=20)
        {
            timeOfmeasurement=time;
            String[] split = message.split(",");
            timeRed = (float)(Integer.decode("0x"+split[3] + split[4]))/10;
            timeGreen = (float)(Integer.decode("0x"+split[5] + split[6]))/10;
            elapsedTime = (float)(Integer.decode("0x"+split[1] + split[2]))/10;
            state = Integer.parseInt(split[0]);
            //listAdapter.add("Parsed: "+ String.valueOf(state)+","+String.valueOf(elapsedTime)+","+String.valueOf(timeRed)+","+String.valueOf(timeGreen));
            if(state==0)
                stateText.setText("Red");
            else
                stateText.setText("Green");
            redText.setText(Float.toString(timeRed));
            greenText.setText(Float.toString(timeGreen));
            elapsedText.setText(Float.toString(elapsedTime));
        }

    }

}
