package com.example.squareboat.ui.dashboard

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squareboat.App
import com.example.squareboat.R
import com.example.squareboat.model.icon.IconResponse
import com.example.squareboat.model.icon.IconsItem
import com.example.squareboat.network.DataWrapper
import com.example.squareboat.network.IconRepo
import com.example.squareboat.utils.CallBack
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

class DashBoardViewModel @Inject constructor(private var repo: IconRepo) : ViewModel() {

    private val iconObserver = MutableLiveData<DataWrapper<List<IconsItem>>>()
    private val canRequestMore = MutableLiveData<Boolean>()
    private val toastObserver = MutableLiveData<String>()
    private var offset = 0

    fun getIconsForFirstTime() {
        canRequestMore.value = false
        repo.getIcons(object : CallBack<IconResponse> {
            override fun onSuccess(response: IconResponse) {
                response.icons?.let {
                    if (response.totalCount <= 10) {
                        canRequestMore.value = false
                    } else {
                        canRequestMore.value = true
                        offset = it[it.size - 1].iconId
                    }
                    iconObserver.value = DataWrapper(it, null)
                }
            }

            override fun onError(message: String) {
                toastObserver.value = message
            }
        })
    }

    fun getMoreIcons() {
        repo.getMoreIcons(object : CallBack<IconResponse> {
            override fun onSuccess(response: IconResponse) {
                response.icons?.let {
                    if (response.totalCount <= 10) {
                        canRequestMore.value = false
                    } else {
                        canRequestMore.value = true
                        offset = it[it.size - 1].iconId
                    }
                    iconObserver.value = DataWrapper(it, null)
                }
            }

            override fun onError(message: String) {
                toastObserver.value = message
            }
        }, offset)
    }

    fun downloadImage(url: String) {
        toastObserver.value = App.instance.getString(R.string.downloading_image)
        Picasso.get().load(url).into(object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap ?: return


                val root = Environment.getExternalStorageDirectory().toString()
                val dir = File("$root/squareboat")
                if (!dir.exists()) dir.mkdir()
                val file = File(dir, "squareboat_${Date().time}.png")
                try {
                    val stream: OutputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                    stream.close()
                    toastObserver.value = App.instance.getString(R.string.download_success)

                } catch (e: IOException) {
                    e.printStackTrace()
                    toastObserver.value = App.instance.getString(R.string.download_failed)

                } finally {

                    MediaScannerConnection.scanFile(
                        App.instance,
                        arrayOf(file.path),
                        arrayOf("image/png"),
                        null
                    )

                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()

        repo.clear()
    }

    fun getIconObserver(): LiveData<DataWrapper<List<IconsItem>>> = iconObserver
    fun canRequestObserver(): LiveData<Boolean> = canRequestMore
    fun toastOberver(): LiveData<String> = toastObserver
}