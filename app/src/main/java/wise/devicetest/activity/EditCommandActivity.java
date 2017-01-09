package wise.devicetest.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import wise.devicetest.R;
import wise.devicetest.app.Constant;
import wise.devicetest.utils.MyStatusBarUtil;
import wise.devicetest.utils.SystemTool;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2016/12/29 17:15
 * 邮箱： descriable
 */
public class EditCommandActivity extends AppCompatActivity {


    @Bind(R.id.edit_comm)
    EditText editComm;
    @Bind(R.id.tv_msg)
    TextView tvMsg;

    SharedPreferences sp;

    private List<String> commandListList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_edit);
        ButterKnife.bind(this);
        MyStatusBarUtil.setStatusColor(this, getResources().getColor(R.color.colorPrimary));
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        initText();
    }

    private String command;
    private void initText() {
        String str="指令编辑如：<font color='#FF0000'>指令名称=指令=</font> （%s代表设备IMEI号）";
        tvMsg.setText(Html.fromHtml(str));
        sp = getSharedPreferences(Constant.SP_DATA, 0);
        command = sp.getString(Constant.SP_COMMAND_STRING,"");
        if(TextUtils.isEmpty(command)){
            Log.w("TAG_COLL","没有内容第一次进入");
            command = SystemTool.getAssetsTxt(this, "command.txt");
            sp.edit().putString(Constant.SP_COMMAND_STRING,command).commit();
        }
        editComm.setText(command);
    }


    /**
     * @param context
     */
    public static void startAction(Activity context, int requestCode) {
        Intent intent = new Intent(context, EditCommandActivity.class);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Logger.w("返回：" + editComm.getText().toString());
        sp.edit().putString(Constant.SP_COMMAND_STRING,editComm.getText().toString()).commit();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        this.finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Logger.w("返回：" + editComm.getText().toString());
        sp.edit().putString(Constant.SP_COMMAND_STRING,editComm.getText().toString()).commit();
    }
}
