package com.neday.bomb.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.neday.bomb.R;
import com.neday.bomb.base.BaseOnlineActivity;
import com.neday.bomb.entity.User;
import com.neday.bomb.network.RxFactory;
import com.neday.bomb.util.CommonUtils;
import com.neday.bomb.view.loading.CatLoadingView;
import com.orhanobut.logger.Logger;


/**
 * 会员中心页
 *
 * @author nEdAy
 */
public class VipActivity extends BaseOnlineActivity {
    private final static String TAG = "UpdateNewPasswordActivity";
    private CatLoadingView catLoadingView;
    private TextView tv_nickname;
    private TextView tv_credit;
    private SimpleDraweeView riv_avatar;
    private ImageView iv_vip;

    @Override
    public int bindLayout() {
        return R.layout.activity_vip;
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        setTintManager();
        initTopBarForBoth("会员中心", getString(R.string.tx_back), "等级说明", () ->
                getOperation().startActivity(VipHelpActivity.class)
        );
        catLoadingView = new CatLoadingView();
        iv_vip = (ImageView) findViewById(R.id.iv_vip);
        riv_avatar = (SimpleDraweeView) findViewById(R.id.riv_avatar);
        tv_nickname = (TextView) findViewById(R.id.tv_nickname);
        tv_credit = (TextView) findViewById(R.id.tv_credit);
        findViewById(R.id.tv_sign_in).setOnClickListener(v -> getOperation().startActivity(SignInActivity.class));
        findViewById(R.id.rl_privilege).setOnClickListener(v -> CommonUtils.showToast("尚未开放，敬请期待"));
        findViewById(R.id.rl_achievement).setOnClickListener(v -> CommonUtils.showToast("尚未开放，敬请期待"));
    }

    @Override
    public void onResumeAfter() {
        findViewById(R.id.tv_integration_shop).setOnClickListener(v -> showIntegrationShop());
        getCurrentUserInfo();
    }

    /**
     * 获取当前用户全信息
     */
    private void getCurrentUserInfo() {
        toSubscribe(RxFactory.getUserServiceInstance(null)
                        .getUser(currentUser.getObjectId()),
                () ->
                        catLoadingView.show(getSupportFragmentManager(), TAG),
                user -> {
                    catLoadingView.dismissAllowingStateLoss();
                    updateUser(user);
                },
                throwable -> {
                    catLoadingView.dismissAllowingStateLoss();
                    CommonUtils.showToast("加载用户信息失败");
                    Logger.e(throwable.getMessage());
                });
    }

    /**
     * 更新用户信息前端显示
     *
     * @param user 用户信息
     */
    private void updateUser(User user) {
        String nickname = user.getNickname();
        if (TextUtils.isEmpty(nickname)) {
            tv_nickname.setOnClickListener(view -> getOperation().startActivity(UpdateInfoActivity.class));
        } else {
            tv_nickname.setText(nickname);
        }
        refreshAvatar(user.getAvatar());
        riv_avatar.setOnClickListener(v -> getOperation().startActivity(AccountActivity.class));
        int credit = user.getCredit();
        int creditSub;
        if (credit >= 200000) {
            iv_vip.setImageResource(R.drawable.level_10);
            creditSub = 0;
        } else if (credit >= 100000) {
            iv_vip.setImageResource(R.drawable.level_9);
            creditSub = 200000 - credit;
        } else if (credit >= 50000) {
            iv_vip.setImageResource(R.drawable.level_8);
            creditSub = 100000 - credit;
        } else if (credit >= 15000) {
            iv_vip.setImageResource(R.drawable.level_7);
            creditSub = 50000 - credit;
        } else if (credit >= 5000) {
            iv_vip.setImageResource(R.drawable.level_6);
            creditSub = 15000 - credit;
        } else if (credit >= 2000) {
            iv_vip.setImageResource(R.drawable.level_5);
            creditSub = 5000 - credit;
        } else if (credit >= 1000) {
            iv_vip.setImageResource(R.drawable.level_4);
            creditSub = 2000 - credit;
        } else if (credit >= 500) {
            iv_vip.setImageResource(R.drawable.level_3);
            creditSub = 1000 - credit;
        } else if (credit >= 200) {
            iv_vip.setImageResource(R.drawable.level_2);
            creditSub = 500 - credit;
        } else if (credit >= 100) {
            iv_vip.setImageResource(R.drawable.level_1);
            creditSub = 200 - credit;
        } else {
            iv_vip.setImageResource(R.drawable.level_0);
            creditSub = 100 - credit;
        }
        tv_credit.setText(String.format(getResources().getString(R.string.tx_credit_vip_up), credit, creditSub));
    }


    /**
     * 更新头像 refreshAvatar
     */
    private void refreshAvatar(String avatar) {
        if (avatar != null && !avatar.equals("")) {
            Uri uri = Uri.parse(avatar);
            riv_avatar.setImageURI(uri);
        } else {
            riv_avatar.setImageResource(R.drawable.avatar_default);
        }
    }


    /**
     * 查询当前用户积分用以构造积分商城免登录url并跳转
     */
    private void showIntegrationShop() {
        String objectId = currentUser.getObjectId();
        toSubscribe(RxFactory.getUserServiceInstance(null)//查询当前用户积分
                        .getUser(objectId, "_User[credit]")
                        .map(User::getCredit),
                () ->
                        catLoadingView.show(getSupportFragmentManager(), TAG)
                ,
                credit ->
                        getAutoLoginUrl(objectId, credit)
                ,
                throwable -> {
                    catLoadingView.dismissAllowingStateLoss();
                    CommonUtils.showToast("获取用户口袋币失败");
                    Logger.e(throwable.getMessage());
                });
    }

    /**
     * 访问服务器获取真正的积分商城免登陆URL
     *
     * @param objectId 用户ID
     * @param credits  积分
     */
    private void getAutoLoginUrl(String objectId, Integer credits) {
        toSubscribe(RxFactory.getPublicServiceInstance(null)//构造免登陆Url
                        .getAutoLoginUrl(objectId, credits),
                url -> {
                    catLoadingView.dismissAllowingStateLoss();
                    Intent intent = new Intent();
                    intent.setClass(mContext, WebShopActivity.class);
                    intent.putExtra("url", url); //配置自动登陆地址，每次需根据服务端时间动态生成。
                    startActivity(intent);
                },
                throwable -> {
                    catLoadingView.dismissAllowingStateLoss();
                    CommonUtils.showToast("免登录url获取成功失败");
                    Logger.e(throwable.getMessage());
                });
    }
}
