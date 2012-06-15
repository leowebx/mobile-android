package org.exoplatform.controller.document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.exoplatform.model.ExoFile;
import org.exoplatform.singleton.DocumentHelper;
import org.exoplatform.ui.DocumentActivity;
import org.exoplatform.utils.ExoConnectionUtils;
import org.exoplatform.utils.ExoConstants;
import org.exoplatform.utils.ExoDocumentUtils;
import org.exoplatform.utils.PhotoUtils;
import org.exoplatform.widget.ConnTimeOutDialog;
import org.exoplatform.widget.DocumentWaitingDialog;
import org.exoplatform.widget.WarningDialog;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.view.animation.AnimationUtils;

import com.cyrilmottier.android.greendroid.R;

public class DocumentLoadTask extends AsyncTask<Integer, Void, Integer> {

  // delete file or folder
  protected static final int      ACTION_DELETE  = 1;

  // copy file
  protected static final int      ACTION_COPY    = 2;

  // move file
  protected static final int      ACTION_MOVE    = 3;

  // upload file
  protected static final int      ACTION_UPLOAD  = 4;

  // rename folder
  protected static final int      ACTION_RENAME  = 5;

  // create new folder
  protected static final int      ACTION_CREATE  = 6;

  /*
   * Result status
   */
  private static final int      RESULT_OK      = 7;

  private static final int      RESULT_ERROR   = 8;

  private static final int      RESULT_TIMEOUT = 9;

  private static final int      RESULT_FALSE   = 10;

  private DocumentWaitingDialog _progressDialog;

  private String                loadingData;

  private String                okString;

  private String                titleString;

  /*
   * This @contentWarningString is for display the error/warning message when
   * retrieving document
   */
  private String                contentWarningString;

  private int                   actionID;

  private String                strSourceUrl;

  private String                strDestinationUrl;

  private DocumentActivity      documentActivity;

  private ArrayList<ExoFile>    _documentList;

  private Resources             resource;

  public DocumentLoadTask(DocumentActivity activity, String source, String destination, int action) {
    resource = activity.getResources();
    documentActivity = activity;
    strSourceUrl = source;
    strDestinationUrl = destination;
    actionID = action;
    changeLanguage();
  }

  private void changeLanguage() {
    loadingData = resource.getString(R.string.LoadingData);
    okString = resource.getString(R.string.OK);
    titleString = resource.getString(R.string.Warning);
    contentWarningString = resource.getString(R.string.LoadingDataError);
  }

  @Override
  public void onPreExecute() {
    _progressDialog = new DocumentWaitingDialog(documentActivity, null, loadingData);
    _progressDialog.show();
  }

  @Override
  public Integer doInBackground(Integer... params) {
    boolean result = true;
    _documentList = new ArrayList<ExoFile>();
    try {
      /*
       * Checking the session status each time we retrieve files/folders. If
       * time out, re logging in. If relogging in error, pop up a error dialog
       */
      if (ExoConnectionUtils.getResponseCode(DocumentHelper.getInstance().getRepositoryHomeUrl()) != 1) {
        if (!ExoConnectionUtils.onReLogin()) {
          return RESULT_TIMEOUT;
        }
      }
      if (actionID == ACTION_DELETE) {
        contentWarningString = resource.getString(R.string.DocumentCannotDelete);
        result = ExoDocumentUtils.deleteFile(strSourceUrl);
        strSourceUrl = ExoDocumentUtils.getParentUrl(strSourceUrl);

      } else if (actionID == ACTION_COPY) {
        contentWarningString = resource.getString(R.string.DocumentCopyPasteError);
        result = ExoDocumentUtils.copyFile(strSourceUrl, strDestinationUrl);
        strSourceUrl = ExoDocumentUtils.getParentUrl(strDestinationUrl);

      } else if (actionID == ACTION_MOVE) {
        contentWarningString = resource.getString(R.string.DocumentCopyPasteError);
        result = ExoDocumentUtils.moveFile(strSourceUrl, strDestinationUrl);
        strSourceUrl = ExoDocumentUtils.getParentUrl(strDestinationUrl);

      } else if (actionID == ACTION_UPLOAD) {
        File file = new File(documentActivity._sdcard_temp_dir);
        contentWarningString = resource.getString(R.string.DocumentUploadError);
        File tempFile = PhotoUtils.reziseFileImage(file);
        if (tempFile != null) {
          result = ExoDocumentUtils.putFileToServerFromLocal(strSourceUrl + "/" + file.getName(),
                                                             tempFile,
                                                             ExoConstants.IMAGE_TYPE);
        }

      } else if (actionID == ACTION_RENAME) {
        contentWarningString = resource.getString(R.string.DocumentRenameError);
        result = ExoDocumentUtils.renameFolder(strSourceUrl, strDestinationUrl);
        if (result) {
          boolean isFolder = documentActivity._documentAdapter._documentActionDialog.myFile.isFolder;
          String type = documentActivity._documentAdapter._documentActionDialog.myFile.nodeType;
          documentActivity._fileForCurrentActionBar.isFolder = isFolder;
          documentActivity._fileForCurrentActionBar.nodeType = type;
          strSourceUrl = strDestinationUrl;
          if (!isFolder)
            strSourceUrl = ExoDocumentUtils.getParentUrl(strSourceUrl);
        } else {
          DocumentActivity._documentActivityInstance._fileForCurrentActionBar.currentFolder = strSourceUrl;
          int lastIndex = strSourceUrl.lastIndexOf("/");
          String folderName = strSourceUrl.substring(lastIndex + 1, strSourceUrl.length());
          DocumentActivity._documentActivityInstance._fileForCurrentActionBar.name = folderName;
        }

      } else if (actionID == ACTION_CREATE) {
        contentWarningString = resource.getString(R.string.DocumentCreateFolderError);
        result = ExoDocumentUtils.createFolder(strDestinationUrl);

      }
      /*
       * Get folder content
       */
      
      if (result == true) {
        _documentList = ExoDocumentUtils.getPersonalDriveContent(documentActivity._fileForCurrentActionBar);
        return RESULT_OK;
      } else
        return RESULT_FALSE;

    } catch (IOException e) {
      return RESULT_ERROR;
    }
  }

  @Override
  public void onCancelled() {
    super.onCancelled();
    _progressDialog.dismiss();
  }

  @Override
  public void onPostExecute(Integer result) {
    if (result == RESULT_OK) {

      documentActivity.setDocumentAdapter(_documentList);
      /*
       * Set animation for listview when access to folder
       */
      documentActivity._listViewDocument.setAnimation(AnimationUtils.loadAnimation(documentActivity,
                                                                                   R.anim.anim_right_to_left));

    } else if (result == RESULT_ERROR) {
      new WarningDialog(documentActivity, titleString, contentWarningString, okString).show();
    } else if (result == RESULT_TIMEOUT) {
      new ConnTimeOutDialog(documentActivity, titleString, okString).show();
    } else if (result == RESULT_FALSE) {
      new WarningDialog(documentActivity, titleString, contentWarningString, okString).show();
    }

    _progressDialog.dismiss();

  }

}
