/*
 * Copyright (C) The Android Open Source Project
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
package com.google.android.gms.samples.vision.face.facetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

import static com.google.android.gms.internal.zzs.TAG;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final float SIZE_FACTOR = 2.3f;
    private static final float THRESHOLD_OPEN = 0.7f;
    private static final float THRESHOLD_HALF_OPEN = 0.3f;

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private float mFaceHappiness;

    private Context mContext;
    private boolean mRotateEnabled = true;

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);

        mContext = context;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }


        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        List<Landmark> landmarkList = face.getLandmarks();
        PointF ptNose = null;
        PointF ptMouth = null;
        float degree = 0;
        for (int i=0; i< landmarkList.size(); i++) {
            Landmark landmark = landmarkList.get(i);
            canvas.drawCircle(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y), FACE_POSITION_RADIUS, mFacePositionPaint);
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:
                    break;
                case Landmark.RIGHT_EYE:
                    break;
                case Landmark.LEFT_MOUTH:
                    break;
                case Landmark.RIGHT_MOUTH:
                    break;
                case Landmark.NOSE_BASE:
                    ptNose = landmark.getPosition();
                    break;
                case Landmark.BOTTOM_MOUTH:
                    ptMouth = landmark.getPosition();
                default:
                    break;
            }
        }
        if (mRotateEnabled && ptNose != null && ptMouth != null) {
            float yLength = ptNose.y - ptMouth.y;
            float xLength = ptNose.x - ptMouth.x;
            if (yLength != 0) {
                degree = (float) Math.toDegrees(Math.atan(xLength / yLength));
            } else {
                if (xLength > 0) {
                    degree = 90;
                } else {
                    degree = -90;
                }
            }

            Log.d(TAG, "degree: " + degree);
            if (degree != 0) {
                canvas.save();
                canvas.rotate(degree, x, y);
            }
        }

//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
//        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("happiness: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);
//        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint);
//        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), x - ID_X_OFFSET*2, y - ID_Y_OFFSET*2, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f) * SIZE_FACTOR;
        float yOffset = scaleY(face.getHeight() / 2.0f) * SIZE_FACTOR;
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        // Draw from resource
        Drawable head = mContext.getResources().getDrawable(R.drawable.female_003_head_hd, null);
        head.setBounds((int)left, (int)top, (int)right, (int)bottom);
        //head.draw(canvas);
        if (face.getIsLeftEyeOpenProbability() > THRESHOLD_OPEN || face.getIsRightEyeOpenProbability() > THRESHOLD_OPEN) {
            Drawable eyes = mContext.getResources().getDrawable(R.drawable.female_003_eye01_hd, null);
            eyes.setBounds((int) left, (int) top, (int) right, (int) bottom);
            eyes.draw(canvas);
        } else if (face.getIsLeftEyeOpenProbability() < THRESHOLD_HALF_OPEN || face.getIsRightEyeOpenProbability() < THRESHOLD_HALF_OPEN) {
            Drawable eyes = mContext.getResources().getDrawable(R.drawable.female_003_eye02_hd, null);
            eyes.setBounds((int) left, (int) top, (int) right, (int) bottom);
            eyes.draw(canvas);
        } else {
            Drawable eyes = mContext.getResources().getDrawable(R.drawable.female_003_eye03_hd, null);
            eyes.setBounds((int) left, (int) top, (int) right, (int) bottom);
            eyes.draw(canvas);
        }

        if (face.getIsSmilingProbability() > THRESHOLD_OPEN) {
            Drawable mouth = mContext.getResources().getDrawable(R.drawable.female_003_smile03_hd, null);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        } else if (face.getIsSmilingProbability() > THRESHOLD_HALF_OPEN) {
            Drawable mouth = mContext.getResources().getDrawable(R.drawable.female_003_smile02_hd, null);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        } else {
            Drawable mouth = mContext.getResources().getDrawable(R.drawable.female_003_smile01_hd, null);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        }

        if (degree != 0) {
            canvas.restore();
        }
    }
}