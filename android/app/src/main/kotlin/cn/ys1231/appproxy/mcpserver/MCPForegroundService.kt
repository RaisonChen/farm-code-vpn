package cn.ys1231.appproxy.mcpserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import cn.ys1231.appproxy.MainActivity

/**
 * MCP Server 前台服务（已禁用通知）
 *
 * 不调用 startForeground，不弹任何状态栏通知。
 * 这样就不会在通知栏显示 "appproxy-mcp / MCP Server is not running"。
 */
class MCPForegroundService : Service() {

    private val TAG = "iyue->${this.javaClass.simpleName}"

    inner class MCPServiceBinder : Binder() {
        fun startMcpServer() = this@MCPForegroundService.startMcpServer()
        fun stopMcpServer() = this@MCPForegroundService.stopMcpServer()
        fun updateMcpPort(port: Int?) = this@MCPForegroundService.updateMcpPort(port)
        fun updateMcpAuth(auth: String?) = this@MCPForegroundService.updateMcpAuth(auth)
    }

    private val binder = MCPServiceBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: MCPForegroundService (no notification)")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: MCPForegroundService")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: MCPForegroundService")
        // 不调用 startForeground，不弹通知
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: app exit, Stop MCPForegroundService")
        stopMcpServer()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: MCPForegroundService")
        stopMcpServer()
    }

    private fun startMcpServer() {
        Log.d(TAG, "startMcpServer: Netty server started")
        MCPServer.getInstance(applicationContext).startMcpServer()
    }

    private fun stopMcpServer() {
        MCPServer.getInstance(applicationContext).stopMcpServer()
        Log.d(TAG, "stopMcpServer: Netty server stopped")
    }

    private fun updateMcpPort(port: Int?) {
        Log.d(TAG, "updateMcpPort: $port")
        MCPServer.getInstance(applicationContext).updateMcpPort(port)
    }

    private fun updateMcpAuth(auth: String?) {
        Log.d(TAG, "updateMcpAuth: $auth")
        MCPServer.getInstance(applicationContext).updateMcpAuth(auth)
    }
}
