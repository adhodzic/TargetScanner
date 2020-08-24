package com.example.targetscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
    ArrayList<Double> scores = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proc_image);
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        TextView ScoreTable = (TextView)findViewById(R.id.textView);
        long addr = getIntent().getLongExtra("mat", 0);
        Mat tempImg = new Mat(addr);
        Mat img = tempImg.clone();
        Bitmap img2 = findHits(img, scores);
        if (img2 == null){
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Msg", "Target not found");
            resultIntent.putExtra("hits", scores);
            setResult(2, resultIntent);
            finish();
        }
        imageView.setImageBitmap(img2);
        String strScores = "SCORE TABLE\n\n";
        int count = 0;
        double total = 0;
        double totalRound = 0;
        for (Double s:scores) {
            total = total + s;
            totalRound = totalRound + Math.floor(s);
            count++;
            String formatS = String.format("%.2f", s);
            if(s %7==0) {
                strScores = strScores + count + ": " + formatS;
            }else{
                strScores = strScores + count + ": " + formatS + "\n";
            }
        }
        strScores = strScores + "\nTOTAL: " + totalRound + "(" +String.format("%.2f", total) + ")";
        System.out.println(strScores);
        if (strScores != ""){
            ScoreTable.setVisibility(View.VISIBLE);
            ScoreTable.setText(strScores);
        }
    }

    private static Bitmap findHits(Mat input, ArrayList<Double> scores){
        Bitmap bmp = null;
        Mat image = input.clone();

        Mat kernel = Mat.ones(5,5, CvType.CV_8U);
        Mat grayC = new Mat(0,0,CvType.CV_8UC1);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        double w,h;
        w = image.cols();
        h = image.rows();
        double ratio = h/w;
        Size size = new Size(1024, 1024*ratio);
        Imgproc.resize(image, image, size);
        Mat imageO = image.clone();
        Mat edge = image.clone();
        Imgproc.cvtColor(imageO, grayC, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(grayC, grayC,110,255, Imgproc.THRESH_BINARY_INV);
        Imgproc.erode(grayC, grayC, kernel);
        Imgproc.dilate(grayC, grayC, kernel);
        Imgproc.medianBlur(grayC, grayC, 15);
        Imgproc.Canny(grayC, edge,50,255);
        Imgproc.findContours(edge, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
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

        Rect resizeRect;
        try{
            resizeRect = rectArr.get(0);
        }catch (Exception e){
            return null;
        }
        double wR = ((resizeRect.br().y - resizeRect.tl().y) + (resizeRect.br().x - resizeRect.tl().x)) /2;
        double R = wR/2/7.2;
        double aW = resizeRect.br().x - resizeRect.tl().x;
        double aH = resizeRect.br().y - resizeRect.tl().y;
        Log.i("Radius", String.valueOf(R));
        int offsetX = (int)aW / 2;
        int offsetY = (int)aH / 2;
        Rect offSet = new Rect((int)resizeRect.tl().x-offsetX, (int)resizeRect.tl().y-offsetY, (int)aW + (offsetY*2), (int)aH + (offsetY*2));
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
        //Imgproc.medianBlur(cropped, cropped, 9);
        Imgproc.cvtColor(cropped,hsv,Imgproc.COLOR_RGB2HSV);
        Core.inRange(hsv, lower, upper, mask);
        Mat kernel1 = Mat.ones(3,3, CvType.CV_8U);
        Imgproc.cvtColor(cropped,cropped,Imgproc.COLOR_BGR2RGB);
        Core.bitwise_and(cropped, cropped, output, mask);
        Imgproc.cvtColor(output, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.erode(mask, erosion, kernel);
        Imgproc.dilate(erosion, dil, kernel);
        Imgproc.medianBlur(dil, dil, 11);
        Imgproc.Canny(dil, edgeH, 50, 255);
        Imgproc.findContours(edgeH, contoursH, hierarchyH, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat bg = Mat.zeros(cropped.size(), CvType.CV_8UC1);
        Mat bg1 = Mat.zeros(cropped.size(), CvType.CV_8UC1);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        List<MatOfPoint> contoursC = new ArrayList<>();
        List<Point> hits = new ArrayList<>();
        for(MatOfPoint c:contoursH){
            for(Point p:c.toList()){
                Imgproc.circle(bg1, p, (int)R, new Scalar(55,55,55), 1,8,0);
                Core.addWeighted(bg,1,bg1,1,0,bg);
                bg1 = Mat.zeros(gray.size(), CvType.CV_8UC1);
            }
            Mat k = new Mat();
            Imgproc.threshold(bg, bg, 254, 255, Imgproc.THRESH_BINARY);
            Imgproc.dilate(bg, bg, kernel1);
            Imgproc.medianBlur(bg,bg,3);
            Imgproc.erode(bg, bg, kernel1);
            Imgproc.medianBlur(bg,bg,3);

            Imgproc.findContours(bg, contoursC, hierarchyH, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Point centeroid = new Point();
            for(MatOfPoint cC:contoursC){
                Moments M = Imgproc.moments(cC);
                if(M.get_m00()!=0){
                   centeroid.x = M.get_m10() / M.get_m00();
                   centeroid.y = M.get_m01() / M.get_m00();
                   double[] colo = dil.get((int)centeroid.y, (int)centeroid.x);
                   Double sth = colo[0];
                   if(sth > 50){
                       if(!hits.contains(new Point(centeroid.x, centeroid.y))){
                           hits.add(new Point(centeroid.x, centeroid.y));
                       }
                   }
                }
            }
            bg = Mat.zeros(gray.size(), CvType.CV_8UC1);
        }
        Imgproc.circle(cropped, new Point(aW, aH), 1, new Scalar(0,255,0), 4, 8,0 );
        Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_BGR2RGB);
        Scalar color = new Scalar(255,0,0);
        double segment = aW/2/6;
    int index = 0;
        for (Point h1:hits){
        index++;
        Imgproc.circle(cropped, h1, (int) R, color, 1, 8,0);
        Imgproc.putText(cropped, String.valueOf(index), new Point(h1.x - (R/3.5), h1.y + (R/3.5)), Imgproc.FONT_HERSHEY_COMPLEX, R/35,  color, 1, Imgproc.LINE_AA);
        double aa = h1.x - aW;
        double bb = h1.y - aW;
        double d = (aa*aa) + (bb*bb);
        d = Math.sqrt(d);
        scores.add((10-((d-R)/segment)));
    }
        try {
        bmp = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cropped, bmp);
    }
        catch (CvException e){
        return null;
    }
        return bmp;
}
}