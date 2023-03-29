package io.flutter.plugins.nfc_host_card_emulation

import android.util.Log

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.FlutterPlugin

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import android.content.BroadcastReceiver

import io.flutter.plugins.nfc_host_card_emulation.AndroidHceService

/** NfcHostCardEmulationPlugin */
class NfcHostCardEmulationPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var channel : MethodChannel

  // base methods
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "nfc_host_card_emulation")
    channel.setMethodCallHandler(this)

    context = flutterPluginBinding.applicationContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    activity.registerReceiver(apduServiceReciever, IntentFilter("apduCommand"))
  }

  override fun onDetachedFromActivity() {
    activity.unregisterReceiver(apduServiceReciever);
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    activity.registerReceiver(apduServiceReciever, IntentFilter("apduCommand"))
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity.unregisterReceiver(apduServiceReciever);
  }
  
  // nfc host card emulation methods
  private val apduServiceReciever = object : BroadcastReceiver() {
    override fun onReceive(contxt: Context?, intent: Intent?) {   
        when (intent?.action) {
          "apduCommand" -> channel.invokeMethod("apduCommand", mapOf(
            "port" to intent!!.getIntExtra("port", -1),
            "command" to intent!!.getByteArrayExtra("command"),
            "data" to intent!!.getByteArrayExtra("data"))
          )
        }
    }
  }
  
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "init" -> init(call, result)
      "addApduResponse" -> addApduResponse(call, result)
      "removeApduResponse" -> removeApduResponse(call, result)
      else -> result.notImplemented()
    }
  }

  private fun init(call: MethodCall, result: Result) {
    try {
      AndroidHceService.permanentApduResponses = 
        call.argument<Boolean>("permanentApduResponses")!!;
      AndroidHceService.listenOnlyConfiguredPorts = 
        call.argument<Boolean>("listenOnlyConfiguredPorts")!!;

      val aid = call.argument<ByteArray>("aid");
      if(aid != null) AndroidHceService.aid = aid;

      val cla = call.argument<Int>("cla")?.toByte();
      if(cla != null) AndroidHceService.cla = cla;

      val ins = call.argument<Int>("ins")?.toByte();
      if(ins != null) AndroidHceService.ins = ins;

      val AID = AndroidHceService.byteArrayToString(AndroidHceService.aid)
      Log.d("HCE", "HCE initialized. AID = $AID. CLA = ${AndroidHceService.cla.toUByte().toString(16)}. INS = ${AndroidHceService.ins.toUByte().toString(16)}")
    }
    catch(e : Exception) {
      result.error("invalid method parameters", "invalid parameters in 'init' method", null)
    }

    result.success(null)
  }


  private fun addApduResponse(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")!!
      val data = call.argument<ByteArray>("data")!!

      AndroidHceService.portData[port] = data

      val portData = AndroidHceService.byteArrayToString(AndroidHceService.portData[port]!!)
      Log.d("HCE", "Added $portData to port $port")
    }
    catch(e : Exception) {
      result.error("invalid method parameters", "invalid parameters in 'addApduResponse' method", null)
    }

    result.success(null)
  }

  private fun removeApduResponse(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")!!
      AndroidHceService.portData.remove(port)

      Log.d("HCE", "Removed APDU response from port $port")
    }
    catch(e : Exception) {
      result.error("invalid method parameters", "invalid parameters in 'removeApduResponse' method", null)
    }

    result.success(null)
  }
}
