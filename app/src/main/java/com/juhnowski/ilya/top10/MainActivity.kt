package com.juhnowski.ilya.top10

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.properties.Delegates


class FeedEntry{
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String =""

    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private var downloadData: DownloadData? = null //by lazy { DownloadData(this, xmlListView) }

    private var feedUrl: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadUrl(feedUrl.format(feedLimit))
//        Log.d(TAG,"onCreate called")
//        downloadData.execute()
        Log.d(TAG, "onCreate: done")
    }

    private fun downloadUrl(feedUrl:String) {
        Log.d(TAG,"downloadUrl starting AsyncTask")
        downloadData = DownloadData(this,xmlListView)
        downloadData?.execute(feedUrl)
        Log.d(TAG, "downloadUrl: done")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu,menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.mnuTop10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnuTop25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            R.id.mnuFree ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnuTop10, R.id.mnuTop25 ->{
                if(!item.isChecked){
                    item.isChecked = true;
                    feedLimit = 35 - feedLimit
                    Log.d(TAG,"onOptionsItemSelected: ${item.title} settings feedLimit to $feedLimit")
                } else {
                    Log.d(TAG,"onOptionsItemSelected: ${item.title} settings unchanged")
                }
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

       // downloadUrl(feedUrl)
        downloadUrl(feedUrl.format(feedLimit))
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView:ListView): AsyncTask<String, Void, String>() {

            private val TAG = "DownloadData"

            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                Log.d(TAG,"onPostExecute: parameter is $result")
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

//                val arrayAdapter = ArrayAdapter<FeedEntry>(propContext, R.layout.list_item, parseApplications.applications)
//                propListView.adapter = arrayAdapter
                val feedAdapter = FeedAdapter(propContext,R.layout.list_record,parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]} ")
                val rssFeed = downloadXML(url[0])
                if(rssFeed.isEmpty()){
                    Log.e(TAG,"doInBackground: Error downloading")
                }
                Log.d(TAG,rssFeed)
                return rssFeed
            }

            private fun downloadXML(urlPath: String?):String {
                val xmlResult = StringBuilder()
                try {
                    val url = URL(urlPath)
                    Log.d(TAG, "downloadXML: url = $url")

                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    val response = connection.responseCode
                    Log.d(TAG, "downloadXML: The response code was $response")

                    connection.inputStream.buffered().reader().use { xmlResult.append(it.readText()) }

                } catch (e:Exception){
                    val errorMessage:String = when(e) {
                        is MalformedURLException -> "downloadXML: Invalid URL ${e.message}"
                        is IOException -> "downloadXML: IO Exception reading data: ${e.message}"
                        is SecurityException -> {
                            e.printStackTrace()
                            "downloadXML: Security Exception. Needs permissions? ${e.message}"
                        }
                        else -> "Unknown error: ${e.message}"
                    }
                }
                return xmlResult.toString()
            }
        }
    }


}
