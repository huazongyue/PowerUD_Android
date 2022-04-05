package com.example.powerud;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.utils.ListAdapter;
import com.example.utils.ToastUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.Semaphore;


public class MainActivity extends AppCompatActivity {

    public int TIMEOUT = 5000; //ms
    //控件
    private Button bt_paging;
    private Button bt_setParam;
    private Button bt_fataTest;
    private ProgressBar progressBar;
    private TextView tv_progress;
    //线程
    private TCPServerThread tcpServerThread = null;
    private RecvDataProcessThread recvDataProcessThread = null;
    //变量
    private int startIndex = 0;
    private int endIndex = 3;
    public int[] test_result = new int[32];
    ArrayList<String> paramsList = new ArrayList<String>();
    public String recvString = "";
    //信号量
    private Semaphore semaphore = new Semaphore(1);
    //状态
    private int current_chipNmb = 0;
    public boolean recvFlag = false;
    public boolean isRepeat = false;
    private boolean isPaging = false;
    private boolean isCurrentReporting = false;

    //参数
    boolean isStep = true;
    int start = 0;
    int end = 31;
    int Vh = 5000;
    int step = 10;
    int Vl = 1200;
    int Tr = 50;
    int Td = 50;
    int Tms = 1000;
    int Tn = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        setListener();
        startTCPServer();
    }
    protected void initUI(){
        bt_paging = findViewById(R.id.paging);
        bt_setParam = findViewById(R.id.set_parameters);
        bt_fataTest = findViewById(R.id.bt_fast_test);
        progressBar = findViewById(R.id.progressBar);
        tv_progress = findViewById(R.id.tv_progress);
        bt_paging.setEnabled(false);


    }
    protected void setListener(){
        OnClick onClick = new OnClick();
        bt_paging.setOnClickListener(onClick);
        bt_setParam.setOnClickListener(onClick);
        bt_fataTest.setOnClickListener(onClick);

    }

    private Handler recvHandler = new Handler(){
        public void handleMessage(Message msg){
            recvString = msg.obj.toString();
        }
    };

    protected void startTCPServer(){
        tcpServerThread = new TCPServerThread(12580);
        tcpServerThread.start();
        recvDataProcessThread = new RecvDataProcessThread();
        recvDataProcessThread.start();
    }

    class OnClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.paging:
                    current_chipNmb = 0;
                    if(!isPaging){
                        Paging paging = new Paging();
                        paging.start();
                        isPaging = true;
                    }
                    else {
                        ToastUtil.showMsg(getApplicationContext(),"Paging is not end!");
                    }
                    break;
                case R.id.set_parameters:
                    setParam();
                    break;
                case R.id.bt_fast_test:
                    current_chipNmb = 0;
                    String cmd = "?[255],[set],[Vh5000,Vl0,Tr50,Td50,Tms1000,Tn5]";
                    FastTest fastTest = new FastTest(cmd);
                    fastTest.start();
                    break;
            }
        }
    }
    public void setParam(){
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_dialog,null);
        //获取AlertDialog中的控件
        EditText et_setVh = view.findViewById(R.id.et_vh);
        EditText et_setVl = view.findViewById(R.id.et_vl);
        EditText et_setTr = view.findViewById(R.id.et_tr);
        EditText et_setTd = view.findViewById(R.id.et_td);
        EditText et_setTms = view.findViewById(R.id.et_tms);
        EditText et_setTn = view.findViewById(R.id.et_tn);
        EditText et_setStep = view.findViewById(R.id.et_step);
        TextView tv_cancel = view.findViewById(R.id.tv_cancel);
        TextView tv_confirm = view.findViewById(R.id.tv_confirm);
        ListView lv_params = view.findViewById(R.id.lv_params);
        Button bt_addparam = view.findViewById(R.id.bt_addparam);

        //AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set parameter");
        builder.setView(view);
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.show();
        ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,R.layout.layout_editlist,paramsList);
        lv_params.setAdapter(adapter);

        //添加测试参数条件
        bt_addparam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastUtil.showMsg(getApplicationContext(),"add.");
                String newParam = "";
                newParam = "?[255],[set],[Vh:"+et_setVh.getText()+",Vl[0,"+et_setVl.getText()+ ","+et_setStep.getText()
                        +"]," +"Tr:"+et_setTr.getText()+",Td:"+et_setTd.getText()+
                        ",Tms:"+et_setTms.getText()+",Tn:"+et_setTn.getText()+"]";
                paramsList.add(newParam);
                adapter.notifyDataSetChanged();

            }
        });

        //长按删除
        lv_params.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                paramsList.remove(i);
                adapter.notifyDataSetChanged();
                return true;
            }
        });


        //取消按钮事件
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastUtil.showMsg(getApplicationContext(),"Cancel");
                paramsList.clear();
                alertDialog.dismiss();
            }
        });
        //确定按钮事件
        tv_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastUtil.showMsg(getApplicationContext(),"Set parameters successfully!");
                alertDialog.dismiss();
            }
        });

    }
    public void sendData(String cmd){
        if(tcpServerThread == null || tcpServerThread.outputstream == null){
            return;
        }
        try
        {
            //添加结束符
            String cmd_all =  cmd + "\r\n\0";
            //计算校验值
            int crc8 = calcCRC8(cmd_all);
            //得到数据的Hex字符串
            String hex_cmd = stringToHexString(cmd) + "0d0a00";
            //Hex转String
            String hex_all_cmd = hexStringToString(hex_cmd);
            //两次分开发 目前是有问题的
            tcpServerThread.outputstream.write(hex_all_cmd.getBytes(StandardCharsets.UTF_8));
            tcpServerThread.outputstream.write(crc8&0xff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class FastTest extends Thread{
        String param = "";

        public FastTest(String param){
            this.param = param;
        }
        @Override
        public void run() {
            super.run();
            //set
            sendData(param);
            try {
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //repeat
            isRepeat = true;
            SendCMDThread repeatThread = new SendCMDThread(2, startIndex, endIndex);
            repeatThread.start();

            while(isRepeat);
            //report
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            current_chipNmb = 0;
            SendCMDThread reportThread = new SendCMDThread(3, startIndex, endIndex);
            reportThread.start();
        }
    }

    class Paging extends Thread{
        @Override
        public void run() {
            super.run();
            int startId = startIndex;
            int EndId = endIndex;
            for(int i = startId; i <= EndId; i++){
                int tempIndex = i;
                runOnUiThread(new Runnable()//不允许其他线程直接操作组件，用提供的此方法可以
                {
                    public void run()
                    {
                        int progress = ((tempIndex+1)*100 / 32);
                        progressBar.setProgress(progress);
                        tv_progress.setText(String.valueOf(progress)+"%");
                    }
                });

                recvFlag = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String cmd = "?["+String.valueOf(tempIndex)+"],[paging]";
                        sendData(cmd);
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                int timeout = TIMEOUT;
                while(recvFlag == false && (--timeout) != 0){
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                recvFlag = true;
                current_chipNmb++;
            }
            isPaging = false;
        }
    }

    class SendCMDThread extends Thread{
        int cmdType = 0;
        int startIndex;
        int endIndex;

        public SendCMDThread(int cmdType, int start, int end){
            this.cmdType = cmdType;
            this.startIndex = start;
            this.endIndex = end;
        }
        @Override
        public void run() {
            super.run();
            for(int i = this.startIndex; i <= this.endIndex; i++){
                int tempIndex = i;
                runOnUiThread(new Runnable()//不允许其他线程直接操作组件，用提供的此方法可以
                {
                    public void run()
                    {
                        ImageView chips = findChip(tempIndex);
                        int progress = ((tempIndex+1)*100 / 32);
                        progressBar.setProgress(progress);
                        tv_progress.setText(String.valueOf(progress)+"%");
                    }
                });
//                if((cmdType == 2 || cmdType == 3) && ((test_result[tempIndex] == -1)||(test_result[tempIndex] == 0) )){
//                    current_chipNmb++;
//                    continue;
//                }
                recvFlag = false;
                new Thread(new Runnable() {
                    String str_cmd = "";
                    @Override
                    public void run() {
                        switch(cmdType){
                            case 1:
                                str_cmd = "paging";break;
                            case 2:
                                str_cmd = "repeat";break;
                            case 3:
                                str_cmd = "report";break;
                        }
                        String cmd = "?["+String.valueOf(tempIndex)+"],["+str_cmd+"]";
                        if(cmdType == 3){
                            isCurrentReporting = true;
                            while(isCurrentReporting){
                                sendData(cmd);
                                int timeout = TIMEOUT;
                                while(recvFlag == false && (--timeout) != 0){
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }else{
                            sendData(cmd);
                        }
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                if(cmdType == 3){
                    while(isCurrentReporting);
                }
                int timeout = TIMEOUT;
                while(recvFlag == false && (--timeout) != 0){
//                    if(tempIndex == endIndex){
//                        break;
//                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                recvFlag = true;
                current_chipNmb++;
            }
            isRepeat = false;
        }
    }


    class RecvDataProcessThread extends Thread{
        @Override
        public void run() {
            super.run();
            Resources resources = getResources();
            Drawable chip,chip_none,chip_testing,chip_ok,chip_error,chip_testing_error;
            chip = resources.getDrawable(R.drawable.chip);
            chip_none = resources.getDrawable(R.drawable.chip_none);
            chip_testing = resources.getDrawable(R.drawable.chip_testing);
            chip_ok = resources.getDrawable(R.drawable.chip_ok);
            chip_error = resources.getDrawable(R.drawable.chip_error);
            chip_testing_error = resources.getDrawable(R.drawable.chip_testing_error);
            while(true){
                int cmdType = 0;
                if(recvString != ""){
                    if(recvString.indexOf("["+String.valueOf(current_chipNmb)+"],[paging]") >= 0){
                        cmdType = 1;
                        test_result[current_chipNmb] = 1;
                    }
                    else if(recvString.indexOf("["+String.valueOf(current_chipNmb)+"],[set]") >= 0){
                        cmdType = 2;
                        if(recvString.indexOf("[OK]") >= 0){
                            test_result[current_chipNmb] = 2;
                        }
                        else test_result[current_chipNmb] = -2;
                    }
                    else if(recvString.indexOf("["+String.valueOf(current_chipNmb)+"],[report]") >= 0){
                        cmdType = 3;
                        if(recvString.indexOf("[OK]") >= 0){
                            test_result[current_chipNmb] = 3;
                            isCurrentReporting = false;
                        }
                        else if(recvString.indexOf("[ERROR") >= 0){
                            test_result[current_chipNmb] = -3;
                            isCurrentReporting = false;
                        }
                        else if (recvString.indexOf("[BUSY") >= 0){
                            test_result[current_chipNmb] = 2;
                        }
                        else {
                            test_result[current_chipNmb] = 2;
                        }
                    }
                    recvFlag = true;
                    int timeout = TIMEOUT;
                    int temp_nmb = current_chipNmb;
                    while(recvFlag && (--timeout) != 0){
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String DEBUG = recvString;
                    recvFlag = false;
                    String ack_error_type = "";
                    if(timeout == 0) {
                        //paging_result[current_chipNmb] = -1;
                        switch (cmdType){
                            case 0:ack_error_type = "ACK ERROR";break;
                            case 1:ack_error_type = "PAGING ACK ERROR";break;
                            case 2:ack_error_type = "REPEAT ACK ERROR";break;
                            case 3:ack_error_type = "REPORT ACK ERROR";break;
                        }
                    }
                    recvString = "";
                    int finalCmdType = cmdType;
                    String finalAck_error_type = ack_error_type;
                    runOnUiThread(new Runnable()//不允许其他线程直接操作组件，用提供的此方法可以
                    {
                        public void run()
                        {
                            String s = DEBUG;
                            ImageView chips = findChip(temp_nmb);
                            if(finalCmdType == 0){
                                ToastUtil.showMsg(getApplicationContext(), "TIMEOUT,"+ finalAck_error_type);
                                return;
                            }
                            if(finalCmdType == 1){
                                if(test_result[temp_nmb] == 1){
                                    chips.setImageDrawable(chip);
                                }
                                else{
                                    chips.setImageDrawable(chip_none);
                                }
                            }
                            if(finalCmdType == 2){
                                if(test_result[temp_nmb] == 2){
                                    chips.setImageDrawable(chip_testing);
                                }
                                else{
                                    chips.setImageDrawable(chip_error);
                                }
                            }
                            if(finalCmdType == 3){
                                if(test_result[temp_nmb] == 3){
                                    chips.setImageDrawable(chip_ok);
                                }
                                else{
                                    chips.setImageDrawable(chip_error);
                                }
                            }


                        }
                    });
                } else {
                    //do nothing
                }
            }
        }
    }

    class TCPServerThread extends Thread{
        OutputStream outputstream = null;
        Socket socket = null;
        String ip;
        int port = 0;
        public TCPServerThread(int port){
            this.ip = ip;
            this.port = port;
        }

        public String getPort() {
            if(this.port != 0){
                return String.valueOf(port);
            }
            else{
                return "";
            }
        }
        @Override
        public void run() {
            try {
                //创建TCP服务器
                ServerSocket serverSocket = new ServerSocket(port);
                runOnUiThread(new Runnable()//不允许其他线程直接操作组件，用提供的此方法可以
                {
                    public void run()
                    {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "IP:"+getLocalIPAddress()+"  Port:"+getPort(), Toast.LENGTH_SHORT).show();
                    }
                });
                //创建监听
                socket = serverSocket.accept();

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputstream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(socket.isConnected()){
                runOnUiThread(new Runnable()//不允许其他线程直接操作组件，用提供的此方法可以
                {
                    public void run()
                    {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "client has connected!", Toast.LENGTH_SHORT).show();
                        bt_paging.setEnabled(true);
                    }
                });

            }
            while (true){
                final byte[] buffer = new byte[1024];//创建接收缓冲区
                int len = 0;
                InputStream inputstream = null;

                try {
                    inputstream = socket.getInputStream();
                    len = inputstream.read(buffer);//数据读出来，并且返回数据的长度
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(len > 0){
                    Message msg = new Message();
                    msg.obj = (new String(buffer,0, len));
                    recvHandler.sendMessage(msg);

                }
            }
        }
    }
    /**获得IP地址，分为两种情况，一是wifi下，二是移动网络下，得到的ip地址是不一样的*/
    String getLocalIPAddress() {
        Context context=MainActivity.this;
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                //调用方法将int转换为地址字符串
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }
    //IP地址转换
    String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    //crc8 算法
    public static int calcCRC8(String string){
        byte crc = 0;
        for (int j = 0; j < string.length(); j++) {
            crc ^= string.charAt(j);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = (byte) ((crc)<< 1);
                    crc ^= 0x31;
                } else {
                    crc = (byte) ((crc)<< 1);
                }
            }
        }
        return crc&0xFF;
    }
    public static String stringToHexString(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

    public static String getIntegerOfString(String str) {
        str = str.trim();
        String str2 = "";
        if(str != null && !"".equals(str)){
            for(int i = 0; i < str.length(); i++){
                if(str.charAt(i)>=48 && str.charAt(i)<=57){
                    str2 += String.valueOf(str.charAt(i));
                }
            }
        }
        return str2;
    }

    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(
                        s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "utf-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }
    public static int[] hexStringToInt(String hexString){
        if(hexString.length()%2 != 0){
            return null;
        }
        int[] intOfHex = new int[hexString.length()/2];
        for(int i = 0; i < hexString.length(); i += 2){
            //int temp = Integer.parseInt(hexString.charAt(i)+hexString.charAt(i+1) + "",16);
            int temp = Integer.decode("0x"+hexString.charAt(i)+hexString.charAt(i+1));
            intOfHex[i/2] = temp&0xff;
        }
        return intOfHex;
    }

    public ImageView findChip(int num){
        ImageView iv = null;
        switch (num){
            case 0:  iv = findViewById(R.id.chip0);break;
            case 1:  iv = findViewById(R.id.chip1);break;
            case 2:  iv = findViewById(R.id.chip2);break;
            case 3:  iv = findViewById(R.id.chip3);break;
            case 4:  iv = findViewById(R.id.chip4);break;
            case 5:  iv = findViewById(R.id.chip5);break;
            case 6:  iv = findViewById(R.id.chip6);break;
            case 7:  iv = findViewById(R.id.chip7);break;
            case 8:  iv = findViewById(R.id.chip8);break;
            case 9:  iv = findViewById(R.id.chip9);break;
            case 10: iv = findViewById(R.id.chip10);break;
            case 11: iv = findViewById(R.id.chip11);break;
            case 12: iv = findViewById(R.id.chip12);break;
            case 13: iv = findViewById(R.id.chip13);break;
            case 14: iv = findViewById(R.id.chip14);break;
            case 15: iv = findViewById(R.id.chip15);break;
            case 16: iv = findViewById(R.id.chip16);break;
            case 17: iv = findViewById(R.id.chip17);break;
            case 18: iv = findViewById(R.id.chip18);break;
            case 19: iv = findViewById(R.id.chip19);break;
            case 20: iv = findViewById(R.id.chip20);break;
            case 21: iv = findViewById(R.id.chip21);break;
            case 22: iv = findViewById(R.id.chip22);break;
            case 23: iv = findViewById(R.id.chip23);break;
            case 24: iv = findViewById(R.id.chip24);break;
            case 25: iv = findViewById(R.id.chip25);break;
            case 26: iv = findViewById(R.id.chip26);break;
            case 27: iv = findViewById(R.id.chip27);break;
            case 28: iv = findViewById(R.id.chip28);break;
            case 29: iv = findViewById(R.id.chip29);break;
            case 30: iv = findViewById(R.id.chip30);break;
            case 31: iv = findViewById(R.id.chip31);break;
            default: iv = null;
        }
        return iv;
    }
}