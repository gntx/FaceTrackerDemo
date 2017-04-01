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
import android.support.v4.content.ContextCompat;

import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    //private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    // Factor that controls the size of overlay
    private static final float HEAD_SIZE_FACTOR = 2.0f;
    // Thresholds to decide whether an eye is open, half open or closed
    private static final float THRESHOLD_EYES_OPEN = 0.65f;
    private static final float THRESHOLD_EYES_HALF_OPEN = 0.35f;
    // Thresholds to decide whether a mouth is open, half open or closed
    private static final float THRESHOLD_MOUTH_OPEN = 0.6f;
    private static final float THRESHOLD_MOUTH_HALF_OPEN = 0.2f;
    // Mininum degree to tile the head
    private static final float DIFF_ROTATE_DEGREE = 3.0f;

    private float mLastDegree = 0f;

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

        // Calculate the angle of head tilting based on NOSE_BASE and BOTTOM_MOUTH
        List<Landmark> landmarkList = face.getLandmarks();
        PointF ptNose = null;
        PointF ptMouth = null;
        for (int i=0; i< landmarkList.size(); i++) {
            Landmark landmark = landmarkList.get(i);
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
            float degree = 0;
            float yLength = ptNose.y - ptMouth.y;
            float xLength = ptNose.x - ptMouth.x;
            if (yLength != 0) {
                degree = (float) Math.toDegrees(Math.atan(xLength / yLength));
            } else {
                if (xLength > 0) {
                    degree = 90;
                } else if (xLength < 0) {
                    degree = -90;
                }
            }

            // Rotate canvas only when diff is larger than a threshold to avoid vibration
            if (Math.abs(degree - mLastDegree) > DIFF_ROTATE_DEGREE) {
                mLastDegree = degree;
            }
        } else {
            //Log.d(TAG, "nose or mouth missing");
        }

        if (mLastDegree != 0) {
            canvas.save();
            canvas.rotate(mLastDegree, x, y);
        }
//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
//        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f) * HEAD_SIZE_FACTOR;
        float yOffset = scaleY(face.getHeight() / 2.0f) * HEAD_SIZE_FACTOR;
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        // Draw from resource
        Drawable head = ContextCompat.getDrawable(mContext, R.drawable.female_003_head);
        head.setBounds((int)left, (int)top, (int)right, (int)bottom);
        head.draw(canvas);

        if (face.getIsLeftEyeOpenProbability() < THRESHOLD_EYES_HALF_OPEN) {
            Drawable leftEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_left_eye02);
            leftEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            leftEye.draw(canvas);
        } else if (face.getIsLeftEyeOpenProbability() > THRESHOLD_EYES_OPEN) {
            Drawable leftEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_left_eye01);
            leftEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            leftEye.draw(canvas);
        } else {
            Drawable leftEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_left_eye03);
            leftEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            leftEye.draw(canvas);
        }

        if (face.getIsRightEyeOpenProbability() < THRESHOLD_EYES_HALF_OPEN) {
            Drawable rightEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_right_eye02);
            rightEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            rightEye.draw(canvas);
        } else if (face.getIsRightEyeOpenProbability() > THRESHOLD_EYES_OPEN) {
            Drawable rightEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_right_eye01);
            rightEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            rightEye.draw(canvas);
        } else {
            Drawable rightEye = ContextCompat.getDrawable(mContext, R.drawable.female_003_right_eye03);
            rightEye.setBounds((int) left, (int) top, (int) right, (int) bottom);
            rightEye.draw(canvas);
        }

        if (face.getIsSmilingProbability() > THRESHOLD_MOUTH_OPEN) {
            Drawable mouth = ContextCompat.getDrawable(mContext, R.drawable.female_003_smile03);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        } else if (face.getIsSmilingProbability() > THRESHOLD_MOUTH_HALF_OPEN) {
            Drawable mouth = ContextCompat.getDrawable(mContext, R.drawable.female_003_smile02);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        } else {
            Drawable mouth = ContextCompat.getDrawable(mContext, R.drawable.female_003_smile01);
            mouth.setBounds((int) left, (int) top, (int) right, (int) bottom);
            mouth.draw(canvas);
        }

        if (mLastDegree != 0) {
            canvas.restore();
        }
    }
}
