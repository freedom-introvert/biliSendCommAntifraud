package icu.freedomIntrovert.biliSendCommAntifraud.account;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.Nav;
import icu.freedomIntrovert.biliSendCommAntifraud.db.AccountOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

public class AccountManger {
    private static AccountManger instance;
    private final AccountOpenHelper db;
    private final Map<Long, Account> accountMap;
    public final File avatarDir;
    private AccountManger(Context context) {
        avatarDir = new File(context.getFilesDir(),"account_avatars");
        db = new AccountOpenHelper(context);
        accountMap = new HashMap<>();
        List<Account> accounts = db.getAllAccounts();
        for (Account account : accounts) {
            accountMap.put(account.uid, account);
        }
    }

    public synchronized static AccountManger getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManger(context.getApplicationContext());
        }
        return instance;
    }

    public Account getAccount(long uid) {
        return accountMap.get(uid);
    }

    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        for (Map.Entry<Long, Account> longAccountEntry : accountMap.entrySet()) {
            accounts.add(longAccountEntry.getValue());
        }
        return accounts;
    }

    public void addOrUpdateAccount(Account account) {
        accountMap.put(account.uid, account);
        db.insertOrUpdateAccount(account);
    }

    public void deleteAccount(Account account) {
        accountMap.remove(account.uid);
        db.deleteAccount(account);
    }

    public boolean accountExists(long uid) {
        return accountMap.containsKey(uid);
    }

    public static Account cookieToAccount(String cookie) throws IOException {
        BiliApiService biliApiService = ServiceGenerator.getBiliApiService();
        Nav nav = biliApiService.getNav(cookie).data();
        if (nav.isLogin) {
            Account account = new Account();
            account.uid = nav.mid;
            account.uname = nav.uname;
            account.cookie = cookie;
            //获取头像
            OkHttpClient httpClient = OkHttpUtil.getHttpClient();
            Request request = new Request.Builder()
                    .url(nav.face+"@240w_240h_1c.jpg")
                    .build();
            ResponseBody body = httpClient.newCall(request).execute().body();
            OkHttpUtil.respNotNull(body);
            account.setAvatar(body.bytes());
            return account;
        } else {
            return null;
        }
    }

    public static boolean checkCookieNotFailed(Account account) throws IOException {
        BiliApiService biliApiService = ServiceGenerator.getBiliApiService();
        return biliApiService.getNav(account.cookie).data().isLogin;
    }

    @Nullable
    public Account random(){
        List<Account> accounts = getAccounts();
        int size = accounts.size();
        if (size == 0){
            return null;
        }
        return accounts.get(generateRandomInt(0,size-1));
    }

    private static int generateRandomInt(int min, int max) {
        Random random = new Random();
        // 生成 [min, max] 范围内的随机整数
        return random.nextInt(max - min + 1) + min;
    }
}
