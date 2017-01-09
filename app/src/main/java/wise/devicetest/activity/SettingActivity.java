package wise.devicetest.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import wise.devicetest.R;
import wise.devicetest.app.Constant;
import wise.devicetest.utils.MyStatusBarUtil;
import wise.devicetest.utils.SPUtils;
import wise.devicetest.utils.SystemTool;

/**
 * 功能： 设置
 * 作者： Administrator
 * 日期： 2016/12/26 17:52
 * 邮箱： descriable
 */
public class SettingActivity extends AppCompatActivity {


    @Bind(R.id.edit_ip)
    TextInputEditText editIp;
    @Bind(R.id.edit_port)
    TextInputEditText editPort;
    @Bind(R.id.tv_version)
    TextView tvVersion;

    private String ip;
    private String port;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        MyStatusBarUtil.setStatusColor(this, getResources().getColor(R.color.colorPrimary));
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        init();
        tvVersion.setText("v:" + SystemTool.getVersion(this)+ " " + "update at 2016-12-27");
    }


    private void init() {
//        ip = editIp.getText().toString().trim();
//        port = editPort.getText().toString().trim();
//        SPUtils.put(this, Constant.SP_SERVICE_IP, ip);
//        SPUtils.put(this, Constant.SP_SERVICE_PORT, port);
        ip = SPUtils.get(this, Constant.SP_SERVICE_IP, "").toString();
        port = SPUtils.get(this, Constant.SP_SERVICE_PORT, "").toString();

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)){
            editPort.setText(getResources().getString(R.string.default_port));
            editIp.setText(getResources().getString(R.string.default_ip));
        }else{
            editIp.setText(ip);
            editPort.setText(port);
        }
    }


    /**
     * @param context
     */
    public static void startAction(Activity context, int requestCode) {
        Intent intent = new Intent(context, SettingActivity.class);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        check();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        this.finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        check();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        this.finish();
    }

    private void check() {
        ip = editIp.getText().toString().trim();
        port = editPort.getText().toString().trim();
        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            SystemTool.showSnackeBar(editPort, "服务器IP或端口不能为空");
            return;
        } else {
            SPUtils.put(this, Constant.SP_SERVICE_IP, ip);
            SPUtils.put(this, Constant.SP_SERVICE_PORT, port);
        }
    }


}
