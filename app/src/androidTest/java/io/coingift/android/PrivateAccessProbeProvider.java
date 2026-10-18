package io.coingift.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Test-process bridge: returns counters/booleans only, never captured content. Absent from main APK. */
public final class PrivateAccessProbeProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if("reset".equals(method))PrivateAccessService.reset();
        return PrivateAccessService.snapshot();
    }
    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String order){return null;}
    @Override public String getType(Uri uri){return null;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}
