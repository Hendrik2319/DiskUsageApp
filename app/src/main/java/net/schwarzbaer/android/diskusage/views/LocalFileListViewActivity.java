package net.schwarzbaer.android.diskusage.views;

import android.content.Context;
import android.content.Intent;
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
import net.schwarzbaer.android.diskusage.databinding.ActivityLocalFileListViewBinding;
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.io.File;

public class LocalFileListViewActivity extends AppCompatActivity
{

    public static final String activityParam_StorageIndex = "StorageIndex";
    public static final String activityParam_FileCategory = "FileCategory";
    public static final String activityParam_FolderID = "FolderID";

    private ActivityLocalFileListViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalFileListViewBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        final int storageIndex = intent.getIntExtra(activityParam_StorageIndex, 0);
        final FileCategory fileCat = FileCategory.valueOf_checked(intent.getStringExtra(activityParam_FileCategory));
        final int folderID = intent.getIntExtra(activityParam_FolderID, Storage.FolderID_Root);

        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);
        Storage.ScannedFolder scannedFolder =
                storage==null || fileCat==null
                    ? null
                    : folderID == Storage.FolderID_Root
                        ? storage.getRootFolder(fileCat)
                        : storage.getFolder(folderID);

        binding.txtViewOutput.setText(String.format("Folder: %s", scannedFolder == null ? "<no folder>" : scannedFolder.getPath()));

        binding.listFiles.setLayoutManager(new LinearLayoutManager(this));
        if (scannedFolder != null)
            binding.listFiles.setAdapter(new MyAdapter(this, storageIndex, fileCat, scannedFolder));

    }

    public void clickBackBtn(View view) {
        finish();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView txtName;
        private final TextView txtSize;

        private MyViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtFileName);
            txtSize = itemView.findViewById(R.id.txtFileSize);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyViewHolder>
    {
        private final Context context;
        private final int storageIndex;
        @NonNull
        private final FileCategory fileCat;
        @NonNull
        private final Storage.ScannedFolder scannedFolder;

        private MyAdapter(Context context, int storageIndex, @NonNull FileCategory fileCat, @NonNull Storage.ScannedFolder scannedFolder)
        {
            this.context = context;
            this.storageIndex = storageIndex;
            this.fileCat = fileCat;
            this.scannedFolder = scannedFolder;
        }

        @Override
        public int getItemCount()
        {
            return scannedFolder.getLocalFileCount();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_local_file_list_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position)
        {
            String strName, strSize;

            File localFile = scannedFolder.getLocalFile(position);

            if (localFile!=null)
            {
                long fileSize_Byte = localFile.length();
                strSize = fileSize_Byte == 0 ? "----" : Storage.getFormatedSize(fileSize_Byte);
                strName = localFile.getName();
            }
            else
            {
                strSize = "----";
                strName = "<No File>";
            }

            holder.txtName.setText(strName);
            holder.txtSize.setText(strSize);
        }
    }
}