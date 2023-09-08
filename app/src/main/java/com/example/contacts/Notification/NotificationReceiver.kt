package com.example.contacts.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {//onreceive는 브로드캐스트의 메서드로 이벤트를 수신하면 자동으로호출
        if (intent?.action == "com.example.contacts.NOTIFICATION_ACTION") {
            }
        }
    }
//즉 특정알림 액션을 수신하면 해당알림을 표시하는 역할