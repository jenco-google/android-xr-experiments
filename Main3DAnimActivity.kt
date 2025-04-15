package com.example.xrexp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.concurrent.futures.await
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.*
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import com.example.xrexp.ui.theme.XRExpTheme
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.InteractableComponent
import java.util.concurrent.Executors


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.rotate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Main3DAnimActivity : ComponentActivity() {

    companion object {
        const val GLB_FILE_NAME = "models/shiba_inu_texture_updated.glb"
    }

    private lateinit var GLB : GltfModelEntity

    private var gltfModelEntity: GltfModelEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val session = LocalSession.current!!
            XRExpTheme {
                AnimatedSceneInFullSpace(session)
            }
        }

    }

    @Composable
    fun AnimatedSceneInFullSpace(xrSession: Session) {
        val root: Entity = xrSession.activitySpaceRoot
        val executor by lazy { Executors.newSingleThreadExecutor() }

        Subspace {
            LaunchedEffect(Unit) {
                // Model Loading
                val gltfModel = GltfModel.create(xrSession, GLB_FILE_NAME).await()
                val gltfEntity = GltfModelEntity.create(xrSession, gltfModel)

                // Global pty initialization
                GLB = gltfEntity

                // Transformations
                val translation = Vector3(0.5f, -2.7f, -2.4f)
                val orientation = Quaternion.fromEulerAngles(0f, -90f, 0f)
                val pose = Pose(translation, orientation)
                gltfEntity.setPose(pose)
                gltfEntity.setScale(1.5f)

                // Playing and stopping Animation
                gltfEntity.startAnimation(loop = false, animationName = "sitting_skeletal.3")
                gltfEntity.stopAnimation()

                // Setting an Interactable Component
                val interactable = InteractableComponent.create(xrSession, executor) { ie ->
                    when (ie.action) {
                        InputEvent.ACTION_HOVER_ENTER -> {
                            gltfEntity.setScale(2.7f)
                            println(gltfEntity.getScale())
                        }
                        InputEvent.ACTION_HOVER_EXIT -> {
                            gltfEntity.setScale(1.5f)
                            println(gltfEntity.getScale())
                        }
                    }
                }
                gltfEntity.addComponent(interactable)
                gltfEntity.setParent(root)
            }

            val tiltQuaternion: Quaternion = Quaternion.fromEulerAngles(-25f, 0f, 0f)

            loadModel(xrSession, "models/car_dodge_charger.glb")

            SpatialPanel(
                SubspaceModifier
                    .height(80.dp)
                    .width(320.dp)
                    .offset(x = (-10).dp, y = (-200).dp, z = (10).dp) // Adjust z as needed
                    .rotate(tiltQuaternion) // Apply the tilt rotation
                    //.resizable()
                    .movable()
            ) {
                //AnimationSwitch()

                /*Surface() {

                }*/
                MaterialImageCarousel() {
                    gltfModelEntity?.removeAllComponents()
                    gltfModelEntity?.dispose()
                    loadModel(xrSession, it.modelLocation)
                }
            }
        }
    }

    @Composable
    private fun ButtonPrototype(
        modifier: Modifier = Modifier,
        tint: Color,
        onClick: () -> Unit
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.padding(16.dp),
        ) {
            Icon(
                tint = tint,
                modifier = Modifier.scale(1.75f),
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null
            )
        }
    }

    @Composable
    private fun AnimationSwitch() {

        /*
        *  "Static Pose"
        *   "play_dead_skeletal.3"
        *   "rollover_skeletal.3"
        *   "shake_skeletal.3"
        *  "sitting_skeletal.3"
        *   "standing_skeletal.3"
         */

        Surface(
            Modifier.clip(CircleShape)
        ) {
            Row(
                Modifier.width(IntrinsicSize.Min)
            ) {
                ButtonPrototype(tint = Color.Magenta) {
                    GLB.startAnimation(loop = true, animationName = "standing_skeletal.3")
                }
                ButtonPrototype(tint = Color.Green) {
                    GLB.startAnimation(loop = true, animationName = "rollover_skeletal.3")
                }
                ButtonPrototype(tint = Color.Yellow) {
                    GLB.startAnimation(loop = true, animationName = "shake_skeletal.3")
                }
                ButtonPrototype(tint = Color.Cyan) {
                    GLB.startAnimation(loop = true, animationName = "play_dead_skeletal.3")
                }
            }
        }
    }

    fun loadModel(xrSession: Session, modelLocation: String) {

        val root: Entity = xrSession.activitySpaceRoot

        CoroutineScope(Dispatchers.Main)
            .launch {
                val gltfModel: GltfModel? =
                    //withContext(Dispatchers.IO) {
                    try {
                        GltfModel.create(
                            xrSession,
                            modelLocation
                        )
                            .await()
                    } catch (e: Exception) {
                        Log.e(
                            "ModelLoading",
                            "Error loading model: $modelLocation",
                            e
                        )
                        null
                    }
                //}
                if (gltfModel != null) {
                    //update the UI with the loaded model
                    gltfModelEntity = GltfModelEntity.create(xrSession, gltfModel)

                    // Transformations
                    //val translation = Vector3(0.5f, -2.7f, -2.4f)
                    val translation = Vector3(0.0f, 0.2f, 0.0f)
                    val orientation = Quaternion.fromEulerAngles(0f, -90f, 0f)
                    val pose = Pose(translation, orientation)
                    gltfModelEntity?.setPose(pose)
                    gltfModelEntity?.setScale(0.2f)
                    gltfModelEntity?.setParent(root)
                } else {
                    //handle the case when model was not loaded
                }
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MaterialImageCarousel(onItemClick: (CarouselItem) -> Unit) {

        val items =
            listOf(
                CarouselItem(0, R.drawable.ic_launcher_background, R.string.carousel_image_1_description, "models/car_dodge_charger.glb"),
                CarouselItem(1, R.drawable.ic_launcher_background, R.string.carousel_image_2_description, "models/car_jeep.glb"),
                CarouselItem(2, R.drawable.ic_launcher_background, R.string.carousel_image_3_description, "models/car_limbo.glb"),
                CarouselItem(3, R.drawable.ic_launcher_background, R.string.carousel_image_4_description, "models/car_mazda.glb"),
                CarouselItem(4, R.drawable.ic_launcher_background, R.string.carousel_image_5_description, "models/car_nissan.glb"),
                CarouselItem(5, R.drawable.ic_launcher_background, R.string.carousel_image_3_description, "models/car_truck.glb"),
            )

        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { items.count() },
            modifier = Modifier
                .width(420.dp)
                .height(221.dp),
            preferredItemWidth = 50.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val item = items[i]
            Image(
                modifier = Modifier
                    .height(205.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable {
                        // Handle click for this item here
                        println("Clicked on item at index: $i, id: ${item.id}")
                        // Add your navigation logic or other actions here
                        onItemClick(item)


                    },
                painter = painterResource(id = item.imageResId),
                contentDescription = stringResource(item.contentDescriptionResId),
                contentScale = ContentScale.Crop
            )
        }
    }

    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val modelLocation: String
    )


}
