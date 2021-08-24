package com.codewithishita.instamate;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ModelRenderable modelRenderable;
    private Texture texture;
    private boolean isAdded = false;
    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CustomArFragment customArFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);


        ModelRenderable.builder()
                .setSource(this, R.raw.fox_face)
                .build()
                .thenAccept(rendarable -> {
                    this.modelRenderable = rendarable;
                    this.modelRenderable.setShadowCaster(false);
                    this.modelRenderable.setShadowReceiver(false);

                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "error loading model", Toast.LENGTH_SHORT).show();
                    return null;
                });

        // Load the face mesh texture.(2D texture on face)
        // Save the texture(.png file) in drawable folder.
        Texture.builder()
                .setSource(this, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept(textureModel -> this.texture = textureModel)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "cannot load texture", Toast.LENGTH_SHORT).show();
                    return null;
                });

        assert customArFragment != null;

        customArFragment.getArSceneView().setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        customArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (modelRenderable == null || texture == null) {
                return;
            }
            Frame frame = customArFragment.getArSceneView().getArFrame();
            assert frame != null;

            Collection<AugmentedFace> augmentedFaces = frame.getUpdatedTrackables(AugmentedFace.class);

            // Make new AugmentedFaceNodes for any new faces.
            for (AugmentedFace augmentedFace : augmentedFaces) {
                if (isAdded) return;

                AugmentedFaceNode augmentedFaceMode = new AugmentedFaceNode(augmentedFace);
                augmentedFaceMode.setParent(customArFragment.getArSceneView().getScene());
                augmentedFaceMode.setFaceRegionsRenderable(modelRenderable);
                augmentedFaceMode.setFaceMeshTexture(texture);
                faceNodeMap.put(augmentedFace, augmentedFaceMode);
                isAdded = true;


                Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeMap.entrySet().iterator();
                Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                AugmentedFace face = entry.getKey();
                while (face.getTrackingState() == TrackingState.STOPPED) {
                    AugmentedFaceNode node = entry.getValue();
                    node.setParent(null);
                    iterator.remove();
                }
            }
        });
    }
}
