package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.net.URISyntaxException;

/**
 * Created by lawren on 02/11/17.
 */

public class PhotoPageFragment extends VisibleFragment {
    private static final String ARG_URI = "photo_page_url";
    private static final String TAG = "PhotoPageFragment";

    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;

    public static PhotoPageFragment newInstance(Uri uri){
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);

        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mUri = getArguments().getParcelable(ARG_URI);
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);

        mWebView = (WebView) v.findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient(){
            public void onProgressChanged(WebView webView, int newProgress){
                if(newProgress == 100){
                    mProgressBar.setVisibility(View.GONE);
                }else{
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            public void onReceivedTitle(WebView webView, String title){
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });

        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
               if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                   if (url.startsWith("http://") || url.startsWith("https://")) {
                       return false;
                   } else if(url.startsWith("intent://")){
                      try{
                          Intent intent = new Intent().parseUri(url, Intent.URI_INTENT_SCHEME);
                          startActivity(intent);
                      }catch(URISyntaxException e){
                          Log.e(TAG, "Can't resolve intent: " + e);
                          return true;
                      }
                       return true;
                   }
                   return true;
               }
               return false;
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    String requestUrl = request.getUrl().toString();
                    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")){
                        return false;
                    }else if(requestUrl.startsWith("intent://")){
                        try {
                            Intent intent = new Intent().parseUri(requestUrl, Intent.URI_INTENT_SCHEME);
                            startActivity(intent);
                        }catch(URISyntaxException e){
                            Log.e(TAG, "Can't resolve intent: " + e);
                            return true;
                        }
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });
        mWebView.loadUrl(mUri.toString());

        return v;
    }

    public boolean onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }
}
