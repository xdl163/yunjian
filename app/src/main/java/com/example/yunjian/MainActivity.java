package com.example.yunjian;

import android.annotation.SuppressLint;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import zpSDK.zpSDK.zpBluetoothPrinter;

public class MainActivity extends AppCompatActivity {
    public static BluetoothAdapter myBluetoothAdapter;
    public String selectedBDAddress = "00:00:05:DB:69:20";
    private StatusBox statusBox;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!this.listBluetoothDevice()) {
            finish();
        }
        Button printButton = findViewById(R.id.buttonPrint);
        statusBox = new StatusBox(this, printButton);
        printButton.setOnClickListener(v -> print(selectedBDAddress));
    }

    public boolean listBluetoothDevice() {
        final List<Map<String, String>> list = new ArrayList<>();
        ListView listView = findViewById(R.id.listViewDevices);
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                list,
                android.R.layout.simple_list_item_2,
                new String[]{"DeviceName", "BDAddress"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        listView.setAdapter(adapter);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (myBluetoothAdapter == null) {
            Toast.makeText(this, "没有找到蓝牙适配器", Toast.LENGTH_LONG).show();
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }

        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
        }

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() <= 0) {
            Toast.makeText(this, "请先与蓝牙打印机配对", Toast.LENGTH_LONG).show();
            return false;
        }
        for (BluetoothDevice device : pairedDevices) {
            Map<String, String> map = new HashMap<>();
            map.put("DeviceName", device.getName());
            map.put("BDAddress", device.getAddress());
            list.add(map);
        }
        adapter.notifyDataSetChanged();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedBDAddress = list.get(position).get("BDAddress");
                if (parent.getTag() != null) {
                    ((View) parent.getTag()).setBackgroundDrawable(null);
                }
                parent.setTag(view);
                view.setBackgroundColor(getColor(R.color.selected_device_background));
            }
        });
        return true;
    }

    public void print(String bdAddress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
            Toast.makeText(this, "需要蓝牙权限以连接打印机", Toast.LENGTH_LONG).show();
            return;
        }

        zpBluetoothPrinter zpSDK = new zpBluetoothPrinter(this);
        if (!zpSDK.connect(bdAddress)) {
            Toast.makeText(this, "连接失败", Toast.LENGTH_LONG).show();
            return;
        }

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher_foreground, getTheme());
        Bitmap bmp = drawableToBitmap(drawable);

        zpSDK.pageSetup(668, 668);
        zpSDK.drawBarCode(8, 540, "12345678901234567", 128, true, 3, 60);
        zpSDK.drawGraphic(90, 48, 0, 0, bmp);
        zpSDK.drawQrCode(350, 48, "111111111", 0, 3, 0);
        zpSDK.drawText(90, 148, "400-8800-", 3, 0, 0, false, false);
        zpSDK.drawText(100, 204, "示例订单", 4, 0, 0, false, false);
        zpSDK.drawText(250, 260, "经由  演示", 2, 0, 0, false, false);
        zpSDK.drawText(100, 340, "订单编号 123456", 3, 0, 0, false, false);
        zpSDK.drawText(100, 420, "2024-01-01  12:00", 3, 0, 0, false, false);
        zpSDK.drawBarCode(124, 500, "12345678901234567", 128, false, 3, 60);
        zpSDK.print(0, 0);
        zpSDK.printerStatus();
        int status = zpSDK.GetStatus();
        if (status == -1) {
            Toast.makeText(this, "获取状态异常", Toast.LENGTH_LONG).show();
        } else if (status == 1) {
            Toast.makeText(this, "缺纸", Toast.LENGTH_LONG).show();
        } else if (status == 2) {
            Toast.makeText(this, "开盖", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "打印机正常", Toast.LENGTH_LONG).show();
        }
        zpSDK.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            listBluetoothDevice();
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1,
                drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
