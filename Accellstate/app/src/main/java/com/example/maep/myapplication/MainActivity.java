package com.example.maep.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class MainActivity extends Activity implements Runnable, SensorEventListener {
    SensorManager sm;
    TextView tv;
    TextView tv_result;
    Handler h;
    float gx, gy, gz;
    float syn_accel;

    String DirName = "/result/";
    String type = "";
    String[] alltype = {"stand","walk","run","No Data"};

    boolean d_flag = false;
    boolean w_flag = false;
    int count = 0;
    int result = 3;

    DataSource source;
    Instances instances;
    Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        tv_result = (TextView) findViewById(R.id.tv_result);

        h = new Handler();
        h.postDelayed(this, 500);

        verifyStoragePermissions(this);
        File dir = new File(Environment.getExternalStorageDirectory().getPath() +
                DirName);
        dir.mkdir();

        Button standSave = findViewById(R.id.b_stand);
        standSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                type = alltype[0];
                deleteF(type);
                dialog();
            }

        });

        Button walkSave = findViewById(R.id.b_walk);
        walkSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                type = alltype[1];
                deleteF(type);
                dialog();
            }

        });

        Button runSave = findViewById(R.id.b_run);
        runSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                type = alltype[2];
                deleteF(type);
                dialog();
            }

        });

        Button deleteAll = findViewById(R.id.b_delete);
        deleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                for(String str : alltype) {
                    deleteF(str);
                }

            }

        });

        Button makeARFF = findViewById(R.id.b_arff);
        makeARFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                makeResource();

            }

        });

        Button runWEKA = findViewById(R.id.b_weka);
        runWEKA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                w_flag = true;

            }

        });

        Button stopWEKA = findViewById(R.id.b_stop);
        stopWEKA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View a) {

                w_flag = false;
                result = 3;

            }

        });
    }

    @Override
    public void run() {
        tv.setText("X-axis : " + gx + "\n"
                + "Y-axis : " + gy + "\n"
                + "Z-axis : " + gz + "\n"
                + "syn-accel : " + syn_accel  + "\n");
        tv_result.setText("State : " + alltype[result] + "\n");
        h.postDelayed(this, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors =
                sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (0 < sensors.size()) {
            sm.registerListener(this, sensors.get(0),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacks(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        gx = event.values[0];
        gy = event.values[1];
        gz = event.values[2];
        syn_accel = (float) Math.sqrt((double) ((gx * gx) + (gy * gy) + (gz * gz)));

        if (d_flag == true) {
            String text = (syn_accel + "," + type);
            saveFile(type, text);
        }

        if(count > 10 && w_flag == true) {
            try {
                Evaluation eval = new Evaluation(instances);
                eval.evaluateModel(classifier, instances);
                System.out.println(eval.toSummaryString());

                Attribute acceleration = new Attribute("acceleration", 0);

                Instance instance = new DenseInstance(1);
                instance.setValue(acceleration, syn_accel);
                instance.setDataset(instances);

                result = (int)classifier.classifyInstance(instance);
                System.out.println(result);

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            count = 0;
        }
        else {
            count++;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static void verifyStoragePermissions(Activity activity) {
        int readPermission = ContextCompat.checkSelfPermission(activity, mPermissions[0]);
        int writePermission = ContextCompat.checkSelfPermission(activity, mPermissions[1]);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }

    public void saveFile(String typename, String str) {


        // try-with-resources
        try {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() +
                    DirName + typename + ".csv", true);

            fw.write(str + "\n");
            fw.close();

            System.out.println("保存完了!");
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("保存失敗.");
        }

    }

    public void dialog() {
        d_flag = true;
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this)
                .setTitle("hoge")
                .setMessage(type + "state sampling now")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        d_flag = false;
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        d_flag = false;
                        deleteF(type);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        d_flag = false;
                        deleteF(type);
                    }
                });

        builder.show();

    }

    public void deleteF(String str) {
        File file = new File(Environment.getExternalStorageDirectory().getPath() +
                DirName + str + ".csv");

        if(file.exists()) {
            if(file.delete()) {
                System.out.println("File delete Success");
            }
            else {
                System.out.println("File delete Fault");
            }
        }
        else {
            System.out.println("File not Found");
        }
    }

    public void makeResource() {
        try {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() +
                    DirName + "Resource.arff", false);

            fw.write("@RELATION\tmovestate\n\n" +
                    "@ATTRIBUTE\tacceleration\tREAL\n" +
                    "@ATTRIBUTE\tstate\t{stand,walk,run}\n\n" +
                    "@DATA\n");

            try {
                for(String str : alltype) {
                    FileReader fr = new FileReader(Environment.getExternalStorageDirectory().getPath() +
                            DirName + str + ".csv");
                    BufferedReader br = new BufferedReader(fr);

                    //読み込んだファイルを１行ずつ処理する
                    String line;
                    while ((line = br.readLine()) != null) {
                        fw.write(line + "\n");

                    }
                    //終了処理
                    br.close();
                }

            } catch (IOException ex) {
                //例外発生時処理
                ex.printStackTrace();
            }
            fw.close();

            makeClassifier();

            System.out.println("作成完了!");
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("作成失敗.");
        }
    }

    public void makeClassifier() {
        try {
            source = new DataSource(Environment.getExternalStorageDirectory().getPath() +
                    DirName + "Resource.arff");
            instances = source.getDataSet();
            instances.setClassIndex(1);
            classifier = new J48();
            classifier.buildClassifier(instances);

            System.out.println("make Classifier Success!");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("make Classifier Fault");
        }

    }

}
