package com.example.targetscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proc_image);
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        System.out.println("Usli smo u ac2");
        long addr = getIntent().getLongExtra("mat", 0);
        Mat tempImg = new Mat(addr);
        Mat img = tempImg.clone();
        Bitmap img2 = convertMatToBitMap(img);
        if (img2 == null){
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Msg", "Target not found");
            setResult(2, resultIntent);
            finish();
        }
        imageView.setImageBitmap(img2);
    }

    private static Bitmap convertMatToBitMap(Mat input){
        Bitmap bmp = null;
        Mat image = input.clone();
        Mat imageF = input.clone();
        Mat kernel = Mat.ones(5,5, CvType.CV_8U);
        double w,h;
        Point point = new Point(200,200);
        Scalar color = new Scalar(0,255,0);
        w = image.cols();
        h = image.rows();
        double ratio = h/w;
        Size size = new Size(1024, 1024*ratio);
        Imgproc.resize(image, image, size);
        Mat imageO = image.clone();
        Mat edge = image.clone();
        Mat grayC = new Mat(0,0,CvType.CV_8UC1);
        Imgproc.cvtColor(imageO, grayC, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(grayC, grayC,110,255, Imgproc.THRESH_BINARY_INV);
        Imgproc.erode(grayC, grayC, kernel);
        Imgproc.dilate(grayC, grayC, kernel);
        Imgproc.medianBlur(grayC, grayC, 15);
        Imgproc.Canny(grayC, edge,50,255);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edge, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        Point[] centers = new Point[contours.size()];
        float[][] radius = new float[contours.size()][1];
        Mat drawing = Mat.zeros(edge.size(), CvType.CV_8UC3);
        double cx = 0,cy = 0,cw = 0,ch = 0;
        List<Rect> rectArr = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
            cx = boundRect[i].tl().x;
            cy = boundRect[i].tl().y;
            cw = boundRect[i].br().x - cx;
            ch = boundRect[i].br().y - cy;
            Log.i("w", String.valueOf(cw));
            Log.i("h", String.valueOf(ch));
            if(cw > 200 && cw < 520 && ch > 200 && ch < 520){
                rectArr.add(boundRect[i]);
            }
        }
        for (Rect r:rectArr) {
            System.out.println(r);
        }
        Collections.sort(rectArr, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                double h1 = o1.br().y - o1.tl().y;
                double w1 = o1.br().x - o1.tl().x;
                double h2 = o2.br().y - o2.tl().y;
                double w2 = o2.br().x - o2.tl().x;
                int result = Double.compare(Math.abs(w1-h1), Math.abs(w2-h2));
                return result;
            }
        } );
        Rect a;
        try{
            a = rectArr.get(0);
        }catch (Exception e){
            return null;
        }
        double wR = ((a.br().y - a.tl().y) + (a.br().x - a.tl().x)) /2;
        double R = wR/2/7.4;
                ;
        double aW = a.br().x - a.tl().x;
        double aH = a.br().y - a.tl().y;
        Log.i("Radius", String.valueOf(R));
        int offsetX = (int)aW / 2;
        int offsetY = (int)aH / 2;
        Rect offSet = new Rect((int)a.tl().x-offsetX, (int)a.tl().y-offsetY, (int)aW + (offsetY*2), (int)aH + (offsetY*2));
        Mat cropped = image.submat(offSet);
        Scalar lower = new Scalar(80, 25, 108);
        Scalar upper = new Scalar(179, 255, 255);
        Mat hsv = new Mat();
        Mat mask = new Mat();
        Mat output = new Mat();
        Mat gray = new Mat();
        Mat erosion = new Mat();
        Mat dil = new Mat();
        Mat edgeH = new Mat();
        Mat hierarchyH = new Mat();
        List<MatOfPoint> contoursH = new ArrayList<>();
        Imgproc.cvtColor(cropped,hsv,Imgproc.COLOR_RGB2HSV);
        Core.inRange(hsv, lower, upper, mask);
        Mat kernel1 = Mat.ones(3,3, CvType.CV_8U);
        Imgproc.cvtColor(cropped,cropped,Imgproc.COLOR_BGR2RGB);
        Core.bitwise_and(cropped, cropped, output, mask);
        Imgproc.cvtColor(output, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.erode(mask, erosion, kernel);
        Imgproc.dilate(erosion, dil, kernel);
        Imgproc.medianBlur(dil, dil, 17);
        Imgproc.Canny(dil, edgeH, 50, 255);
        Imgproc.findContours(edgeH, contoursH, hierarchyH, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat bg = Mat.zeros(cropped.size(), CvType.CV_8UC1);
        Mat bgcopy = Mat.zeros(cropped.size(), CvType.CV_8UC1);
        Mat bg1 = Mat.zeros(cropped.size(), CvType.CV_8UC1);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        List<MatOfPoint> contoursC = new ArrayList<>();
        Moments M = new Moments();
        List<Point> hits = new ArrayList<>();
        for(MatOfPoint c:contoursH){
            for(Point p:c.toList()){
                Imgproc.circle(bg1, p, (int)R, new Scalar(73,73,73), 1,8,0);
                Core.addWeighted(bg,1,bg1,1,0,bg);
                bg1 = Mat.zeros(gray.size(), CvType.CV_8UC1);
            }

            Mat k = new Mat();
            Imgproc.threshold(bg, bg, 200, 255, Imgproc.THRESH_BINARY);
            Imgproc.erode(bg, bg, kernel1);
            //Imgproc.dilate(bg, bg, kernel1);
            Imgproc.medianBlur(bg, bg, 3);
            Imgproc.dilate(bg, bg, kernel1);

            Imgproc.findContours(bg, contoursC, hierarchyH, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Point centeroid = new Point();
            for(MatOfPoint cC:contoursC){
                M = Imgproc.moments(cC);
                if(M.get_m00()!=0){
                   centeroid.x = M.get_m10() / M.get_m00();
                   centeroid.y = M.get_m01() / M.get_m00();
                   double[] colo = dil.get((int)centeroid.y, (int)centeroid.x);
                   Double sth = colo[0];
                   if(sth > 50){
                       hits.add(new Point(centeroid.x, centeroid.y));
                   }
                }
            }
            Core.addWeighted(bgcopy,1,bg,1,0,bgcopy);
            bg = Mat.zeros(gray.size(), CvType.CV_8UC1);
        }
        Imgproc.circle(cropped, new Point(cropped.width()/2, cropped.height()/2), 1, new Scalar(0,255,0), 4, 8,0 );
        Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_BGR2RGB);
        double segment = cropped.width()/2/6;
        for (Point h1:hits){
            Imgproc.circle(cropped, h1, (int) R, color, 1, 8,0);
            double aa = h1.x - cropped.width();
            double bb = h1.y - cropped.height();
            double d = (aa*aa) + (bb*bb);
            d = Math.sqrt(d);
            System.out.println((10-((d-R)/segment)));
        }

        try {
            bmp = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, bmp);
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
        return bmp;
    }
}