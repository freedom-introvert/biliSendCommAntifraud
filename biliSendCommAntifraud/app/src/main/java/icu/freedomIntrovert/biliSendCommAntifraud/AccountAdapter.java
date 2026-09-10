package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
    private final AccountManger accountManger;

    private OnItemClickListener listener;
    private OnItemLongClickListener longListener;

    public final Context context;
    public List<Account> accounts;
    public long disableUid = 0;
    public AccountAdapter(Context context) {
        this.context = context;
        this.accountManger = AccountManger.getInstance(context);
        accounts = accountManger.getAccounts();
    }

    public AccountAdapter(Context context,long disableUid){
        this(context);
        this.disableUid = disableUid;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == getItemCount()-1){
            holder.itemView.findViewById(R.id.line).setVisibility(View.INVISIBLE);
        } else {
            holder.itemView.findViewById(R.id.line).setVisibility(View.VISIBLE);
        }
        Account account = accounts.get(position);
        holder.uname.setText(account.uname);
        holder.uidInfo.setText(String.format("uid:%s", account.uid));
        System.out.println(account.avatarBitmap);
        holder.avatar.setImageBitmap(account.avatarBitmap);
        if (account.accountCommentArea != null){
            holder.deputyInfo.setText(String.format("用户评论区:%s",account.accountCommentArea.sourceId));
        } else {
            holder.deputyInfo.setText("用户评论区:（未设置）");
        }
        if (account.uid == disableUid) {
            holder.itemView.setClickable(false);
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setClickable(true);
            holder.itemView.setAlpha(1f);
            holder.itemView.setOnClickListener(v ->{
                if (listener != null){
                    listener.onItemClick(account);
                }
            });
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null){
                longListener.onItemLongClick(account);
                return true;
            }
            return false;
        });
    }

    public void setItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public void setItemLongClickListener(OnItemLongClickListener listener) {
        this.longListener = listener;
    }
    /**
     *  添加账号
     * @return 添加是否成功，如果已存在返回false
     */
    public boolean addAccount(Account account){
        if (accountManger.getAccount(account.uid) != null) {
            return false;
        }
        accountManger.addOrUpdateAccount(account);
        accounts.add(account);
        notifyItemChanged(getItemCount());
        return true;
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void removeAccount(Account account) {
        int i = accounts.indexOf(account);
        accountManger.deleteAccount(account);
        accounts.remove(i);
        notifyItemRemoved(i);
    }

    public void updateAccount(Account oldAccount,Account newAccount){
        int i = accounts.indexOf(oldAccount);
        oldAccount.update(newAccount);
        oldAccount.accountCommentArea = newAccount.accountCommentArea;
        accountManger.addOrUpdateAccount(oldAccount);
        notifyItemChanged(i);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final TextView uname;
        public final TextView uidInfo;
        public final TextView deputyInfo;
        public final ImageView avatar;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            uname = itemView.findViewById(R.id.uname);
            uidInfo = itemView.findViewById(R.id.uidInfo);
            deputyInfo = itemView.findViewById(R.id.deputyInfo);
            avatar = itemView.findViewById(R.id.img_avatar);
        }
    }

    public interface OnItemClickListener{
        void onItemClick(Account account);
    }

    public interface OnItemLongClickListener{
        void onItemLongClick(Account account);
    }
}
