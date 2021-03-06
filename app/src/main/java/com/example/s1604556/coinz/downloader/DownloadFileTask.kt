package com.example.s1604556.coinz.downloader

import android.os.AsyncTask
import com.example.s1604556.coinz.downloader.DownloadCompleteRunner.result
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

//taken directly from lecture note, no modification made
class DownloadFileTask(private val caller : DownloadCompleteListener):
        AsyncTask<String, Void, String>(){

    override fun doInBackground(vararg urls: String): String = try{
        loadFileFromNetwork(urls[0])

    }catch(e: IOException){
        "unable to load content. Check your network connection"
    }

    private fun loadFileFromNetwork(urlString: String): String{
        val stream : InputStream = downloadUrl(urlString)
        result= stream.bufferedReader().use { it.readText() }
        return result
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream{
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: String){
        super.onPostExecute(result)

        caller.downloadComplete(result)
    }

}