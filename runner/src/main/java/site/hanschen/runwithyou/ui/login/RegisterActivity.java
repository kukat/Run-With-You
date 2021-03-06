package site.hanschen.runwithyou.ui.login;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import site.hanschen.api.user.RegisterReply;
import site.hanschen.api.user.UserCenterApiWrapper;
import site.hanschen.api.user.VerificationReply;
import site.hanschen.common.statusbar.StatusBarCompat;
import site.hanschen.common.utils.ResourceUtils;
import site.hanschen.runwithyou.R;
import site.hanschen.runwithyou.application.RunnerApplication;
import site.hanschen.runwithyou.base.RunnerBaseActivity;
import site.hanschen.runwithyou.utils.CommonUtils;

/**
 * @author HansChen
 */
public class RegisterActivity extends RunnerBaseActivity {

    @BindView(R.id.activity_register_email)
    EditText mEmail;
    @BindView(R.id.activity_register_password)
    EditText mPassword;
    @BindView(R.id.activity_register_password_repeat)
    EditText mPasswordRepeat;
    @BindView(R.id.activity_register_verification_code)
    EditText mVerificationCode;
    @BindView(R.id.activity_register_verification_code_request_btn)
    Button   mRequestCodeBtn;
    @BindView(R.id.activity_register_btn)
    Button   mRegisterBtn;

    private int mCountDown;

    @Inject
    UserCenterApiWrapper mUserCenterApi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        StatusBarCompat.setColor(RegisterActivity.this, ResourceUtils.getColor(mContext, R.color.colorPrimary), 0);
        DaggerRegisterComponent.builder()
                               .applicationComponent(RunnerApplication.getInstance().getAppComponent())
                               .registerModule(new RegisterModule())
                               .build()
                               .inject(RegisterActivity.this);
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountDown();
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.register_toolbar);
        toolbar.setTitle("帐号注册");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        refreshRegisterBtnState();
    }

    @OnTextChanged({
            R.id.activity_register_email,
            R.id.activity_register_verification_code,
            R.id.activity_register_password,
            R.id.activity_register_password_repeat})
    void onTextChanged() {
        refreshRegisterBtnState();
    }

    private void refreshRegisterBtnState() {
        String email = mEmail.getEditableText().toString();
        String verificationCode = mVerificationCode.getEditableText().toString();
        String password = mPassword.getEditableText().toString();
        String passwordRepeat = mPasswordRepeat.getEditableText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(passwordRepeat) || TextUtils.isEmpty(
                verificationCode)) {
            mRegisterBtn.setEnabled(false);
        } else {
            mRegisterBtn.setEnabled(true);
        }
    }

    @OnClick({R.id.activity_register_verification_code_request_btn, R.id.activity_register_btn})
    void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.activity_register_verification_code_request_btn:
                if (!CommonUtils.isEmailValid(mEmail.getEditableText().toString())) {
                    mEmail.setError("邮箱地址不合法，请重新输入");
                } else {
                    requestVerificationCode(mEmail.getEditableText().toString());
                }
                break;
            case R.id.activity_register_btn:
                register();
                break;
            default:
                break;
        }
    }

    private void clearErrorInfo() {
        mEmail.setError(null);
        mVerificationCode.setError(null);
        mPassword.setError(null);
        mPasswordRepeat.setError(null);
    }

    private void register() {
        String email = mEmail.getEditableText().toString();
        String verificationCode = mVerificationCode.getEditableText().toString();
        String password = mPassword.getEditableText().toString();
        String passwordRepeat = mPasswordRepeat.getEditableText().toString();

        clearErrorInfo();
        if (!CommonUtils.isEmailValid(email)) {
            mEmail.setError("邮箱地址不合法，请重新输入");
        } else if (verificationCode.length() != 6) {
            mVerificationCode.setError("请输入6位验证码");
        } else if (!CommonUtils.isPasswordValid(password)) {
            mPassword.setError("请输入8-20位密码，必须包含字母和数字");
        } else if (!CommonUtils.isPasswordValid(passwordRepeat)) {
            mPasswordRepeat.setError("请输入8-20位密码，必须包含字母和数字");
        } else if (!password.equals(passwordRepeat)) {
            mPasswordRepeat.setError("前后两次输入密码不一致");
        } else {
            doRegister(email, verificationCode, password);
        }
    }

    private Runnable mRefreshTask = new Runnable() {
        @Override
        public void run() {
            if (mCountDown > 0) {
                mRequestCodeBtn.setText("重新获取" + mCountDown + "秒");
                mRequestCodeBtn.setEnabled(false);
                mCountDown--;
                getMainHandler().postDelayed(mRefreshTask, 1000);
            } else {
                mRequestCodeBtn.setText("获取验证码");
                mRequestCodeBtn.setEnabled(true);
            }
        }
    };

    private void startCountDown() {
        mCountDown = 30;
        getMainHandler().post(mRefreshTask);
    }

    private void stopCountDown() {
        getMainHandler().removeCallbacksAndMessages(null);
    }

    private void requestVerificationCode(final String email) {
        mUserCenterApi.requestVerificationCode(email).subscribe(new Observer<VerificationReply>() {
            @Override
            public void onSubscribe(Disposable d) {
                showWaitingDialog();
            }

            @Override
            public void onNext(VerificationReply reply) {
                dismissWaitingDialog();
                if (reply.getSucceed()) {
                    startCountDown();
                    toast("验证码已发送到您的邮箱");
                } else {
                    switch (reply.getErrCode()) {
                        case EMAIL_ALREADY_REGISTERED:
                            mEmail.setError("邮箱已注册，请换个邮箱试试");
                            break;
                        case EMAIL_INVALID:
                            mEmail.setError("无效邮箱，请换个邮箱试试");
                            break;
                        case UNKNOWN:
                        default:
                            toast("请求失败，请重新获取验证码");
                            break;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                dismissWaitingDialog();
                toast("网络错误:" + e.toString());
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private void doRegister(final String email, final String verificationCode, final String password) {
        mUserCenterApi.register(email, verificationCode, password).subscribe(new Observer<RegisterReply>() {
            @Override
            public void onSubscribe(Disposable d) {
                showWaitingDialog();
            }

            @Override
            public void onNext(RegisterReply reply) {
                dismissWaitingDialog();
                if (reply.getSucceed()) {
                    new MaterialDialog.Builder(mContext).title("注册成功")
                                                        .content("注册成功，欢迎您使用")
                                                        .positiveText("马上登录")
                                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                            @Override
                                                            public void onClick(@NonNull MaterialDialog dialog,
                                                                                @NonNull DialogAction which) {
                                                                finish();
                                                            }
                                                        })
                                                        .build()
                                                        .show();
                } else {
                    switch (reply.getErrCode()) {
                        case EMAIL_INVALID:
                            mEmail.setError("无效邮箱，请换个邮箱试试");
                            break;
                        case PASSWORD_INVALID:
                            mPassword.setError("请输入8-20位密码，必须包含字母和数字");
                            break;
                        case EMAIL_ALREADY_REGISTERED:
                            mEmail.setError("邮箱已注册，请换个邮箱试试");
                            break;
                        case VERIFICATION_CODE_ERROR:
                            mVerificationCode.setError("验证码错误");
                            break;
                        case UNKNOWN:
                        default:
                            toast("注册失败，请重试~");
                            break;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                dismissWaitingDialog();
                toast("网络错误:" + e.toString());
            }

            @Override
            public void onComplete() {
                dismissWaitingDialog();
            }
        });
    }
}
