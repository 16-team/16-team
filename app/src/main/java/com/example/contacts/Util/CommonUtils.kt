package com.example.contacts.Util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.contacts.ContactFragment

fun callPhoneNumber(activity : Activity, phoneNumber: String) {
    val callIntent = Intent(Intent.ACTION_CALL)
    callIntent.data = Uri.parse("tel:$phoneNumber")

    if (ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            ContactFragment.REQUEST_PHONE_CALL
        )
        return
    }
    activity.startActivity(callIntent)
}

fun messagePhoneNumber(activity: Activity, phoneNumber: String){
    val smsUri = Uri.parse("smsto:$phoneNumber") // 문자를 전송할 phoneNumber
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = smsUri
    intent.putExtra("sms_body", "") // body에 전송할 내용입니다!
    activity.startActivity(intent)
}


