package com.mangaguide.manganavi.data

import android.content.Intent
import android.net.Uri
import com.mangaguide.manganavi.AppContext

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AppContext.context?.startActivity(intent)
}
