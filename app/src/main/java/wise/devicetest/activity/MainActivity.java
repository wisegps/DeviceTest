package wise.devicetest.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import wise.devicetest.R;
import wise.devicetest.app.Constant;
import wise.devicetest.utils.MyStatusBarUtil;
import wise.devicetest.utils.SPUtils;
import wise.devicetest.utils.SystemTool;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.btn_connet)
    Button btnConnet;
    @Bind(R.id.btn_disconnet)
    Button btnDisconnet;
    @Bind(R.id.stop_data_display)
    CheckBox stopDataDisplay;
    @Bind(R.id.btn_clear)
    Button btnClear;
    @Bind(R.id.tv_receive)
    TextView tvReceive;
    @Bind(R.id.spinner)
    Spinner spinner;
    @Bind(R.id.edit_comm)
    TextInputEditText editComm;
    @Bind(R.id.btn_send)
    Button btnSend;
    @Bind(R.id.edit_comm_hex)
    TextInputEditText editCommHex;

    @Bind(R.id.auto_view)
    AutoCompleteTextView autoView;

    private boolean isFirst = true;
    private boolean isFirstSpinner = true;
    private static final int REQUEST_CODE = 0;
    private static final int REQUEST_EDIT_COMMAND_CODE = 1;
    public String ip;//服务器ip
    public String port;//服务器端口
    private List<String> commListName = new ArrayList<>();
    private List<String> commListCommand = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SocThread socketThread;
    public final int RECEIVE_DATA = 0;
    public final int SEND_DATA_SUCCESS = 1;
    public final int SEND_DATA_FAILED = 2;
    public final int CONNECT_SUCCESS = 3;
    public final int DISCONNECT = 4;
    public final int CONNECT_ERROR = 5;
    public boolean isStopDisplay = false;//暂停显示
    public boolean isConnect = false;
    public String IMEI = "" ;
    private SharedPreferences spf;
    private String command;
    private String commNormal ="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        spf = getSharedPreferences(Constant.SP_DATA, 0);
        MyStatusBarUtil.setStatusColor(this, getResources().getColor(R.color.colorPrimary));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tvReceive.setMovementMethod(ScrollingMovementMethod.getInstance());
        initAutoComplete(Constant.FIELD_AUTO,autoView);
        initSpinner();
        initSp();
        initOhterView();
    }

    /**
     * 初始化AutoCompleteTextView，最多显示5项提示，使
     * AutoCompleteTextView在一开始获得焦点时自动提示
     * @param field 保存在sharedPreference中的字段名
     * @param auto 要操作的AutoCompleteTextView
     */
    private void initAutoComplete(final String field, AutoCompleteTextView auto) {
        String longhistory = spf.getString(field, "");
        final String[]  hisArrays = longhistory.split(",");// 3. 设置适配器，为控件填充数据
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,hisArrays);
        if(hisArrays.length > 50){//只保留最近的50条的记录
            String[] newArrays = new String[50];
            System.arraycopy(hisArrays, 0, newArrays, 0, 50);
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,newArrays);
        }
        auto.setAdapter(adapter);
