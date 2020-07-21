/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.co.firmtech.ble_gpio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    
    byte sendByte[] = new byte[20]; 
	private ImageView iv1,iv2,iv3,iv4,iv5,iv6,iv7,iv8;
	private ImageView iv11,iv22,iv33,iv44,iv55,iv66,iv77,iv88;
	private TextView tvadc0, tvadc1;
	private SeekBar seekBar1, seekBar2;
	private TextView pwm0Value, pwm1Value, adcTimeText;
	private byte portStatus;
	private byte ledStatus = 0;
	private byte pwmStatus;
	private byte linklossStatus;
	private byte adctimeHighStatus;
	private byte adctimeLowStatus;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            	
            	getDeviceSetting();
            	
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	
            	byte[] sendByte = intent.getByteArrayExtra("init");
            	
            	
            	
            	if((sendByte[0] == 0x55) && (sendByte[1] == 0x33)){
            		Log.d(TAG,"======= Init Setting Data ");
            		updateCommandState("Init Data");

            		
            		portStatus = sendByte[2];
            		pwmStatus = sendByte[3];
            		linklossStatus = sendByte[4];
            		
            		adctimeHighStatus = sendByte[5];
            		adctimeLowStatus = sendByte[6];
            		
            		setPortStatus(portStatus);
            		setPWMStatus(pwmStatus);
            		setAdcTimeStatus(adctimeHighStatus,adctimeLowStatus);
            		
            		ledStatus = (byte) 0xff;
            		sendLedStatus(); 
            		
            		
            	} else if((sendByte[0] == 0x55) && (sendByte[1] == 0x00)){
            		Log.d(TAG,"======= PIO READ NOTIFY ");
            		updateCommandState("PIO READ");
            		
            		byte notifyValue = sendByte[2];
            		
            		updateReadPort(notifyValue);
            	}
	        	else if((sendByte[0] == 0x55) && (sendByte[1] == 0x01)){
	        		Log.d(TAG,"======= ADC0 READ NOTIFY ");
            		updateCommandState("ADC READ");

	        		
	        		byte notifyValue = sendByte[2];
					byte notifyValue2 = sendByte[3];
	        		
	        		updateADC0(notifyValue, notifyValue2);
	        	}
	        	else if((sendByte[0] == 0x55) && (sendByte[1] == 0x02)){
	        		Log.d(TAG,"======= ADC1 READ NOTIFY ");
            		updateCommandState("ADC READ");
	        		
	        		byte notifyValue = sendByte[2];
					byte notifyValue2 = sendByte[3];
	        		
	        		updateADC1(notifyValue, notifyValue2);
	        	}
            		
            	
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    
    private void getDeviceSetting(){
    	
    	
    	if(mGattCharacteristics != null){ 
    		final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(6).get(0);
    		mBluetoothLeService.readCharacteristic(characteristic);
    	}
    }
    
    private void setPortStatus(byte status){
    	
    	if((status & 0x80) == 0x80){
    		
    		
    		iv8.setImageResource(R.drawable.one);
    		iv88.setImageResource(R.drawable.x);
    		
    		 iv8.setVisibility(View.VISIBLE);
    	}
    	else {
    		iv8.setImageResource(R.drawable.x);
    		iv88.setImageResource(R.drawable.zero);

    		iv88.setVisibility(View.VISIBLE);

    	}
    	if((status & 0x40) == 0x40){
    		
    		iv7.setImageResource(R.drawable.one);
    		iv77.setImageResource(R.drawable.x);
    		
   		 	iv7.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv7.setImageResource(R.drawable.x);
    		iv77.setImageResource(R.drawable.zero);
    		
   		    iv77.setVisibility(View.VISIBLE);

    	}
    	if((status & 0x20) == 0x20){
    		
    		iv6.setImageResource(R.drawable.one);
    		iv66.setImageResource(R.drawable.x);
    		
   		 iv6.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv6.setImageResource(R.drawable.x);
    		iv66.setImageResource(R.drawable.zero);
    		
   		 iv66.setVisibility(View.VISIBLE);

    	}
    	if((status & 0x10) == 0x10){
    		
    		iv5.setImageResource(R.drawable.one);
    		iv55.setImageResource(R.drawable.x);
    		
   		 iv5.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv5.setImageResource(R.drawable.x);
    		iv55.setImageResource(R.drawable.zero);
    		
   		 iv55.setVisibility(View.VISIBLE);

    	}  
    	if((status & 0x08) == 0x08){
    		
    		iv4.setImageResource(R.drawable.one);
    		iv44.setImageResource(R.drawable.x);
    		
   		 iv4.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv4.setImageResource(R.drawable.x);
    		iv44.setImageResource(R.drawable.zero);
    		
   		 iv44.setVisibility(View.VISIBLE);

    	}       	
    	if((status & 0x04) == 0x04){
    		
    		iv3.setImageResource(R.drawable.one);
    		iv33.setImageResource(R.drawable.x);
    		
   		 iv3.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv3.setImageResource(R.drawable.x);
    		iv33.setImageResource(R.drawable.zero);
    		
   		 iv33.setVisibility(View.VISIBLE);

    	}       	
    	if((status & 0x02) == 0x02){
    		
    		iv2.setImageResource(R.drawable.one);
    		iv22.setImageResource(R.drawable.x);
    		
   		 iv2.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv2.setImageResource(R.drawable.x);
    		iv22.setImageResource(R.drawable.zero);
    		
   		 iv22.setVisibility(View.VISIBLE);

    	}       	
    	if((status & 0x01) == 0x01){
    		
    		iv1.setImageResource(R.drawable.one);
    		iv11.setImageResource(R.drawable.x);
    		
   		 iv1.setVisibility(View.VISIBLE);

    		
    	}
    	else {
    		iv1.setImageResource(R.drawable.x);
    		iv11.setImageResource(R.drawable.zero);
    		
   		 iv11.setVisibility(View.VISIBLE);

    	}       	
    	
    	
    	Handler mHandler = new Handler();
    	mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
		    	// notification enable
		    	final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(4).get(0);
		        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
			}
    	   
    	}, 1000);
    	

    	Handler mHandler2 = new Handler();
    	mHandler2.postDelayed(new Runnable() {

			@Override
			public void run() {
		    	// notification enable
		    	final BluetoothGattCharacteristic characteristicADC0 = mGattCharacteristics.get(5).get(0);
		        mBluetoothLeService.setCharacteristicNotification(characteristicADC0, true);
			}
    	   
    	}, 2000);
    	
    	Handler mHandler3 = new Handler();
    	mHandler3.postDelayed(new Runnable() {

			@Override
			public void run() {
		    	// notification enable
		    	final BluetoothGattCharacteristic characteristicADC1 = mGattCharacteristics.get(5).get(1);
		        mBluetoothLeService.setCharacteristicNotification(characteristicADC1, true);
			}
    	   
    	}, 3000);
        
       
        
        
    }
    
    
    private void setPWMStatus(byte status){
    	
    	if(status == 0x00){
    		seekBar1.setVisibility(View.INVISIBLE);
    		seekBar2.setVisibility(View.INVISIBLE);
    		
    		
    		
    		
    	} else {
    		seekBar1.setVisibility(View.VISIBLE);
    		seekBar2.setVisibility(View.VISIBLE);
    		
	  		  iv1.setVisibility(View.INVISIBLE);
	  		  iv2.setVisibility(View.INVISIBLE);
	  		  iv11.setVisibility(View.INVISIBLE);
	  		  iv22.setVisibility(View.INVISIBLE);
    	}
    }
    
    public static String adcTimeToString(long time){
    	
    	long min = time / 60;
    	long sec = time % 60;
    	
    	sec = time;
    	
    	//if(min == 0){
    		return String.format(" %d ",sec);
    		
    //	}else {
    		
    //		return String.format("%d min %d ",min,sec);
    //	}
    }
    
    
    private void setAdcTimeStatus(byte high, byte low){
    	
    	long aa;
    	long bb;
    	
    	if((high == 0) && (low == 0)){
    		tvadc0.setVisibility(View.INVISIBLE);
    		tvadc1.setVisibility(View.INVISIBLE);    		
    	} else { 
    		tvadc0.setVisibility(View.VISIBLE);
    		tvadc1.setVisibility(View.VISIBLE); 
    		
    		tvadc0.setText("0x00, 0x00");
    		tvadc1.setText("0x00, 0x00");
    		
    		
    		aa = (int)low & 0x00ff;
    		bb = (int)high & 0x00ff;
    		
    		
    		adcTimeText.setText(adcTimeToString(aa + (bb*256)));
    		
    		
    	}
    
    }
    
    private void updateADC0(byte value, byte value2){
    	tvadc0.setText("0x"+bytesToHex(value)+", 0x"+bytesToHex(value2));
    }
    private void updateADC1(byte value, byte value2){
    	tvadc1.setText("0x"+bytesToHex(value)+", 0x"+bytesToHex(value2));
    }
    
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
    
    public static String bytesToHex(byte bytedata) {
        char[] hexChars = new char[2];

        int v = bytedata & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];

        return new String(hexChars);
    }
    
    
    private void updateReadPort(byte value){
    	
    	
		if((portStatus & 0x80) == 0x80){
		}else {
			if((value & 0x80) == 0x80){
				iv88.setImageResource(R.drawable.one);
			} else{
				iv88.setImageResource(R.drawable.zero);
			}
		}
			

		if((portStatus & 0x40) == 0x40){
		}else {
			if((value & 0x40) == 0x40){
				iv77.setImageResource(R.drawable.one);
			} else{
				iv77.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x20) == 0x20){
		}else {
			if((value & 0x20) == 0x20){
				iv66.setImageResource(R.drawable.one);
			} else{
				iv66.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x10) == 0x10){
		}else {
			if((value & 0x10) == 0x10){
				iv55.setImageResource(R.drawable.one);
			} else{
				iv55.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x08) == 0x08){
		}else {
			if((value & 0x08) == 0x08){
				iv44.setImageResource(R.drawable.one);
			} else{
				iv44.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x04) == 0x04){
		}else {
			if((value & 0x04) == 0x04){
				iv33.setImageResource(R.drawable.one);
			} else{
				iv33.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x02) == 0x02){
		}else {
			if((value & 0x02) == 0x02){
				iv22.setImageResource(R.drawable.one);
			} else{
				iv22.setImageResource(R.drawable.zero);
			}
		}
		
		if((portStatus & 0x01) == 0x01){
		}else {
			if((value & 0x01) == 0x01){
				iv11.setImageResource(R.drawable.one);
			} else{
				iv11.setImageResource(R.drawable.zero);
			}
		}
    	
    	
    }
    
    
    

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        Log.d(TAG,"TEST");
                        Log.d(TAG, "Selected uuid:" + characteristic.getUuid().toString());
                        
                        //Log.d("BLE", "UUID selected: ".append(characteristic.))
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        ///mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        
        //ActionBar actionBar = getActionBar();
        //actionBar.setBackgroundDrawable(new ColorDrawable(0xFF0000FF));
        //int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        //if (actionBarTitleId > 0) {
        //    TextView title = (TextView) findViewById(actionBarTitleId);
        //    if (title != null) {
        //        title.setTextColor(Color.WHITE);
        //    }
        //}
       

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        
        if(mDeviceName.equals("FBL770 v2.0.0"))
        {
        	mDeviceName = "     BLE_GPIO";
        }
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        
        
        
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        //mConnectionState.setTextColor(Color.WHITE);
        mDataField = (TextView) findViewById(R.id.data_value);

      
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

          Log.d(TAG,"Start ======================");
          
		  iv1 = (ImageView) findViewById(R.id.imageView1); 
		  iv2 = (ImageView) findViewById(R.id.imageView2); 
		  iv3 = (ImageView) findViewById(R.id.imageView3); 
		  iv4 = (ImageView) findViewById(R.id.imageView4); 
		  iv5 = (ImageView) findViewById(R.id.imageView5); 
		  iv6 = (ImageView) findViewById(R.id.imageView6); 
		  iv7 = (ImageView) findViewById(R.id.imageView7); 
		  iv8 = (ImageView) findViewById(R.id.imageView8); 
		  
		  iv1.setVisibility(View.INVISIBLE);
		  iv2.setVisibility(View.INVISIBLE);
		  iv3.setVisibility(View.INVISIBLE);
		  iv4.setVisibility(View.INVISIBLE);
		  iv5.setVisibility(View.INVISIBLE);
		  iv6.setVisibility(View.INVISIBLE);
		  iv7.setVisibility(View.INVISIBLE);
		  iv8.setVisibility(View.INVISIBLE);
		
		  iv11 = (ImageView) findViewById(R.id.imageView01); 
		  iv22 = (ImageView) findViewById(R.id.imageView02); 
		  iv33 = (ImageView) findViewById(R.id.imageView03); 
		  iv44 = (ImageView) findViewById(R.id.imageView04); 
		  iv55 = (ImageView) findViewById(R.id.imageView05); 
		  iv66 = (ImageView) findViewById(R.id.imageView06); 
		  iv77 = (ImageView) findViewById(R.id.imageView07); 
		  iv88 = (ImageView) findViewById(R.id.imageView08);   

		  iv11.setVisibility(View.INVISIBLE);
		  iv22.setVisibility(View.INVISIBLE);
		  iv33.setVisibility(View.INVISIBLE);
		  iv44.setVisibility(View.INVISIBLE);
		  iv55.setVisibility(View.INVISIBLE);
		  iv66.setVisibility(View.INVISIBLE);
		  iv77.setVisibility(View.INVISIBLE);
		  iv88.setVisibility(View.INVISIBLE);
		  
		  
		  adcTimeText = (TextView) findViewById(R.id.adcTimeText);
		  
		  tvadc0 = (TextView) findViewById(R.id.tvadc0);
		  tvadc1 = (TextView) findViewById(R.id.tvadc1);
		  
		  
		  seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
		  pwm0Value = (TextView) findViewById(R.id.pwm0Value);
		  seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
		  pwm1Value = (TextView) findViewById(R.id.pwm1Value);
		  
    		seekBar1.setVisibility(View.INVISIBLE);
	  		seekBar2.setVisibility(View.INVISIBLE);

		  
	        iv1.setOnTouchListener(onBtnTouchListener);
	        iv2.setOnTouchListener(onBtnTouchListener);        
	        iv3.setOnTouchListener(onBtnTouchListener);        
	        iv4.setOnTouchListener(onBtnTouchListener);        
	        iv5.setOnTouchListener(onBtnTouchListener);        
	        iv6.setOnTouchListener(onBtnTouchListener);        
	        iv7.setOnTouchListener(onBtnTouchListener);        
	        iv8.setOnTouchListener(onBtnTouchListener);
	        tvadc0.setOnTouchListener(onBtnTouchListener);
	        tvadc1.setOnTouchListener(onBtnTouchListener);		
	        
	        
	        
		  seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
				
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				
				pwm0Value.setText(" "+progress);
				
		    	final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(4).get(2);
		    	
		    	mBluetoothLeService.writeCharacteristic(characteristic, (byte)progress);

		    	
		    	if(progress == 0)
		    	{
				
			    	Handler mHandlerPwm0 = new Handler();
			    	mHandlerPwm0.postDelayed(new Runnable() {
	
						@Override
						public void run() {
							mBluetoothLeService.writeCharacteristic(characteristic, (byte)0);
						}
			    	   
			    	}, 200);
		    	}

	    		updateCommandState("Send PWM0");
	    		
	    		displayData(bytesToHex((byte)progress));
	    		
	    		
	    		Log.d(TAG, "************************PWM0 :" + progress);

				
			}
		});
		  
		  seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				
				
				pwm1Value.setText(" "+progress);
		    	final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(4).get(3);
		    	
				mBluetoothLeService.writeCharacteristic(characteristic, (byte)progress);

		    	if(progress == 0)
		    	{
				
			    	Handler mHandlerPwm1 = new Handler();
			    	mHandlerPwm1.postDelayed(new Runnable() {
	
						@Override
						public void run() {
							mBluetoothLeService.writeCharacteristic(characteristic, (byte)0);
						}
			    	   
			    	}, 200);
		    	}
		    	
	    		
	    		Log.d(TAG, "*********************PWM1 :" + progress);
	    		
	    		
	    		updateCommandState("Send PWM1");
	    		
	    		displayData(bytesToHex((byte)progress));

				
			}
		});
		  
          
          
          
          
    }
    

    
    

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            
            
            
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //mConnectionState.setText(resourceId);
//            }
//        });
    }

    private void updateCommandState(final String str) {
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              mConnectionState.setText(str);
          }
      });
  }

    
    
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            
            Log.d(TAG,"service uuid : " + uuid);
            
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG,"gattCharacteristic uuid : " + uuid);
                
                
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

       /* SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter); */
        
        
        Log.d(TAG,"service read ok ");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    
    private OnTouchListener onBtnTouchListener = new OnTouchListener(){
 	   
        public boolean onTouch(View v, MotionEvent $e)
        {

     	   
    	   
            switch ($e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                	switch(v.getId()){
	            	case R.id.tvadc0:
	            	case R.id.tvadc1:
//	            		if(adcDisplayMode == 0){
//	            			adcDisplayMode = 1;
//	            		} else {
//	            			adcDisplayMode = 0;
//	            		}
//	            		
//	        			if(adcDisplayMode == 0){
//	            			adc0.setText("0x"+bytesToHex(adc0Value)+" Hex");
//	            			adc1.setText("0x"+bytesToHex(adc1Value)+" Hex");
//	        			} else {
//	            			adc0.setText(adc0Value+" Dec");
//	            			adc1.setText(adc1Value+" Dec");
//	        			}
	            	break;
                	case R.id.imageView1:
                		
                		if((portStatus & 0x01) == 0x01){
                		
                        if((ledStatus & 0x01) == 0x01){
                        	iv1.setImageResource(R.drawable.zero);
                        	ledStatus = (byte) (ledStatus & (~0x01));                        	
                        } else {
                        	iv1.setImageResource(R.drawable.one); 
                        	ledStatus = (byte) (ledStatus | 0x01);
                        }
                		}
                        break;
                	case R.id.imageView2:
                		if((portStatus & 0x02) == 0x02){
                    		
                        if((ledStatus & 0x02) == 0x02){
                        	iv2.setImageResource(R.drawable.zero);
                        	ledStatus = (byte) (ledStatus & (~0x02));                        	
                        } else {
                        	iv2.setImageResource(R.drawable.one); 
                        	ledStatus = (byte) (ledStatus | 0x02);
                        }
                		}
                        break;
                	case R.id.imageView3:
                		if((portStatus & 0x04) == 0x04){
                    		
                		
		                if((ledStatus & 0x04) == 0x04){
		                	iv3.setImageResource(R.drawable.zero);
		                	ledStatus = (byte) (ledStatus & (~0x04));                        	
		                } else {
		                	iv3.setImageResource(R.drawable.one); 
		                	ledStatus = (byte) (ledStatus | 0x04);
		                }
                		}
		                break;
                	case R.id.imageView4:
                		if((portStatus & 0x08) == 0x08){
                    		
                		
		                if((ledStatus & 0x08) == 0x08){
		                	iv4.setImageResource(R.drawable.zero);
		                	ledStatus = (byte) (ledStatus & (~0x08));                        	
		                } else {
		                	iv4.setImageResource(R.drawable.one); 
		                	ledStatus = (byte) (ledStatus | 0x08);
		                }
                		}
		                break;
                	case R.id.imageView5:
                		if((portStatus & 0x10) == 0x10){
                    		
                		
	                    if((ledStatus & 0x10) == 0x10){
	                    	iv5.setImageResource(R.drawable.zero);
	                    	ledStatus = (byte) (ledStatus & (~0x10));                        	
	                    } else {
	                    	iv5.setImageResource(R.drawable.one); 
	                    	ledStatus = (byte) (ledStatus | 0x10);
	                    }
                		}
	                    break;
                	case R.id.imageView6:
                		if((portStatus & 0x20) == 0x20){
                    		
                		
	                    if((ledStatus & 0x20) == 0x20){
	                    	iv6.setImageResource(R.drawable.zero);
	                    	ledStatus = (byte) (ledStatus & (~0x20));                        	
	                    } else {
	                    	iv6.setImageResource(R.drawable.one); 
	                    	ledStatus = (byte) (ledStatus | 0x20);
	                    }
                		}
	                    break;
                	case R.id.imageView7:
                		if((portStatus & 0x40) == 0x40){
                    		
                		
		                if((ledStatus & 0x40) == 0x40){
		                	iv7.setImageResource(R.drawable.zero);
		                	ledStatus = (byte) (ledStatus & (~0x40));                        	
                	    } else {
		                	iv7.setImageResource(R.drawable.one); 
		                	ledStatus = (byte) (ledStatus | 0x40);
		                }
                		}
		                break;
                	case R.id.imageView8:
                		if((portStatus & 0x80) == 0x80){
                    		
                		
		                if((ledStatus & 0x80) == 0x80){
		                	iv8.setImageResource(R.drawable.zero);
		                	ledStatus = (byte) (ledStatus & (~0x80));                        	
		                } else {
		                	iv8.setImageResource(R.drawable.one); 
		                	ledStatus = (byte) (ledStatus | 0x80);
		                }
                		}
		                break;
                	}
              	
                	sendLedStatus();                	
                    break;
                 
                case MotionEvent.ACTION_UP:
             	

                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
    
           		                	
                	break;
                 
                default:
                    break;
            }
            return false;
        }
    }; 
    
   
    
    private void sendLedStatus(){
    	if(mGattCharacteristics != null){ 
    	final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(4).get(1);
    		mBluetoothLeService.writeCharacteristic(characteristic, ledStatus);
    		
    		updateCommandState("Send PIO");
    		
    		displayData(bytesToHex(ledStatus));

    	}    	
    }
    
    
    
    
    
    
}
