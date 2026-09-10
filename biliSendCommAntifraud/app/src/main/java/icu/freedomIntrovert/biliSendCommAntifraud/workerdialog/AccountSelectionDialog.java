package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import icu.freedomIntrovert.biliSendCommAntifraud.AccountAdapter;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;

public class AccountSelectionDialog {

    public static void show(Context context,@Nullable String title,Long disableUid,CallBack callBack){
        View dialogView = View.inflate(context,R.layout.dialog_accounts,null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_accounts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        AccountAdapter accountAdapter = disableUid == null ?
                new AccountAdapter(context) : new AccountAdapter(context,disableUid);
        recyclerView.setAdapter(accountAdapter);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(title == null ? "选择账号" : title)
                .setPositiveButton(R.string.cancel, null)
                .show();
        accountAdapter.setItemClickListener(account -> {
            dialog.dismiss();
            callBack.onSelected(account);
        });
    }
    public interface CallBack{
        void onSelected(Account account);
    }
}
