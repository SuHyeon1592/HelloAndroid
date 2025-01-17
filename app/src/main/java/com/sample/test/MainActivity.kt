package com.sample.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.opencv.core.*
import org.opencv.core.Core.max
import org.opencv.core.Core.sqrt
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private val OPEN_GALLERY = 1
    val src = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
/*
        val openGallery_button = findViewById<Button>(R.id.openGallery_button)

        openGallery_button.setOnClickListener{openGallery()}


   */

        val graySrc = Mat()
        Imgproc.cvtColor(src, graySrc, Imgproc.COLOR_BGR2GRAY)

// 이진화
        val binarySrc = Mat()
        Imgproc.threshold(graySrc, binarySrc, 0.0, 255.0, Imgproc.THRESH_OTSU)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            binarySrc,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_NONE
        )

        var biggestContour: MatOfPoint? = null
        var biggestContourArea: Double = 0.0
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > biggestContourArea) {
                biggestContour = contour
                biggestContourArea = area
            }
        }

        if (biggestContour == null) {
            throw IllegalArgumentException("No Contour")
        }
// 너무 작아도 안됨
        if (biggestContourArea < 400) {
            throw IllegalArgumentException("too small")
        }

        val candidate2f = MatOfPoint2f(*biggestContour.toArray())
        val approxCandidate = MatOfPoint2f()
        Imgproc.approxPolyDP(
            candidate2f,
            approxCandidate,
            Imgproc.arcLength(candidate2f, true) * 0.02,
            true
        )

        // 사각형 판별
        if (approxCandidate.rows() != 4) {
            throw java.lang.IllegalArgumentException("It's not rectangle")
        }

// 컨벡스(볼록한 도형)인지 판별
        if (!Imgproc.isContourConvex(MatOfPoint(*approxCandidate.toArray()))) {
            throw java.lang.IllegalArgumentException("It's not convex")
        }

        // 좌상단부터 시계 반대 방향으로 정점을 정렬한다.
        val points = arrayListOf(
            Point(approxCandidate.get(0, 0)[0], approxCandidate.get(0, 0)[1]),
            Point(approxCandidate.get(1, 0)[0], approxCandidate.get(1, 0)[1]),
            Point(approxCandidate.get(2, 0)[0], approxCandidate.get(2, 0)[1]),
            Point(approxCandidate.get(3, 0)[0], approxCandidate.get(3, 0)[1]),
        )
        points.sortBy { it.x } // x좌표 기준으로 먼저 정렬

        if (points[0].y > points[1].y) {
            val temp = points[0]
            points[0] = points[1]
            points[1] = temp
        }

        if (points[2].y < points[3].y) {
            val temp = points[2]
            points[2] = points[3]
            points[3] = temp
        }
// 원본 영상 내 정점들

        // 사각형 꼭짓점 정보로 사각형 최대 사이즈 구하기
// 평면상 두 점 사이의 거리는 직각삼각형의 빗변길이 구하기와 동일
        fun calculateMaxWidthHeight(
            tl:Point,
            tr:Point,
            br:Point,
            bl:Point,
        ): Size {
            // Calculate width
            val widthA = sqrt((tl.x - tr.x) * (tl.x - tr.x) + (tl.y - tr.y) * (tl.y - tr.y))
            val widthB = sqrt((bl.x - br.x) * (bl.x - br.x) + (bl.y - br.y) * (bl.y - br.y))
            val maxWidth = max(widthA, widthB)
            // Calculate height
            val heightA = sqrt((tl.x - bl.x) * (tl.x - bl.x) + (tl.y - bl.y) * (tl.y - bl.y))
            val heightB = sqrt((tr.x - br.x) * (tr.x - br.x) + (tr.y - br.y) * (tr.y - br.y))
            val maxHeight = max(heightA, heightB)
            return Size(maxWidth, maxHeight)
        }

        val maxSize = calculateMaxWidthHeight(
            tl = points[0],
            bl = points[1],
            br = points[2],
            tr = points[3]
        )
        val dw = maxSize.width
        val dh = dw * maxSize.height/maxSize.width
        val dstQuad = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(0.0, dh),
            Point(dw, dh),
            Point(dw, 0.0)
        )
// 투시변환 매트릭스 구하기
        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcQuad, dstQuad)

// 투시변환 된 결과 영상 얻기
        val dst = Mat()
        Imgproc.warpPerspective(src, dst, perspectiveTransform, Size(dw, dh))
    }

    private fun openGallery() {
        val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent,OPEN_GALLERY)
    }
}