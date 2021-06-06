/*
 * Copyright (C) 2016-2017, Collabora Ltd.
 *   Author: Justin Kim <justin.kim@collabora.com>
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
package org.freedesktop.nativecamera2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.freedesktop.nativecamera2.databinding.MainBinding
import org.freedesktop.nativecamera2.databinding.ExtraviewlayoutBinding

/** Helper to ask camera permission.  */
object CameraPermissionHelper {
    private const val PERMISSION_REQUEST_CAMERA = 0
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(CAMERA_PERMISSION), PERMISSION_REQUEST_CAMERA)
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
}

class NativeCamera2 : Activity() {

    external fun startPreview(surface: Surface?)
    external fun stopPreview()
    external fun startExtraView(surface: Surface?)
    external fun stopExtraView()

    private var isBurstModeOn = false

    companion object {
        const val TAG = "NativeCamera2"

        init {
            System.loadLibrary("native-camera2-jni")
        }
    }

    private lateinit var mainBinding: MainBinding
    private lateinit var extraViewBinding: ExtraviewlayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = MainBinding.inflate(layoutInflater)
        extraViewBinding = ExtraviewlayoutBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        val surfaceHolder = mainBinding.surfaceview.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.v(TAG, "surface created.")
                startPreview(holder.surface)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopPreview()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                Log.v(TAG, "format=$format w/h : ($width, $height)")
            }
        })

        val layoutParamsControl = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        this.addContentView(extraViewBinding.root, layoutParamsControl)

        extraViewBinding.extraview.visibility = View.INVISIBLE
        val extraViewHolder = extraViewBinding.extraview.holder
        extraViewHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startExtraView(extraViewHolder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopExtraView()
            }
        })

        mainBinding.surfaceview.setOnClickListener {
            isBurstModeOn = !isBurstModeOn
            if (isBurstModeOn) {
                extraViewBinding.extraview.visibility = View.VISIBLE
            } else {
                extraViewBinding.extraview.visibility = View.INVISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }

        recreate()
    }

    override fun onDestroy() {
        stopPreview()
        super.onDestroy()
    }


}
