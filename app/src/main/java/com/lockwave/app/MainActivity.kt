package com.lockwave.app

import android.Manifest
import android.app.*
import android.os.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity: AppCompatActivity() {
 private val serviceId=UUID.fromString("9E7A0001-8F37-4F4D-B45E-1A87B7D62A11")
 private val charId=UUID.fromString("9E7A0002-8F37-4F4D-B45E-1A87B7D62A11")
 private lateinit var status:TextView; private lateinit var seek:SeekBar
 private var gatt:BluetoothGatt?=null; private var control:BluetoothGattCharacteristic?=null
 private val adapter by lazy { (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter }
 override fun onCreate(b:Bundle?){super.onCreate(b); buildUi(); requestBle()}
 private fun requestBle(){
  val p=if(Build.VERSION.SDK_INT>=31) arrayOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
  if(p.any{ActivityCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED}) ActivityCompat.requestPermissions(this,p,44) else scan()
 }
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==44&&g.all{it==PackageManager.PERMISSION_GRANTED})scan()else status.text="Bluetooth permission required"}
 private fun scan(){
  status.text="Searching for LockWave…"
  val scanner=adapter.bluetoothLeScanner ?: run{status.text="Turn Bluetooth on";return}
  val filter=ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(serviceId)).build()
  scanner.startScan(listOf(filter),ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),cb)
  Handler(mainLooper).postDelayed({scanner.stopScan(cb);if(gatt==null)status.text="LockWave not found • Tap RESCAN"},8000)
 }
 private val cb=object:ScanCallback(){
  override fun onScanResult(t:Int,r:ScanResult){adapter.bluetoothLeScanner?.stopScan(this);status.text="Connecting…";gatt=r.device.connectGatt(this@MainActivity,false,gcb,BluetoothDevice.TRANSPORT_LE)}
 }
 private val gcb=object:BluetoothGattCallback(){
  override fun onConnectionStateChange(g:BluetoothGatt,s:Int,n:Int){runOnUiThread{status.text=if(n==BluetoothProfile.STATE_CONNECTED)"Connected • discovering…" else "Disconnected"};if(n==BluetoothProfile.STATE_CONNECTED)g.discoverServices()}
  override fun onServicesDiscovered(g:BluetoothGatt,s:Int){control=g.getService(serviceId)?.getCharacteristic(charId);control?.let{g.setCharacteristicNotification(it,true);g.readCharacteristic(it)};runOnUiThread{status.text=if(control!=null)"● Connected to LockWave" else "LockWave service not found";seek.isEnabled=control!=null}}
  override fun onCharacteristicRead(g:BluetoothGatt,c:BluetoothGattCharacteristic,s:Int){update(c)}
  override fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic){update(c)}
  private fun update(c:BluetoothGattCharacteristic){val v=c.value?.firstOrNull()?.toInt()?.and(255)?:return;runOnUiThread{seek.progress=v}}
 }
 private fun send(v:Int){val c=control?:return;c.value=byteArrayOf(v.coerceIn(0,180).toByte());gatt?.writeCharacteristic(c)}
 private fun buildUi(){
  val bg=Color.rgb(27,30,33); val cyan=Color.rgb(40,216,255)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(42,70,42,50);setBackgroundColor(bg)}
  val title=TextView(this).apply{text="LOCKWAVE";textSize=13f;setTextColor(cyan);letterSpacing=.22f;gravity=Gravity.CENTER}
  val lock=TextView(this).apply{text="◯";textSize=150f;setTextColor(cyan);gravity=Gravity.CENTER;setShadowLayer(28f,0f,0f,cyan)}
  status=TextView(this).apply{text="Starting…";textSize=19f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,20,0,35)}
  val angle=TextView(this).apply{text="0°";textSize=46f;setTextColor(Color.WHITE);gravity=Gravity.CENTER}
  seek=SeekBar(this).apply{max=180;progress=0;isEnabled=false;progressTintList=android.content.res.ColorStateList.valueOf(cyan);thumbTintList=android.content.res.ColorStateList.valueOf(cyan)}
  seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar,v:Int,u:Boolean){angle.text="$v°";if(u)send(v)};override fun onStartTrackingTouch(s:SeekBar){};override fun onStopTrackingTouch(s:SeekBar){}})
  val btn=Button(this).apply{text="RESCAN";setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(45,49,54));setOnClickListener{gatt?.close();gatt=null;control=null;seek.isEnabled=false;scan()}}
  root.addView(title,LinearLayout.LayoutParams(-1,-2));root.addView(lock,LinearLayout.LayoutParams(-1,0,1f));root.addView(status,LinearLayout.LayoutParams(-1,-2));root.addView(angle);root.addView(seek,LinearLayout.LayoutParams(-1,-2));root.addView(btn,LinearLayout.LayoutParams(-1,-2))
  setContentView(root)
 }
 override fun onDestroy(){super.onDestroy();gatt?.close()}
}