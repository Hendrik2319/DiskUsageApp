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
import net.schwarzbaer.android.diskusage.models.FileCategory;
import net.schwarzbaer.android.diskusage.models.FileSystemScanner;
import net.schwarzbaer.android.diskusage.models.Storage;

import java.util.Locale;

public class FolderViewActivity extends AppCompatActivity
{
    public static String activityParam_StorageIndex = "StorageIndex";
    public static String activityParam_FileCategory = "FileCategory";
    public static String activityParam_FolderID = "FolderID";
    public static int activityParamValue_FolderID_Root = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_folder_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final int storageIndex = getIntent().getIntExtra(activityParam_StorageIndex, 0);
        final FileCategory fileCat = FileCategory.valueOf_checked(getIntent().getStringExtra(activityParam_FileCategory));
        final int folderID = getIntent().getIntExtra(activityParam_FolderID, activityParamValue_FolderID_Root);

        Storage storage = FileSystemScanner.getInstance().getStorage(storageIndex);
        Storage.ScannedFolder scannedFolder =
                storage==null || fileCat==null
                    ? null
                    : folderID == activityParamValue_FolderID_Root
                        ? storage.getRootFolder(fileCat)
                        : storage.getFolder(fileCat,folderID);

        TextView txtFolderViewOutput = findViewById(R.id.txtFolderViewOutput);
        txtFolderViewOutput.setText(String.format("Folder: %s", scannedFolder == null ? "<no folder>" : scannedFolder.getPath()));

        RecyclerView recyclerView = findViewById(R.id.listFolders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (scannedFolder != null)
            recyclerView.setAdapter(new MyAdapter(this, storageIndex, fileCat, scannedFolder));
    }

    public void clickBackBtn(View view) {
        finish();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView txtTitle;
        private final TextView txtFiles;
        private final TextView txtMarker;
        private final TextView txtSize;

        private MyViewHolder(View itemView) {
            super(itemView);
            txtTitle  = itemView.findViewById(R.id.txtFolderTitle);
            txtFiles  = itemView.findViewById(R.id.txtFolderFiles);
            txtSize   = itemView.findViewById(R.id.txtFolderSize);
            txtMarker = itemView.findViewById(R.id.txtFolderMarker);
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
            int fileCount = scannedFolder.getLocalFileCount();
            int folderCount = scannedFolder.getLocalFolderCount();
            return folderCount + (fileCount==0 ? 0 : 1);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position)
        {
            final String strTitle, strFiles, strSize;
            final View.OnClickListener onClickListener;

            int localFileCount = scannedFolder.getLocalFileCount();
            if (localFileCount!=0 && position==0)
            {
                strTitle = "[ Local Files ]";
                strFiles = localFileCount == 1 ? "1 file" : String.format(Locale.ENGLISH, "%d files", localFileCount);
                strSize = Storage.getFormatedSize(scannedFolder.getLocalFilesSize_Byte());
                onClickListener = view -> {
                    // TODO: LocalFileListViewActivity
//                    Intent intent = new Intent(context, FolderViewActivity.class);
//                    intent.putExtra(FolderViewActivity.activityParam_StorageIndex, storageIndex);
//                    intent.putExtra(FolderViewActivity.activityParam_FileCategory, fileCat.name());
//                    intent.putExtra(FolderViewActivity.activityParam_FolderID, FolderViewActivity.activityParamValue_FolderID_Root);
//                    context.startActivity(intent);
                };
            }
            else
            {
                int folderIndex = position - (localFileCount!=0 ? 1 : 0);
                Storage.ScannedFolder scannedSubFolder = scannedFolder.getSubFolder(folderIndex);
                if (scannedSubFolder != null) {
                    long fileCount     = scannedSubFolder.getTotalFileCount();
                    long fileSize_Byte = scannedSubFolder.getTotalSize_Byte();
                    int folderID       = scannedSubFolder.getFolderID();
                    strTitle = scannedSubFolder.getName();
                    strFiles = fileCount == 0 ? "no files" : fileCount == 1 ? "1 file" : String.format(Locale.ENGLISH, "%d files", fileCount);
                    strSize = fileSize_Byte == 0 ? "----" : Storage.getFormatedSize(fileSize_Byte);
                    onClickListener = view -> {
                        Intent intent = new Intent(context, FolderViewActivity.class);
                        intent.putExtra(FolderViewActivity.activityParam_StorageIndex, storageIndex);
                        intent.putExtra(FolderViewActivity.activityParam_FileCategory, fileCat.name());
                        intent.putExtra(FolderViewActivity.activityParam_FolderID, folderID);
                        context.startActivity(intent);
                    };
                }
                else
                {
                    strTitle = "<No Folder>";
                    strFiles = "<No Files>";
                    strSize = "----";
                    onClickListener = null;
                }
            }

            holder.txtTitle.setText(strTitle);
            holder.txtFiles.setText(strFiles);
            holder.txtSize.setText(strSize);

            if (onClickListener == null)
                holder.txtMarker.setText("X");
            else
            {
                holder.txtMarker.setText(">>");
                holder.itemView.setOnClickListener(onClickListener);
            }
        }
    }
}