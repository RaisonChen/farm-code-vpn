package cn.ys1231.appproxy

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.ys1231.appproxy.IyueService.IyueVPNService
import cn.ys1231.appproxy.IyueService.VpnServiceController
import cn.ys1231.appproxy.data.Utils
import cn.ys1231.appproxy.mcpserver.MCPForegroundService
import cn.ys1231.appproxy.mcpserver.MCPServer
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val TAG = "iyue-${this.javaClass.simpleName}"
    private val CHANNEL = "cn.ys1231/appproxy"
    private val CHANNEL_VPN = "cn.ys1231/appproxy/vpn"
    private val CHANNEL_APP_UPDATE = "cn.ys1231/appproxy/appupdate"
    private val CHANNEL_MCP_SERVER = "cn.ys1231/appproxy/mcpserver"
    private var FLUTTER_VPN_CHANNEL: MethodChannel? = null
    private var FLUTTER_CHANNEL: MethodChannel? = null
    private var FLUTTER_MCP_SERVER: MethodChannel? = null
    private var utils: Utils? = null
    private var intentVpnService: Intent? = null
    private var iyueVpnService: IyueVPNService? = null
    private var isBind: Boolean = false
    var currentProxy: Map<*, *>? = null
    private var conn: ServiceConnection? = null
    private var vpnController: VpnServiceController? = null
    private var mcpServiceBinder: MCPForegroundService.MCPServiceBinder? = null
    private var mcpConn: ServiceConnection? = null
    private var isMcpBind: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 修复：utils 必须先初始化，再传给 VpnServiceController
        utils = Utils(this)

        intentVpnService = Intent(this, IyueVPNService::class.java)
        vpnController = VpnServiceController(this, iyueVpnService, utils!!)
        conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(TAG, "onServiceConnected: $name")
                if (service is IyueVPNService.VPNServiceBinder) {
                    iyueVpnService = service.getService()
                    vpnController?.updateVpnService(iyueVpnService)
                    MCPServer.getInstance(applicationContext).setVpnController(vpnController)
                    Log.d(TAG, "onServiceConnected: ${iyueVpnService.toString()}")
                } else {
                    Log.d(TAG, "onServiceConnected: ClassCastException")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "onServiceDisconnected: $name")
            }
        }
        if (bindService(intentVpnService!!, conn!!, Context.BIND_AUTO_CREATE)) {
            isBind = true
        }

        // 启动并绑定 MCPForegroundService（已禁用通知）
        val mcpServiceIntent = Intent(this, MCPForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mcpServiceIntent)
        } else {
            startService(mcpServiceIntent)
        }
        mcpConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(TAG, "MCPForegroundService onServiceConnected")
                mcpServiceBinder = service as? MCPForegroundService.MCPServiceBinder
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "MCPForegroundService onServiceDisconnected")
                mcpServiceBinder = null
            }
        }
        if (bindService(mcpServiceIntent, mcpConn!!, Context.BIND_AUTO_CREATE)) {
            isMcpBind = true
        }
        checkVpnPermission()
    }

    private fun startVpnService() {
        Log.d(TAG, "startVpnService: ${currentProxy.toString()}")
        iyueVpnService?.startVpnService(currentProxy!!)
        vpnController?.setVpnConfig(currentProxy!!)

        // 检测VPN服务是否停止 通知 Flutter 更新 ui
        Thread {
            Log.d(TAG, "check iyueVpnService isRunning: " + iyueVpnService?.isRunning())
            while (true) {

                if (iyueVpnService?.isRunning() == true) {
                    Thread.sleep(1000)
                } else {
                    runOnUiThread {
                        FLUTTER_VPN_CHANNEL!!.invokeMethod("stopVpn", null)
                    }
                    break
                }
            }
        }.start()
    }

    private fun stopVpnService() {
        Log.d(TAG, "stopVpnService: ...... ")
        iyueVpnService?.stopVpnService()
    }

    private fun startDownload(url: String?) {
        val downloadIntent = Intent(Intent.ACTION_VIEW)
        downloadIntent.data = Uri.parse(url)
        downloadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(downloadIntent)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        utils = Utils(this)

        FLUTTER_CHANNEL = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        )
        FLUTTER_CHANNEL!!.setMethodCallHandler { call, result ->
            if (call.method == "getAppList") {
                try {
                    Log.d(TAG, "configureFlutterEngine ${call.method} ")
                    val appList = utils!!.getAppList()
                    result.success(appList)
                } catch (e: Exception) {
                    result.error("-1", e.message, null)
                }
            }
        }

        FLUTTER_VPN_CHANNEL = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_VPN
        )
        FLUTTER_VPN_CHANNEL!!.setMethodCallHandler { call, result ->
            when (call.method) {
                "startVpn" -> {
                    try {
                        // 修复：安全类型转换
                        currentProxy = call.arguments() as? Map<*, *>
                        checkVpnPermission()
                        startVpnService()
                        result.success(iyueVpnService?.isRunning())
                    } catch (e: Exception) {
                        result.error("-1", e.message, null)
                    }
                }
                "stopVpn" -> {
                    try {
                        stopVpnService()
                        result.success(!iyueVpnService?.isRunning()!!)
                    } catch (e: Exception) {
                        result.error("-1", e.message, null)
                    }
                }
            }
        }

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_APP_UPDATE
        ).setMethodCallHandler { call, result ->
            if (call.method == "startDownload") {
                try {
                    Log.d(TAG, "configureFlutterEngine ${call.method} ")
                    val url: String? = call.arguments()
                    startDownload(url)
                } catch (e: Exception) {
                    result.error("-1", e.message, null)
                }
            }
        }

        FLUTTER_MCP_SERVER = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_MCP_SERVER
        )
        FLUTTER_MCP_SERVER!!.setMethodCallHandler { call, result ->
            when (call.method) {
                "startMcpServer" -> {
                    try {
                        mcpServiceBinder?.startMcpServer()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("-1", e.message, null)
                    }
                }
                "stopMcpServer" -> {
                    try {
                        mcpServiceBinder?.stopMcpServer()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("-1", e.message, null)
                    }
                }
                "updateMcpServerConfig" -> {
                    try {
                        val arguments = call.arguments as? List<*>
                        if (arguments != null && arguments.size >= 2) {
                            val port: Int = arguments[0] as? Int ?: 0
                            val auth: String = arguments[1] as? String ?: ""
                            val binder = mcpServiceBinder
                            if (binder != null) {
                                binder.updateMcpPort(port)
                                binder.updateMcpAuth(auth)
                                result.success(true)
                            } else {
                                result.success(false)
                            }
                        } else {
                            result.error("-1", "Invalid arguments", null)
                        }
                    } catch (e: Exception) {
                        result.error("-1", e.message, null)
                    }
                }
            }
        }

        // 遍历所有 app 通知刷新
        Thread {
            Log.d(TAG, "configureFlutterEngine: start get app list info")
            utils!!.initAppList()
            runOnUiThread {
                Log.d(TAG, "configureFlutterEngine: call onRefresh")
                FLUTTER_CHANNEL!!.invokeMethod("onRefresh", null)
                Log.d(TAG, "configureFlutterEngine: end get app list info")
            }
        }.start()
    }

    private val VPN_REQUEST_CODE = 100
    private val REQUEST_NOTIFICATION_PERMISSION = 1231

    private fun checkVpnPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                Log.d(TAG, "onCreate: 通知权限已授予!")
            }
        }
        // 准备建立 VPN 连接 检测用户是否同意
        val intent = VpnService.prepare(this)
        if (intent != null) {
            this.startActivityForResult(intent, VPN_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult: 用户授权成功")
            } else {
                Log.d(TAG, "onActivityResult: 用户拒绝授权 ")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知权限被授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "此应用程序需要通知权限", Toast.LENGTH_SHORT).show()
                startNotificationSetting()
            }
        }
    }

    private fun startNotificationSetting() {
        val applicationInfo = applicationInfo
        try {
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", applicationInfo.packageName)
            intent.putExtra("android.provider.extra.APP_PACKAGE", applicationInfo.packageName)
            intent.putExtra("app_uid", applicationInfo.uid)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            intent.data = Uri.fromParts("package", applicationInfo.packageName, null)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBind) {
            unbindService(conn!!)
            isBind = false
        }
        if (isMcpBind) {
            unbindService(mcpConn!!)
            isMcpBind = false
        }
    }
}