//        auto.setDropDownHeight(400);
        auto.setThreshold(1);
        auto.setCompletionHint("最近查询记录");
        auto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AutoCompleteTextView view = (AutoCompleteTextView) v;
                if(isFirst){
                    isFirst = false;
                }else{
                    if (hasFocus) {
                        initAutoComplete(Constant.FIELD_AUTO,autoView);
                        view.showDropDown();
                    }
                }
            }
        });
        auto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AutoCompleteTextView v = (AutoCompleteTextView) view;
                v.showDropDown();
            }
        });
        auto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String comm =  String .format(commNormal,charSequence);
                editComm.setText(comm);
                editCommHex.setText(SystemTool.stringToHexString(comm));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }


    /**
     * 把指定AutoCompleteTextView中内容保存到sharedPreference中指定的字符段
     * @param auto  要操作的AutoCompleteTextView
     */
    private void saveAutoHistory(AutoCompleteTextView auto) {
        String text = auto.getText().toString();
        String longhistory = spf.getString(Constant.FIELD_AUTO, "");
        if (!longhistory.contains(text + ",")) {
            StringBuilder sb = new StringBuilder(longhistory);
            sb.insert(0, text + ",");
            spf.edit().putString(Constant.FIELD_AUTO, sb.toString()).commit();
        }
    }

    private void initSpinner() {
        commListName.clear();
        commListCommand.clear();
        command = spf.getString(Constant.SP_COMMAND_STRING,"");
        if(TextUtils.isEmpty(command)){
            Log.w("TAG_COLL","没有内容第一次进入");
            command = SystemTool.getAssetsTxt(this, "command.txt");
            spf.edit().putString(Constant.SP_COMMAND_STRING,command).commit();
        }
        String[]  commArrays = command.split("=");
        for (int i=0;i<commArrays.length;i++){
            if (i%2==0){
                commListName.add(commArrays[i]);
                Log.w("TAG_COLL","指令名称 ：" + commArrays[i]);
            }else{
                commListCommand.add(commArrays[i]);
                Log.w("TAG_COLL","指令内容 ：" + commArrays[i]);
            }
        }
        if (commListCommand.size()!=0)
            commNormal = commListCommand.get(0);
        Logger.w(commListName.size() + "---" +  commListCommand.size());
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, commListName);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                arg0.setVisibility(View.VISIBLE);
                String comm;
                if(!isFirstSpinner){
                    isFirstSpinner = false;
                    return;
                }else{
                    if(TextUtils.isEmpty(autoView.getText().toString().trim())){
                        Toast.makeText(MainActivity.this,"请输入IEMI号",Toast.LENGTH_LONG).show();
                        return;
                    }
                    commNormal = commListCommand.get(arg2);
                    comm = String.format(commListCommand.get(arg2),autoView.getText().toString().trim());
                    editCommHex.setText(SystemTool.stringToHexString(comm));
                    Logger.w(SystemTool.stringToAscii(comm)+"\n" + SystemTool.stringToHexString(comm));
                    editComm.setText(comm);
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                arg0.setVisibility(View.VISIBLE);
            }
        });
        /*下拉菜单弹出的内容选项触屏事件处理*/
        spinner.setOnTouchListener(new Spinner.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        /*下拉菜单弹出的内容选项焦点改变事件处理*/
        spinner.setOnFocusChangeListener(new Spinner.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub
            }
        });
        spinner.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return false;
            }
        });

    }

    private void initOhterView() {
        stopDataDisplay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    isStopDisplay = true;
                }else {
                    isStopDisplay = false;
                }
            }
        });

        autoView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                IMEI = charSequence.toString();
                String comm =  String .format(commNormal,charSequence);
                editComm.setText(comm);
                editCommHex.setText(SystemTool.stringToHexString(comm));
                if(isConnect){
                    if (TextUtils.isEmpty(charSequence) ||TextUtils.isEmpty(editComm.getText().toString()) ){
                        btnSend.setEnabled(false);
                    }else {
                        btnSend.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        editComm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                IMEI = charSequence.toString();
                if(isConnect){
                    if (TextUtils.isEmpty(charSequence) ||TextUtils.isEmpty(editComm.getText().toString()) ){
                        btnSend.setEnabled(false);
                    }else {
                        btnSend.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    private void initSp() {
        ip = SPUtils.get(this, Constant.SP_SERVICE_IP, "").toString();
        port = SPUtils.get(this, Constant.SP_SERVICE_PORT, "").toString();
        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("服务IP地址或端口为空，不能进行连接，是否去设置！");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    SettingActivity.startAction(MainActivity.this, REQUEST_CODE);
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            SettingActivity.startAction(this, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.w("onActivityResult : " + requestCode +  "  " + resultCode );
        if (requestCode == REQUEST_CODE ) {
            initSp();
        }else if(requestCode == REQUEST_EDIT_COMMAND_CODE){
            initSpinner();
        }
    }

    @OnClick({R.id.btn_connet, R.id.btn_disconnet, R.id.btn_clear, R.id.btn_send,R.id.btn_edit_comm})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connet:
                Message msg = mHandler.obtainMessage();
                msg.obj = "connect ---->"  + ip + ":" + port ;
                msg.what = RECEIVE_DATA;
                mHandler.sendMessage(msg);// 结果返回给UI处理
                startSocket();
                break;
            case R.id.btn_disconnet:
                stopSocket();
                Message msgDis = mHandler.obtainMessage();
                msgDis.obj = "disconnect ----> "  + ip + ":" + port ;
                msgDis.what = RECEIVE_DATA;
                mHandler.sendMessage(msgDis);// 结果返回给UI处理
                break;
            case R.id.btn_clear:
                tvReceive.setText("");
                tvReceive.scrollTo(0,0);
                break;
            case R.id.btn_send:
                saveAutoHistory(autoView);
                if(socketThread!=null){
                    socketThread.sendBuffer(SystemTool.hexStringToBytes(SystemTool.stringToHexStringNoSpace(editComm.getText().toString().trim())));
                }
                break;
            case R.id.btn_edit_comm:
                EditCommandActivity.startAction(MainActivity.this,REQUEST_EDIT_COMMAND_CODE);
                break;
        }
    }


    private void getCommandBuf(String s){


    }


    @Override
    protected void onStop() {
        super.onStop();
        stopSocket();
    }


    /**----------------------------------- socket ------------------------------------------------------------------------*/

    public class SocThread extends Thread {
        private String TAG = "socket thread";
        private int timeout = 10000;
        public Socket client = null;
        PrintWriter out;
        OutputStream o;
        BufferedReader in;
        public boolean isRun = true;
        Handler mHandler;
        Context mContext;
        private String TAG1 = "AAAAAAAAAAAA";

        public SocThread(Handler handler, Context context) {
            this.mHandler = handler;
            this.mContext = context;
            Log.i(TAG, "创建线程socket");
        }

        /**
         * 连接socket服务器
         */
        public void conn() {
            try {
                Log.i(TAG, "连接中……" +  ip +":" + port);
                client = new Socket(ip, Integer.valueOf(port).intValue());
                client.setSoTimeout(timeout);// 设置阻塞时间
                Logger.i(TAG, "连接成功");
                mHandler.sendEmptyMessage(CONNECT_SUCCESS);
                in = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        client.getOutputStream())), true);
                o = client.getOutputStream();
                Logger.i(TAG, "输入输出流获取成功");
            } catch (UnknownHostException e) {
                Logger.i(TAG, "连接错误UnknownHostException 重新获取");
                e.printStackTrace();
                conn();
            } catch (IOException e) {
                mHandler.sendEmptyMessage(DISCONNECT);
                Logger.i(TAG, "连接服务器io错误");
                e.printStackTrace();
            } catch (Exception e) {
                mHandler.sendEmptyMessage(DISCONNECT);
                Logger.i(TAG, "连接服务器错误Exception" + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 实时接受数据
         */
        @Override
        public void run() {
            conn();
            String line = "";
            while (isRun) {
                try {
                    if (client.isConnected()) {
                        Log.i(TAG, "2.检测数据");
                        while ((line = in.readLine()) != null) {
                            if(!isStopDisplay) {
                                IMEI = autoView.getText().toString();
//                                Logger.e("返回信息 ：" + IMEI + "-----------" + line.toString().contains(IMEI));
                                if(TextUtils.isEmpty(IMEI)){
                                    Message msg = mHandler.obtainMessage();
                                    msg.obj = line;
                                    msg.what = RECEIVE_DATA;
                                    mHandler.sendMessage(msg);// 结果返回给UI处理
                                    Log.w(TAG1, line.toString());

                                }else{
                                    if(line.toString().contains(IMEI)){
                                        Message msg = mHandler.obtainMessage();
                                        msg.obj = line;
                                        msg.what = RECEIVE_DATA;
                                        mHandler.sendMessage(msg);// 结果返回给UI处理
                                        Log.w(TAG1, line.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        Log.i(TAG, "没有可用连接");
                        conn();
                    }
                } catch (Exception e) {
                    Log.i(TAG, "数据接收错误" + e.getMessage());
                    mHandler.sendEmptyMessage(CONNECT_ERROR);
                    e.printStackTrace();
                }
            }
        }

        public void sendString(String mess) {
            try {
                if (client != null) {
                    Logger.i(TAG1, "发送" + mess + "至"
                            + client.getInetAddress().getHostAddress() + ":"
                            + String.valueOf(client.getPort()));
                    out.println(mess);
                    out.flush();
                    Logger.i(TAG1, "发送成功");
                    Message msg = mHandler.obtainMessage();
                    msg.obj = mess;
                    msg.what = SEND_DATA_SUCCESS;
                    mHandler.sendMessage(msg);// 结果返回给UI处理
                } else {
                    Logger.i(TAG, "client 不存在");
                    Message msg = mHandler.obtainMessage();
                    msg.obj = mess;
                    msg.what = SEND_DATA_FAILED;
                    mHandler.sendMessage(msg);// 结果返回给UI处理
                    Logger.i(TAG, "连接不存在重新连接");
                    conn();
                }

            } catch (Exception e) {
                Logger.i(TAG1, "send error");
                e.printStackTrace();
            } finally {
                Logger.i(TAG1, "发送完毕");
            }
        }


        public void sendBuffer(byte[] mess) {
            try {
                if (client != null) {
                    Logger.i(TAG1, "发送" + mess + "至"
                            + client.getInetAddress().getHostAddress() + ":"
                            + String.valueOf(client.getPort()));
                    o.write(mess);
                    o.flush();
//                    out.println(mess.toString());
//                    out.write();
//                    out.flush();
                    Logger.i(TAG1, "发送成功");
                    Message msg = mHandler.obtainMessage();
                    msg.obj = mess;
                    msg.what = SEND_DATA_SUCCESS;
                    mHandler.sendMessage(msg);// 结果返回给UI处理
                } else {
                    Logger.i(TAG, "client 不存在");
                    Message msg = mHandler.obtainMessage();
                    msg.obj = mess;
                    msg.what = SEND_DATA_FAILED;
                    mHandler.sendMessage(msg);// 结果返回给UI处理
                    Logger.i(TAG, "连接不存在重新连接");
                    conn();
                }

            } catch (Exception e) {
                Logger.i(TAG1, "send error");
                e.printStackTrace();
            } finally {
                Logger.i(TAG1, "发送完毕");
            }
        }


        /**
         * 关闭连接
         */
        public void close() {
            try {
                if (client != null) {
                    Log.i(TAG, "close in");
                    in.close();
                    Log.i(TAG, "close out");
                    out.close();
                    Log.i(TAG, "close client");
                    client.close();
                    mHandler.sendEmptyMessage(DISCONNECT);
                }
            } catch (Exception e) {
                Log.i(TAG, "close err");
                e.printStackTrace();
            }
        }
    }


    /**
     * handler 更新ui
     */
    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case RECEIVE_DATA:
                    String str = msg.obj.toString();
                    String time = SystemTool.getCurrentStringTime() + ":  ";
                    tvReceive.append(time + str + "\n" + "\n");
                    int offset=tvReceive.getLineCount()*tvReceive.getLineHeight();
                    if(offset>tvReceive.getHeight()){
                        tvReceive.scrollTo(0,offset-tvReceive.getHeight());
                    }
                    break;
                case SEND_DATA_SUCCESS:
                    Toast.makeText(MainActivity.this,"发送成功",Toast.LENGTH_LONG).show();
                    break;
                case SEND_DATA_FAILED:
                    Toast.makeText(MainActivity.this,"发送失败",Toast.LENGTH_LONG).show();
                    break;
                case CONNECT_SUCCESS:
                    if(autoView.getText().toString().length()>0 && editComm.getText().toString().length()>0){
                        btnSend.setEnabled(true);
                    }
                    isConnect = true;
                    btnConnet.setEnabled(false);
                    btnDisconnet.setEnabled(true);
                    break;
                case DISCONNECT:
                    isConnect = false;
                    btnConnet.setEnabled(true);
                    btnDisconnet.setEnabled(false);
                    btnSend.setEnabled(false);
                    break;
                case CONNECT_ERROR:
//                    tvReceive.append("连接错误，请检查服务器IP或端口。");
                    break;
            }
        }
    };

    public void startSocket() {
        if(socketThread ==null){
            socketThread = new SocThread(mHandler, this);
            socketThread.start();
        }
    }

    private void stopSocket() {
        if(socketThread!=null){
            socketThread.isRun = false;
            socketThread.close();
            socketThread = null;
        }
    }

}
