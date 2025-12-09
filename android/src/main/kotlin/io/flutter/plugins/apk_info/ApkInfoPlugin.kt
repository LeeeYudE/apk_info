package io.flutter.plugins.apk_info

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream

class ApkInfoPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: android.content.Context

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "io.flutter.plugins.apk_info")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) { "getApkInfo" -> {
                val path = call.arguments<String>()
                val apkInfo = getApkInfo(path)
                result.success(apkInfo)
            }
            else -> result.notImplemented()
        }
    }

    private fun getApkInfo(path: String?): Map<String, Any?> {
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo? = pm.getPackageArchiveInfo(path!!, PackageManager.GET_META_DATA)
            if (packageInfo != null) {
              val info = packageInfo.applicationInfo!!
              info.sourceDir = path
              info.publicSourceDir = path

              // Get icon and convert to base64
              val icon = pm.getApplicationIcon(info)
              val iconBase64 = drawableToBase64(icon)

              mapOf(
                  "uuid" to path!!.hashCode().toString(),
                  "applicationId" to packageInfo.packageName,
                  "applicationLabel" to pm.getApplicationLabel(info).toString(),
                  "versionCode" to packageInfo.versionCode.toString(),
                  "versionName" to packageInfo.versionName,
                  "platformBuildVersionCode" to info.targetSdkVersion.toString(),
                  "compileSdkVersion" to info.targetSdkVersion.toString(),
                  "minSdkVersion" to info.minSdkVersion.toString(),
                  "targetSdkVersion" to info.targetSdkVersion.toString(),
                  "icon" to iconBase64
              )
            } else {
              mapOf("error" to "Failed to read APK info")
            }
        } catch (e: Exception) {
          mapOf("error" to e.toString())
        }
    }

    private fun drawableToBase64(drawable: Drawable): String {
        val bitmap = drawableToBitmap(drawable)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}