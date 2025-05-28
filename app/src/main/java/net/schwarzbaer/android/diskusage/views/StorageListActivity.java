package net.schwarzbaer.android.diskusage.views;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.schwarzbaer.android.diskusage.R;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.util.Locale;

public class StorageListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_storage_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView txtStorageListOutput = findViewById(R.id.txtStorageListOutput);

        RecyclerView recyclerView = findViewById(R.id.listStorages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Storage[] storages = FileSystemScanner.getInstance().getStorages();
        if (storages == null) {
            txtStorageListOutput.setText("No Storages found");
        } else {
            txtStorageListOutput.setText(String.format(Locale.ENGLISH, "%d Storages found", storages.length));
            recyclerView.setAdapter( new MyAdapter(this, storages) );
        }
    }

    public void clickBackBtn(View view) {
        finish();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public final TextView txtTitle;
        public final TextView txtComment;
        public final TextView txtMarker;

        public MyViewHolder(View itemView) {
            super(itemView);
            txtTitle   = itemView.findViewById(R.id.txtStorageItemTitle);
            txtComment = itemView.findViewById(R.id.txtStorageItemComment);
            txtMarker  = itemView.findViewById(R.id.txtStorageItemMarker);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final Context context;
        private final Storage[] storages;

        public MyAdapter(Context context, Storage[] storages) {
            this.context = context;
            this.storages = storages;
        }

        @Override
        public int getItemCount() {
            return storages.length;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_storage_list, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Storage storage = storages[position];
            holder.txtTitle.setText(storage.getPath());
            holder.txtComment.setText(storage.getComment());
            if (!storage.hasFiles())
                holder.txtMarker.setText(" X ");
            else {
                holder.txtMarker.setText(" >>");
                holder.itemView.setOnClickListener(view -> {
                    // TODO: StorageViewActivity
//                    Intent intent = new Intent(context, FileListActivity.class);
//                    intent.putExtra(FileListActivity.activityParam_Path, String.format("%s -> %s", path, item));
//                    context.startActivity(intent);
                });
            }
        }
    }
}