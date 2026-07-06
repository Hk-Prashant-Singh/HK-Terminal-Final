package com.hk.hkterminal;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * HK-OPERATION : ALPHA STORAGE BRIDGE
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : System-Level Binding for Internal Matrix Access
 */
public class HKStorageProvider extends DocumentsProvider {
    
    private static final String AUTHORITY = "com.hk.hkterminal.documents";
    private static final String ROOT_ID = "root";
    
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : new String[]{
                Root.COLUMN_ROOT_ID,
                Root.COLUMN_MIME_TYPES,
                Root.COLUMN_TITLE,
                Root.COLUMN_SUMMARY,
                Root.COLUMN_DOCUMENT_ID,
                Root.COLUMN_ICON,
                Root.COLUMN_FLAGS
        });
        
        cursor.newRow()
                .add(Root.COLUMN_ROOT_ID, ROOT_ID)
                .add(Root.COLUMN_MIME_TYPES, Document.MIME_TYPE_DIR)
                .add(Root.COLUMN_TITLE, "HK Terminal Home")
                .add(Root.COLUMN_SUMMARY, "Alpha Matrix Storage")
                .add(Root.COLUMN_DOCUMENT_ID, TerminalEngine.HOME_PATH)
                .add(Root.COLUMN_ICON, android.R.drawable.ic_lock_idle_lock)
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_RECENTS);
                
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : new String[]{
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPES,
                Document.COLUMN_SIZE,
                Document.COLUMN_LAST_MODIFIED,
                Document.COLUMN_FLAGS
        });
        
        File file = new File(documentId);
        if (file.exists()) {
            includeFile(cursor, file);
        }
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(projection != null ? projection : new String[]{
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPES,
                Document.COLUMN_SIZE,
                Document.COLUMN_LAST_MODIFIED,
                Document.COLUMN_FLAGS
        });
        
        File parent = new File(parentDocumentId);
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                includeFile(cursor, file);
            }
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        File file = new File(documentId);
        int accessMode = ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.contains("w")) {
            accessMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        }
        return ParcelFileDescriptor.open(file, accessMode);
    }

    private void includeFile(MatrixCursor cursor, File file) {
        int flags = 0;
        if (file.canWrite()) {
            flags |= Document.FLAG_SUPPORTS_WRITE;
            flags |= Document.FLAG_SUPPORTS_DELETE;
        }
        
        String mimeType = Document.MIME_TYPE_DIR;
        if (!file.isDirectory()) {
            int lastDot = file.getName().lastIndexOf('.');
            if (lastDot >= 0) {
                String extension = file.getName().substring(lastDot + 1);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            if (mimeType == null) mimeType = "application/octet-stream";
        }

        cursor.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, file.getAbsolutePath())
                .add(Document.COLUMN_DISPLAY_NAME, file.getName())
                .add(Document.COLUMN_MIME_TYPES, mimeType)
                .add(Document.COLUMN_SIZE, file.length())
                .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
                .add(Document.COLUMN_FLAGS, flags);
    }
}
