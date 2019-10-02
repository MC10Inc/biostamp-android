package com.mc10inc.biostamp3.sdkexample.streaming;

import android.content.Context;
import android.view.MotionEvent;

import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.RectangularPrism;
import org.rajawali3d.renderer.Renderer;

public class RotationRenderer extends Renderer {
    private DirectionalLight directionalLight;
    private RectangularPrism shape;

    public RotationRenderer(Context context) {
        super(context);
        setFrameRate(60);
    }

    @Override
    protected void initScene() {
        getCurrentScene().setBackgroundColor(0x00000000);

        directionalLight = new DirectionalLight(1.0f, 1.0f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(1.6f);
        getCurrentScene().addLight(directionalLight);

        shape = new RectangularPrism(1.0f, 0.5f, 0.2f);
        Material material = new Material();
        material.setColor(0xff888888);
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        shape.setMaterial(material);
        getCurrentScene().addChild(shape);
        getCurrentCamera().setY(-2.5);
        getCurrentCamera().setZ(1);
        getCurrentCamera().setUpAxis(Vector3.Axis.Z);
        getCurrentCamera().setLookAt(0, 0, 0);
        getCurrentCamera().enableLookAt();
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    public void update(Quaternion quat) {
        shape.setRotation(quat.inverse());
    }
}
